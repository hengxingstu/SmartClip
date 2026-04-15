package com.smartclip.clip.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartclip.clip.dto.ClipCopyResponse;
import com.smartclip.clip.dto.ClipItemDetailResponse;
import com.smartclip.clip.dto.ClipItemListResponse;
import com.smartclip.clip.dto.ClipSearchRequest;
import com.smartclip.clip.entity.ClipEvent;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.enums.ClipListView;
import com.smartclip.clip.enums.SensitivityLevel;
import com.smartclip.clip.mapper.ClipEventMapper;
import com.smartclip.clip.mapper.ClipItemMapper;
import com.smartclip.clipboard.ClipboardService;
import com.smartclip.common.api.PageResponse;
import com.smartclip.common.exception.NotFoundException;
import com.smartclip.common.util.HashUtils;
import com.smartclip.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
/**
 * 剪贴板内容核心业务服务，负责采集、去重、搜索、详情、再次复制和软删除。
 */
public class ClipItemService {

    private final ClipItemMapper clipItemMapper;
    private final ClipEventMapper clipEventMapper;
    private final ClipTypeDetectService clipTypeDetectService;
    private final ClipPreviewService clipPreviewService;
    private final SensitivityDetectService sensitivityDetectService;
    private final AppSettingService appSettingService;
    private final ClipboardService clipboardService;

    @Transactional
    /**
     * 采集一段文本：按设置过滤空文本/短文本/敏感文本，随后按内容哈希新增或累加复制次数。
     */
    public Optional<ClipItem> captureText(String content) {
        if (content == null) {
            return Optional.empty();
        }

        String trimmed = content.strip();
        if (trimmed.isEmpty() || trimmed.length() < appSettingService.getMinTextLength()) {
            return Optional.empty();
        }

        boolean sensitive = sensitivityDetectService.isSensitive(content);
        if (sensitive && appSettingService.isIgnoreSensitiveEnabled()) {
            return Optional.empty();
        }

        String contentHash = HashUtils.sha256NormalizedText(content);
        LocalDateTime now = LocalDateTime.now();
        ClipItem existing = findByHash(contentHash);
        if (existing != null) {
            existing.setCopyCount(existing.getCopyCount() + 1);
            existing.setLastCopiedAt(now);
            existing.setUpdatedAt(now);
            existing.setIsIgnored(false);
            clipItemMapper.updateById(existing);
            insertEvent(existing.getId(), now, content);
            return Optional.of(existing);
        }

        ClipItem item = new ClipItem();
        item.setContent(content);
        item.setContentHash(contentHash);
        item.setType(clipTypeDetectService.detect(content));
        item.setSubType(null);
        item.setTitle(clipPreviewService.buildTitle(content));
        item.setPreviewText(clipPreviewService.buildPreview(content));
        item.setCopyCount(1);
        item.setFirstCopiedAt(now);
        item.setLastCopiedAt(now);
        item.setIsFavorite(false);
        item.setIsIgnored(false);
        item.setSensitivityLevel(sensitive ? SensitivityLevel.SENSITIVE : SensitivityLevel.NORMAL);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        clipItemMapper.insert(item);
        insertEvent(item.getId(), now, content);
        return Optional.of(item);
    }

    /**
     * 查询剪贴板历史列表，默认排除软删除记录，并支持关键词和类型过滤。
     */
    public PageResponse<ClipItemListResponse> search(ClipSearchRequest request) {
        Page<ClipItem> page = Page.of(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<ClipItem> query = Wrappers.<ClipItem>lambdaQuery();

        applyViewFilter(request, query);
        if (request.getType() != null) {
            query.eq(ClipItem::getType, request.getType());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            query.and(wrapper -> wrapper.like(ClipItem::getContent, keyword)
                    .or()
                    .like(ClipItem::getTitle, keyword)
                    .or()
                    .like(ClipItem::getPreviewText, keyword));
        }

        IPage<ClipItem> result = clipItemMapper.selectPage(page, query);
        return new PageResponse<>(
                result.getRecords().stream().map(this::toListResponse).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal()
        );
    }

    /**
     * 按 ID 查询完整剪贴板内容详情。
     */
    public ClipItemDetailResponse getDetail(Long id) {
        return toDetailResponse(requireItem(id));
    }

    @Transactional
    /**
     * 将历史内容重新写入系统剪贴板，并同步更新复制统计和事件流水。
     */
    public ClipCopyResponse copyToClipboard(Long id) {
        ClipItem item = requireItem(id);
        clipboardService.writeText(item.getContent());

        LocalDateTime now = LocalDateTime.now();
        item.setCopyCount(item.getCopyCount() + 1);
        item.setLastCopiedAt(now);
        item.setUpdatedAt(now);
        clipItemMapper.updateById(item);
        insertEvent(item.getId(), now, item.getContent());

        return new ClipCopyResponse(item.getId(), true, item.getCopyCount(), now);
    }

    @Transactional
    /**
     * 软删除历史记录：将其标记为 ignored，默认列表查询不再返回。
     */
    public void softDelete(Long id) {
        ClipItem item = requireItem(id);
        item.setIsIgnored(true);
        item.setUpdatedAt(LocalDateTime.now());
        clipItemMapper.updateById(item);
    }

    @Transactional
    public void setFavorite(Long id, boolean favorite) {
        ClipItem item = requireItem(id);
        item.setIsFavorite(favorite);
        item.setUpdatedAt(LocalDateTime.now());
        clipItemMapper.updateById(item);
    }

    @Transactional
    public void restore(Long id) {
        ClipItem item = requireItem(id);
        item.setIsIgnored(false);
        item.setUpdatedAt(LocalDateTime.now());
        clipItemMapper.updateById(item);
    }

    /**
     * 查询记录并在不存在时抛出业务异常，统一处理未找到场景。
     */
    private ClipItem requireItem(Long id) {
        ClipItem item = clipItemMapper.selectById(id);
        if (item == null) {
            throw new NotFoundException("Clip item not found: " + id);
        }
        return item;
    }

    /**
     * 按内容哈希查询去重主记录。
     */
    private ClipItem findByHash(String contentHash) {
        return clipItemMapper.selectOne(Wrappers.<ClipItem>lambdaQuery()
                .eq(ClipItem::getContentHash, contentHash)
                .last("LIMIT 1"));
    }

    private void applyViewFilter(ClipSearchRequest request, LambdaQueryWrapper<ClipItem> query) {
        ClipListView view = request.getView();
        if (view == null) {
            if (!request.isIncludeIgnored()) {
                query.eq(ClipItem::getIsIgnored, false);
            }
            query.orderByDesc(ClipItem::getLastCopiedAt);
            return;
        }

        switch (view) {
            case HISTORY -> query.eq(ClipItem::getIsIgnored, false)
                    .orderByDesc(ClipItem::getLastCopiedAt);
            case FAVORITES -> query.eq(ClipItem::getIsIgnored, false)
                    .eq(ClipItem::getIsFavorite, true)
                    .orderByDesc(ClipItem::getLastCopiedAt);
            case FREQUENT -> query.eq(ClipItem::getIsIgnored, false)
                    .orderByDesc(ClipItem::getCopyCount)
                    .orderByDesc(ClipItem::getLastCopiedAt);
            case IGNORED -> query.eq(ClipItem::getIsIgnored, true)
                    .orderByDesc(ClipItem::getLastCopiedAt);
            default -> query.orderByDesc(ClipItem::getLastCopiedAt);
        }
    }

    /**
     * 写入一次复制事件流水，用于保留重复复制和再次复制的发生记录。
     */
    private void insertEvent(Long clipItemId, LocalDateTime copiedAt, String content) {
        ClipEvent event = new ClipEvent();
        event.setClipItemId(clipItemId);
        event.setCopiedAt(copiedAt);
        event.setRawPreview(clipPreviewService.buildPreview(content));
        clipEventMapper.insert(event);
    }

    private ClipItemListResponse toListResponse(ClipItem item) {
        return new ClipItemListResponse(
                item.getId(),
                item.getType(),
                item.getSubType(),
                item.getTitle(),
                item.getPreviewText(),
                item.getCopyCount(),
                item.getLastCopiedAt(),
                item.getIsFavorite(),
                item.getIsIgnored(),
                item.getSensitivityLevel()
        );
    }

    private ClipItemDetailResponse toDetailResponse(ClipItem item) {
        return new ClipItemDetailResponse(
                item.getId(),
                item.getContent(),
                item.getType(),
                item.getSubType(),
                item.getTitle(),
                item.getPreviewText(),
                item.getCopyCount(),
                item.getFirstCopiedAt(),
                item.getLastCopiedAt(),
                item.getIsFavorite(),
                item.getIsIgnored(),
                item.getSensitivityLevel(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}

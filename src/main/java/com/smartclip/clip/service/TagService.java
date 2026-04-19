package com.smartclip.clip.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartclip.clip.dto.TagResponse;
import com.smartclip.clip.dto.TagSearchRequest;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.entity.ClipItemTag;
import com.smartclip.clip.entity.Tag;
import com.smartclip.clip.mapper.ClipItemMapper;
import com.smartclip.clip.mapper.ClipItemTagMapper;
import com.smartclip.clip.mapper.TagMapper;
import com.smartclip.common.api.PageResponse;
import com.smartclip.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private static final int MAX_TAG_LENGTH = 32;
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("^[\\p{IsHan}A-Za-z0-9_-]+$");

    private final TagMapper tagMapper;
    private final ClipItemTagMapper clipItemTagMapper;
    private final ClipItemMapper clipItemMapper;

    public PageResponse<TagResponse> search(TagSearchRequest request) {
        Page<Tag> page = Page.of(request.getPage(), request.getPageSize());
        var query = Wrappers.<Tag>lambdaQuery().orderByAsc(Tag::getName);
        if (StringUtils.hasText(request.getKeyword())) {
            query.like(Tag::getName, request.getKeyword().trim());
        }
        IPage<Tag> result = tagMapper.selectPage(page, query);
        return new PageResponse<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal()
        );
    }

    @Transactional
    public TagResponse create(String name) {
        return toResponse(getOrCreateTag(name));
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new NotFoundException("Tag not found: " + id);
        }
        clipItemTagMapper.delete(Wrappers.<ClipItemTag>lambdaQuery().eq(ClipItemTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    public List<TagResponse> listClipTags(Long clipItemId) {
        requireClip(clipItemId);
        List<ClipItemTag> relations = clipItemTagMapper.selectList(Wrappers.<ClipItemTag>lambdaQuery()
                .eq(ClipItemTag::getClipItemId, clipItemId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = relations.stream().map(ClipItemTag::getTagId).distinct().toList();
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery().in(Tag::getId, tagIds).orderByAsc(Tag::getName))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<TagResponse> replaceClipTags(Long clipItemId, List<String> names) {
        requireClip(clipItemId);
        Map<String, String> uniqueNames = normalizeUniqueNames(names);
        List<Tag> tags = new ArrayList<>();
        for (String name : uniqueNames.values()) {
            tags.add(getOrCreateTag(name));
        }

        clipItemTagMapper.delete(Wrappers.<ClipItemTag>lambdaQuery().eq(ClipItemTag::getClipItemId, clipItemId));
        LocalDateTime now = LocalDateTime.now();
        for (Tag tag : tags) {
            ClipItemTag relation = new ClipItemTag();
            relation.setClipItemId(clipItemId);
            relation.setTagId(tag.getId());
            relation.setCreatedAt(now);
            clipItemTagMapper.insert(relation);
        }
        return tags.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void autoApplyTags(Long clipItemId, List<String> names) {
        try {
            replaceClipTags(clipItemId, names);
        } catch (RuntimeException exception) {
            log.warn("Auto tag application skipped for clip {}: {}", clipItemId, exception.getMessage());
        }
    }

    public Set<Long> findClipIdsByTag(String name) {
        String normalizedName = normalizeName(validateTagName(name));
        Tag tag = findByNormalizedName(normalizedName);
        if (tag == null) {
            return Set.of();
        }
        List<ClipItemTag> relations = clipItemTagMapper.selectList(Wrappers.<ClipItemTag>lambdaQuery()
                .eq(ClipItemTag::getTagId, tag.getId()));
        Set<Long> clipIds = new LinkedHashSet<>();
        for (ClipItemTag relation : relations) {
            clipIds.add(relation.getClipItemId());
        }
        return clipIds;
    }

    public Map<Long, List<TagResponse>> listTagsByClipIds(Collection<Long> clipItemIds) {
        if (clipItemIds == null || clipItemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> ids = clipItemIds.stream().distinct().toList();
        List<ClipItemTag> relations = clipItemTagMapper.selectList(Wrappers.<ClipItemTag>lambdaQuery()
                .in(ClipItemTag::getClipItemId, ids));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> tagIds = relations.stream().map(ClipItemTag::getTagId).distinct().toList();
        Map<Long, TagResponse> tagsById = new HashMap<>();
        for (Tag tag : tagMapper.selectList(Wrappers.<Tag>lambdaQuery().in(Tag::getId, tagIds))) {
            tagsById.put(tag.getId(), toResponse(tag));
        }

        Map<Long, List<TagResponse>> result = new HashMap<>();
        for (ClipItemTag relation : relations) {
            TagResponse tag = tagsById.get(relation.getTagId());
            if (tag != null) {
                result.computeIfAbsent(relation.getClipItemId(), ignored -> new ArrayList<>()).add(tag);
            }
        }
        for (List<TagResponse> tags : result.values()) {
            tags.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        }
        return result;
    }

    private Tag getOrCreateTag(String rawName) {
        String name = validateTagName(rawName);
        String normalizedName = normalizeName(name);
        Tag existing = findByNormalizedName(normalizedName);
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        Tag tag = new Tag();
        tag.setName(name);
        tag.setNormalizedName(normalizedName);
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        tagMapper.insert(tag);
        return tag;
    }

    private Tag findByNormalizedName(String normalizedName) {
        return tagMapper.selectOne(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getNormalizedName, normalizedName)
                .last("LIMIT 1"));
    }

    private Map<String, String> normalizeUniqueNames(List<String> names) {
        if (names == null) {
            throw new IllegalArgumentException("Tag names must not be null");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String rawName : names) {
            String name = validateTagName(rawName);
            result.putIfAbsent(normalizeName(name), name);
        }
        return result;
    }

    private String validateTagName(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Tag name must not be null");
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        if (name.length() > MAX_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag name must not exceed " + MAX_TAG_LENGTH + " characters");
        }
        if (!TAG_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Tag name may only contain Chinese, letters, numbers, '-' or '_'");
        }
        return name;
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void requireClip(Long clipItemId) {
        ClipItem item = clipItemMapper.selectById(clipItemId);
        if (item == null) {
            throw new NotFoundException("Clip item not found: " + clipItemId);
        }
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt(), tag.getUpdatedAt());
    }
}

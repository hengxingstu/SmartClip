package com.smartclip.clip.controller;

import com.smartclip.clip.dto.ClipCopyResponse;
import com.smartclip.clip.dto.ClipItemDetailResponse;
import com.smartclip.clip.dto.ClipItemListResponse;
import com.smartclip.clip.dto.ClipSearchRequest;
import com.smartclip.clip.service.ClipItemService;
import com.smartclip.common.api.ApiResponse;
import com.smartclip.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clips")
/**
 * 剪贴板历史 REST 控制器，提供列表搜索、详情、再次复制和软删除接口。
 */
public class ClipItemController {

    private final ClipItemService clipItemService;

    @GetMapping
    /**
     * 按关键词、类型和分页参数查询剪贴板历史。
     */
    public ApiResponse<PageResponse<ClipItemListResponse>> search(@Valid @ModelAttribute ClipSearchRequest request) {
        return ApiResponse.ok(clipItemService.search(request));
    }

    @GetMapping("/{id}")
    /**
     * 查询单条剪贴板历史的完整内容和统计信息。
     */
    public ApiResponse<ClipItemDetailResponse> detail(@PathVariable @Min(1) Long id) {
        return ApiResponse.ok(clipItemService.getDetail(id));
    }

    @PostMapping("/{id}/copy")
    /**
     * 将历史内容再次写入系统剪贴板，并记录一次复制事件。
     */
    public ApiResponse<ClipCopyResponse> copy(@PathVariable @Min(1) Long id) {
        return ApiResponse.ok(clipItemService.copyToClipboard(id));
    }

    @PutMapping("/{id}/favorite")
    public ApiResponse<Void> favorite(@PathVariable @Min(1) Long id) {
        clipItemService.setFavorite(id, true);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable @Min(1) Long id) {
        clipItemService.setFavorite(id, false);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable @Min(1) Long id) {
        clipItemService.restore(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    /**
     * MVP 阶段使用软删除，将记录标记为忽略而不是物理删除。
     */
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        clipItemService.softDelete(id);
        return ApiResponse.ok(null);
    }
}

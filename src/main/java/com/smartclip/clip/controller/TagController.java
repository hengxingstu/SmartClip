package com.smartclip.clip.controller;

import com.smartclip.clip.dto.TagCreateRequest;
import com.smartclip.clip.dto.TagResponse;
import com.smartclip.clip.dto.TagSearchRequest;
import com.smartclip.clip.service.TagService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ApiResponse<PageResponse<TagResponse>> search(@Valid @ModelAttribute TagSearchRequest request) {
        return ApiResponse.ok(tagService.search(request));
    }

    @PostMapping
    public ApiResponse<TagResponse> create(@Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.ok(tagService.create(request.getName()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        tagService.delete(id);
        return ApiResponse.ok(null);
    }
}

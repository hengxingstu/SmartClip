package com.smartclip.clip.dto;

import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.enums.ClipListView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
/**
 * 剪贴板历史搜索请求 DTO，承载关键词、类型和分页过滤条件。
 */
public class ClipSearchRequest {

    private String keyword;
    private ClipType type;
    private ClipListView view;
    private String tag;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long pageSize = 20;

    private boolean includeIgnored = false;
}

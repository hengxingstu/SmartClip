package com.smartclip.clip.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TagSearchRequest {

    private String keyword;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long pageSize = 20;
}

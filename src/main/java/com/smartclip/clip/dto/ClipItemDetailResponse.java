package com.smartclip.clip.dto;

import java.time.LocalDateTime;

import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.enums.SensitivityLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 剪贴板详情响应 DTO，包含完整内容、类型和复制统计。
 */
public class ClipItemDetailResponse {

    private Long id;
    private String content;
    private ClipType type;
    private String subType;
    private String title;
    private String previewText;
    private Integer copyCount;
    private LocalDateTime firstCopiedAt;
    private LocalDateTime lastCopiedAt;
    private Boolean isFavorite;
    private Boolean isIgnored;
    private SensitivityLevel sensitivityLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

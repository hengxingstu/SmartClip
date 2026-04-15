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
 * 剪贴板历史列表响应 DTO，只携带列表页需要展示的摘要字段。
 */
public class ClipItemListResponse {

    private Long id;
    private ClipType type;
    private String subType;
    private String title;
    private String previewText;
    private Integer copyCount;
    private LocalDateTime lastCopiedAt;
    private Boolean isFavorite;
    private Boolean isIgnored;
    private SensitivityLevel sensitivityLevel;
}

package com.smartclip.clip.model;

import java.util.List;

import com.smartclip.clip.enums.ClipType;

public record ClipClassification(
        ClipType type,
        String subType,
        String title,
        List<String> tagNames
) {
}

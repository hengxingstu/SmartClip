package com.smartclip.clip.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClipTagUpdateRequest {

    @NotNull
    private List<String> names;
}

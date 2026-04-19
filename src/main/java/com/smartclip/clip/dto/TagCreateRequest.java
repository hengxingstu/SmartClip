package com.smartclip.clip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagCreateRequest {

    @NotBlank
    private String name;
}

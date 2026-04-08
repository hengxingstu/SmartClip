package com.smartclip.clip.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 再次复制接口响应 DTO，返回复制结果、复制次数和发生时间。
 */
public class ClipCopyResponse {

    private Long id;
    private boolean copied;
    private Integer copyCount;
    private LocalDateTime copiedAt;
}

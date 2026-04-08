package com.smartclip.clip.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.enums.SensitivityLevel;
import com.smartclip.common.mybatis.SqliteLocalDateTimeTypeHandler;
import lombok.Data;

@Data
@TableName(value = "clip_item", autoResultMap = true)
/**
 * 剪贴板内容主表实体，保存去重后的文本内容、类型、预览和复制统计。
 */
public class ClipItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;
    private String contentHash;
    private ClipType type;
    private String subType;
    private String title;
    private String previewText;
    private Integer copyCount;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime firstCopiedAt;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime lastCopiedAt;

    private Boolean isFavorite;
    private Boolean isIgnored;
    private SensitivityLevel sensitivityLevel;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime createdAt;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime updatedAt;
}

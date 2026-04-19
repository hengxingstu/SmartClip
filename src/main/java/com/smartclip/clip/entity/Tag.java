package com.smartclip.clip.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartclip.common.mybatis.SqliteLocalDateTimeTypeHandler;
import lombok.Data;

@Data
@TableName(value = "tag", autoResultMap = true)
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String normalizedName;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime createdAt;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime updatedAt;
}

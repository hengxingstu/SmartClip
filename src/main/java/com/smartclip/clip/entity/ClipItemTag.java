package com.smartclip.clip.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartclip.common.mybatis.SqliteLocalDateTimeTypeHandler;
import lombok.Data;

@Data
@TableName(value = "clip_item_tag", autoResultMap = true)
public class ClipItemTag {

    @TableId(type = IdType.INPUT)
    private Long clipItemId;
    private Long tagId;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime createdAt;
}

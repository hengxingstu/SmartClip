package com.smartclip.clip.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartclip.common.mybatis.SqliteLocalDateTimeTypeHandler;
import lombok.Data;

@Data
@TableName(value = "clip_event", autoResultMap = true)
/**
 * 剪贴板复制事件实体，记录每一次复制发生的时间和预览内容。
 */
public class ClipEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clipItemId;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime copiedAt;

    private String rawPreview;
}

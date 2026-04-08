package com.smartclip.setting.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartclip.common.mybatis.SqliteLocalDateTimeTypeHandler;
import lombok.Data;

@Data
@TableName(value = "app_setting", autoResultMap = true)
/**
 * 应用设置实体，以 key-value 方式保存本地运行配置。
 */
public class AppSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String settingKey;
    private String settingValue;
    private String valueType;
    private String description;

    @TableField(typeHandler = SqliteLocalDateTimeTypeHandler.class)
    private LocalDateTime updatedAt;
}

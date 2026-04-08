package com.smartclip.common.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 统一分页响应结构，封装列表数据、页码、页大小和总数。
 */
public class PageResponse<T> {

    private List<T> items;
    private long page;
    private long pageSize;
    private long total;
}

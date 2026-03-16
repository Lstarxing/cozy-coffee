package com.cozy.common.result;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装
 * 
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 当前页码（1开始） */
    private int page;

    /** 每页大小 */
    private int size;

    /** 总页数 */
    private int totalPages;

    /** 数据列表 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, int page, int size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(long total, int page, int size, List<T> records) {
        return new PageResult<>(total, page, size, records);
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(0, page, size, java.util.Collections.emptyList());
    }
}

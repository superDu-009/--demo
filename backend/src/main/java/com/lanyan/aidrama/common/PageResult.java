package com.lanyan.aidrama.common;

import lombok.Data;

/**
 * 分页响应格式 (系分 2.1)
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码 (1-based) */
    private int page;

    /** 每页大小 */
    private int size;

    /** 是否还有下一页 */
    private boolean hasNext;

    /** 数据列表 */
    private T list;

    /**
     * 静态工厂方法：从 MyBatis-Plus IPage 构建 PageResult
     * hasNext 计算：当前页 * 每页大小 < 总数
     */
    public static <T> PageResult<java.util.List<T>> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        PageResult<java.util.List<T>> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPage((int) page.getCurrent());
        result.setSize((int) page.getSize());
        result.setHasNext(page.getCurrent() * page.getSize() < page.getTotal());
        result.setList(page.getRecords());
        return result;
    }
}

package com.example.demo.common;

import java.util.List;

/**
 * 分页响应类
 * 
 * 【架构说明】
 * 封装分页查询的结果，包含数据列表、总记录数、当前页码、每页记录数、总页数
 * 
 * 【核心功能】
 * 1. 提供分页查询的结果封装
 * 2. 支持自定义分页参数（page、size）
 * 3. 计算总页数（totalPages）
 * 
 * 
 */
public class PageResponse<T> {

    private List<T> data;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse() {
    }

    /**
     * 分页响应构造函数
     * 
     * @param data     数据列表
     * @param total    总记录数
     * @param page     当前页码
     * @param size     每页记录数
     */
    public PageResponse(List<T> data, long total, int page, int size) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
    }

    /**
     * 获取分页数据列表
     * 
     * @return 分页数据列表
     */
    public List<T> getData() {
        return data;
    }

    /**
     * 设置分页数据列表
     * 
     * @param data 分页数据列表
     */
    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

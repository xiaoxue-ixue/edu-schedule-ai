package com.edu.schedule.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果 {list, total, page, size}
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Integer page, Integer size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(Integer page, Integer size, Long total, List<T> list) {
        if (list == null) {
            list = Collections.emptyList();
        }
        if (total == null) {
            total = (long) list.size();
        }
        return new PageResult<>(list, total, page, size);
    }

    public static <T> PageResult<T> from(PageInfo<T> info) {
        return new PageResult<>(info.getList(), info.getTotal(), info.getPageNum(), info.getPageSize());
    }
}

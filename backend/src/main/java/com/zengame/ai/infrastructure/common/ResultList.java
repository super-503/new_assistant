package com.zengame.ai.infrastructure.common;

import lombok.Data;

import java.util.List;

/**
 * @author jurzis.zhang
 * @date 2024/10/12 16:56
 **/
@Data
public class ResultList<T> {

    /**
     * 数据
     */
    List<T> list;

    /**
     * 分页参数
     */
    BasePageInfo pagination;


    public ResultList() {
    }

    public ResultList(List<T> list, BasePageInfo pagination) {
        this.list = list;
        this.pagination = pagination;
    }

}

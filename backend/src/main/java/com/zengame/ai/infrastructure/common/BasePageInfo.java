package com.zengame.ai.infrastructure.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jurzis.zhang
 * @date 2024/10/12 14:41
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePageInfo {

    /**
     * 当前页码
     */
    int pageNo = 1;

    /**
     * 页大小
     */
    int pageSize = 10;

    /**
     * 总记录数
     */
    long total;

    public BasePageInfo(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}

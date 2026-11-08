package com.zengame.ai.infrastructure.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

/**
 * @author jurzis.zhang
 * @date 2024/10/12 14:38
 **/
@Data
public class Result<T> {

    public static final int CODE_OK = 200;

    public static final int RET_CODE_OK = 1;

    public static final String MSG_OK = "OK";

    /**
     * 状态码
     */
    int code;

    /**
     * 内部系统的状态码
     */
    int ret;

    /**
     * 消息
     */
    String msg;

    /**
     * 数据
     */
    T data;


    public Result() {
    }

    public Result(int code, int ret, String msg, T data) {
        this.code = code;
        this.ret = ret;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 正常返回
     *
     * @param data 正常数据
     * @param <T>  类型
     * @return 正常返回
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_OK, RET_CODE_OK, MSG_OK, data);
    }

    /**
     * 正常返回
     *
     * @param <T>  类型
     * @return 正常返回
     */
    public static <T> Result<T> ok() {
        return new Result<>(CODE_OK, RET_CODE_OK, MSG_OK, null);
    }

    /**
     * 正常返回
     *
     * @param page 正常数据
     * @param <T>  类型
     * @return 正常返回
     */
    public static <T> Result<ResultList<T>> ok(PageInfo<T> page) {
        ResultList<T> resultList = new ResultList<>(page.getList(), new BasePageInfo(page.getPageNum(), page.getPageSize(), page.getTotal()));
        return new Result<>(CODE_OK, RET_CODE_OK, MSG_OK, resultList);
    }

    public boolean isOk() {
        return code == CODE_OK || ret == RET_CODE_OK;
    }

}

package com.zengame.ai.infrastructure.common;

import com.github.pagehelper.ISelect;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.Objects;

/**
 * PageHelper工具包
 *
 * @author jurzis.zhang
 */
public abstract class PageHelperUtil {

    private static final String DEFAULT_SORT = "CREATE_TIME";

    private static final String DEFAULT_ORDER = " DESC";

    private static final Integer DEFAULT_PAGE_NUMBER = 1;

    private static final Integer DEFAULT_PAGE_SIZE = 10;

    private PageHelperUtil() {
    }

    /**
     * 开启分页 下一次查询时分页
     *
     * @param pageNumber 页码
     * @param pageSize   每页数量
     * @param orderBy    根据某字段排序 倒序请加" DESC"
     * @param <E>        Model
     * @return 分页对象
     */
    public static <E> Page<E> startPage(Integer pageNumber, Integer pageSize, String orderBy) {
        return PageHelper.startPage(pageNumber, pageSize, orderBy);
    }

    /**
     * 开启分页 下一次查询时分页
     *
     * @param pageNumber 页码
     * @param pageSize   每页数量
     * @param <E>        Model
     * @return 分页对象
     */
    public static <E> Page<E> startPage(Integer pageNumber, Integer pageSize) {
        return PageHelper.startPage(pageNumber, pageSize);
    }

    /**
     * 移除分页本地变量
     */
    public static void clearPage() {
        PageHelper.clearPage();
    }

    /**
     * 分页查询
     *
     * @param basePageInfo 分页数据 不带时使用默认值
     * @param <E>          Model
     * @return 分页对象
     */
    public static <E> Page<E> startPage(BasePageInfo basePageInfo) {
        Integer currentNo = null;
        Integer pageSize = null;
        if (!Objects.isNull(basePageInfo)) {
            currentNo = basePageInfo.getPageNo();
            pageSize = basePageInfo.getPageSize();
            if (currentNo == 0) {
                currentNo = DEFAULT_PAGE_NUMBER;
            }
            if (pageSize == 0) {
                pageSize = DEFAULT_PAGE_SIZE;
            }
        }

        return startPage(currentNo, pageSize);
    }

    /**
     * 分页查询
     *
     * @param basePageInfo 带分页数据 不带时使用默认值
     * @param iSelect      查询语句
     * @param <E>          Model
     * @return 分页对象
     */
    public static <E> PageInfo<E> doSelectPageInfo(BasePageInfo basePageInfo, ISelect iSelect) {
        return startPage(basePageInfo).doSelectPageInfo(iSelect);
    }
}

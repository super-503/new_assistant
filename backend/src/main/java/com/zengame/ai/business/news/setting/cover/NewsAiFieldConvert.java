package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.processing.StringListJson;
import com.zengame.ai.business.news.setting.domain.request.NewsAiFieldSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiFieldVO;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 字段配置的转换：库表行转列表展示，保存请求转库表行。
 */
@Mapper
public interface NewsAiFieldConvert {

    NewsAiFieldConvert INSTANCE = Mappers.getMapper(NewsAiFieldConvert.class);

    /**
     * 库表行转展示对象，带上种类决定的锁定项。
     *
     * @param po AI 字段
     * @return 展示对象
     */
    @Mapping(target = "kindDesc", source = "kind", qualifiedByName = "kindDesc")
    @Mapping(target = "sourceDesc", source = "source", qualifiedByName = "sourceDesc")
    @Mapping(target = "outputTypeLabel", source = "po", qualifiedByName = "outputTypeLabel")
    @Mapping(target = "options", source = "optionsJson", qualifiedByName = "readOptions")
    @Mapping(target = "deletable", source = "kind", qualifiedByName = "deletable")
    @Mapping(target = "fieldKeyLocked", source = "kind", qualifiedByName = "fieldKeyLocked")
    @Mapping(target = "outputTypeLocked", source = "kind", qualifiedByName = "fieldKeyLocked")
    @Mapping(target = "promptOnly", source = "kind", qualifiedByName = "promptOnly")
    NewsAiFieldVO toVO(NewsAiFieldPO po);

    /**
     * @param pos AI 字段
     * @return 展示对象，保持原顺序
     */
    List<NewsAiFieldVO> toVOList(List<NewsAiFieldPO> pos);

    /**
     * 新增请求转库表行；种类、只对文章生成、排序由服务层决定或取库表默认值。
     *
     * @param request 保存请求
     * @return 库表行
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kind", ignore = true)
    @Mapping(target = "articleOnly", ignore = true)
    @Mapping(target = "sortNo", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "source", source = "source", qualifiedByName = "toSource")
    @Mapping(target = "outputType", source = "outputType", qualifiedByName = "toOutputType")
    @Mapping(target = "optionsJson", source = "options", qualifiedByName = "writeOptions")
    NewsAiFieldPO toPO(NewsAiFieldSaveRequest request);

    /**
     * 编辑请求覆盖到已有的库表行；主键、种类、只对文章生成、排序保持不变。
     *
     * @param request 保存请求
     * @param po      已有的 AI 字段
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kind", ignore = true)
    @Mapping(target = "articleOnly", ignore = true)
    @Mapping(target = "sortNo", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "source", source = "source", qualifiedByName = "toSource")
    @Mapping(target = "outputType", source = "outputType", qualifiedByName = "toOutputType")
    @Mapping(target = "optionsJson", source = "options", qualifiedByName = "writeOptions")
    void updatePO(NewsAiFieldSaveRequest request, @MappingTarget NewsAiFieldPO po);

    @Named("kindDesc")
    default String kindDesc(NewsAiFieldKind kind) {
        return kind.getDesc();
    }

    @Named("sourceDesc")
    default String sourceDesc(NewsAiFieldSource source) {
        return source.getDesc();
    }

    /**
     * 文本按是否多行区分为单行文本、多行文本，其他取输出类型中文名。
     */
    @Named("outputTypeLabel")
    default String outputTypeLabel(NewsAiFieldPO po) {
        if (po.getOutputType() != NewsAiFieldOutputType.TEXT) {
            return po.getOutputType().getDesc();
        }
        if (BooleanUtils.isTrue(po.getMultiline())) {
            return "多行文本";
        }
        return "单行文本";
    }

    @Named("readOptions")
    default List<String> readOptions(String optionsJson) {
        return StringListJson.read(optionsJson);
    }

    /**
     * 选项去掉首尾空白后写为 JSON 数组。
     */
    @Named("writeOptions")
    default String writeOptions(List<String> options) {
        return StringListJson.write(options.stream().map(StringUtils::trim).collect(Collectors.toList()));
    }

    @Named("deletable")
    default Boolean deletable(NewsAiFieldKind kind) {
        return kind == NewsAiFieldKind.CUSTOM;
    }

    /**
     * 系统字段、内置字段的字段标识和输出类型都锁定。
     */
    @Named("fieldKeyLocked")
    default Boolean fieldKeyLocked(NewsAiFieldKind kind) {
        return kind != NewsAiFieldKind.CUSTOM;
    }

    @Named("promptOnly")
    default Boolean promptOnly(NewsAiFieldKind kind) {
        return kind == NewsAiFieldKind.BUILTIN;
    }

    @Named("toSource")
    default NewsAiFieldSource toSource(String code) {
        return CodeEnumUtils.fromCode(code, NewsAiFieldSource.class);
    }

    @Named("toOutputType")
    default NewsAiFieldOutputType toOutputType(String code) {
        return CodeEnumUtils.fromCode(code, NewsAiFieldOutputType.class);
    }
}

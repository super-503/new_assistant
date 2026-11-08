package com.zengame.ai.infrastructure.util;

import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link CodeEnum} 的严格解析工具：请求边界的字符串编码进入 Service 前经此转为枚举。
 */
public final class CodeEnumUtils {

    private CodeEnumUtils() {
    }

    /**
     * 把外部编码转换为枚举，空编码和未知编码都拒绝。
     *
     * @param code     外部编码
     * @param enumType 目标枚举类型
     * @param <E>      实现稳定编码的枚举类型
     * @return 编码对应的枚举值
     * @throws BusinessException 编码为空或没有对应的枚举值
     */
    public static <E extends Enum<E> & CodeEnum> E fromCode(String code, Class<E> enumType) {
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(enumType.getSimpleName() + " 编码不能为空");
        }
        for (E value : enumType.getEnumConstants()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BusinessException(enumType.getSimpleName() + " 编码无效: " + code);
    }
}

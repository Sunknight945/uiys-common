package uiys.common.util;

import uiys.common.constant.BaseEnum;

public class EnumUtils {
    public static <E extends Enum<E> & BaseEnum<E>> E parseByCode(Class<E> clazz, Integer code) {
        return BaseEnum.parseByCode(clazz, code).orElse(null);
    }
}
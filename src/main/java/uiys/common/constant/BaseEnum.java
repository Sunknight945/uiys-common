package uiys.common.constant;


import java.util.Arrays;
import java.util.Optional;

/**
 * 枚举基类接口
 * 所有枚举必须实现此接口，统一 code 和 name 的访问方式
 *
 * @param <E> 枚举类型自身，必须同时为 Enum 和 BaseEnum 的子类型
 */
public interface BaseEnum<E extends Enum<E> & BaseEnum<E>> {

    /**
     * 获取枚举编码（用于存储和传输）
     */
    Integer getCode();

    /**
     * 获取枚举名称（用于显示）
     */
    String getName();

    /**
     * 根据 code 解析枚举
     */
    static <E extends Enum<E> & BaseEnum<E>> Optional<E> parseByCode(Class<E> clazz, Integer code) {
        if (clazz == null || code == null) {
            return Optional.empty();
        }
        E[] constants = clazz.getEnumConstants();
        if (constants == null) {
            return Optional.empty();
        }
        return Arrays.stream(constants)
                .filter(item -> item.getCode().equals(code))
                .findFirst();
    }

    /**
     * 根据 name 解析枚举
     */
    static <E extends Enum<E> & BaseEnum<E>> Optional<E> parseByName(Class<E> clazz, String name) {
        if (clazz == null || name == null) {
            return Optional.empty();
        }
        E[] constants = clazz.getEnumConstants();
        if (constants == null) {
            return Optional.empty();
        }
        return Arrays.stream(constants)
                .filter(item -> item.getName().equals(name))
                .findFirst();
    }

    /**
     * 根据 code 解析枚举，不存在则返回默认值
     */
    static <E extends Enum<E> & BaseEnum<E>> E parseByCodeOrDefault(Class<E> clazz, Integer code, E defaultValue) {
        return parseByCode(clazz, code).orElse(defaultValue);
    }

    /**
     * 根据 name 解析枚举，不存在则返回默认值
     */
    static <E extends Enum<E> & BaseEnum<E>> E parseByNameOrDefault(Class<E> clazz, String name, E defaultValue) {
        return parseByName(clazz, name).orElse(defaultValue);
    }

    /**
     * 判断当前枚举是否等于指定 code
     */
    default boolean equalsCode(Integer code) {
        return code != null && this.getCode().equals(code);
    }

    /**
     * 判断当前枚举是否等于指定 name
     */
    default boolean equalsName(String name) {
        return name != null && this.getName().equals(name);
    }
}
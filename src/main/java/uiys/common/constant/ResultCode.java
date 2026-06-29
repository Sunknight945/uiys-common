package uiys.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ResultCode implements BaseEnum<ResultCode> {

    // ===== 成功 =====
    SUCCESS(200, "操作成功"),

    // ===== 客户端错误 (4xx) =====
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请重新登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    REQUEST_TIMEOUT(408, "请求超时"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),

    // ===== 服务端错误 (5xx) =====
    INTERNAL_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // ===== 业务错误 (1xxx - 9xxx) =====
    BUSINESS_ERROR(1000, "业务处理失败"),
    PARAM_VALID_ERROR(1001, "参数校验失败"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_DUPLICATE(1003, "数据重复"),
    DATA_EXPIRED(1004, "数据已过期"),
    OPERATION_FORBIDDEN(1005, "操作被禁止"),
    TOO_MANY_REQUESTS(1006, "请求过于频繁"),

    // ===== 认证授权 (2xxx) =====
    TOKEN_EXPIRED(2001, "Token已过期"),
    TOKEN_INVALID(2002, "Token无效"),
    TOKEN_MISSING(2003, "Token缺失"),
    ACCOUNT_LOCKED(2004, "账户已锁定"),
    ACCOUNT_DISABLED(2005, "账户已禁用");

    private final Integer code;
    private final String name;

    /**
     * 根据 code 获取枚举（使用 BaseEnum 的静态方法）
     */
    public static ResultCode parseByCode(Integer code) {
        return BaseEnum.parseByCode(ResultCode.class, code).orElse(null);
    }

    /**
     * 根据 name 获取枚举（使用 BaseEnum 的静态方法）
     */
    public static ResultCode parseByName(String name) {
        return BaseEnum.parseByName(ResultCode.class, name).orElse(null);
    }

    /**
     * 根据 code 获取枚举，不存在则返回默认值
     */
    public static ResultCode parseByCodeOrDefault(Integer code, ResultCode defaultValue) {
        return BaseEnum.parseByCodeOrDefault(ResultCode.class, code, defaultValue);
    }
}
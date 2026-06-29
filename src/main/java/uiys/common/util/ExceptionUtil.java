package uiys.common.util;

import cn.hutool.json.JSONUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * 异常工具类
 */
@UtilityClass
public class ExceptionUtil {

    /**
     * 获取异常的根本原因（展开代理异常链）
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable cause = throwable;

        // 展开 UndeclaredThrowableException
        if (cause instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException ue = (UndeclaredThrowableException) cause;
            Throwable undeclaredCause = ue.getUndeclaredThrowable();
            if (undeclaredCause != null) {
                cause = undeclaredCause;
            }
        }

        // 展开 InvocationTargetException
        if (cause instanceof InvocationTargetException) {
            InvocationTargetException ite = (InvocationTargetException) cause;
            Throwable target = ite.getTargetException();
            if (target != null) {
                cause = target;
            }
        }

        // 如果有更深层的 cause，递归获取
        if (cause.getCause() != null && cause != cause.getCause()) {
            return getRootCause(cause.getCause());
        }

        return cause;
    }

    /**
     * 获取异常的根本原因和详细信息（用于日志打印）
     */
    public static String getCauseDetail(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable rootCause = getRootCause(throwable);
        return JSONUtil.toJsonStr(rootCause);
    }

    /**
     * 将异常对象转换为 JSON 格式（供日志使用）
     */
    public static String toJson(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return JSONUtil.toJsonStr(throwable);
    }

    /**
     * 判断是否为特定类型的异常（递归检查 cause）
     */
    public static boolean isCauseType(Throwable throwable, Class<? extends Throwable> targetType) {
        if (throwable == null) {
            return false;
        }
        if (targetType.isAssignableFrom(throwable.getClass())) {
            return true;
        }
        return throwable.getCause() != null && isCauseType(throwable.getCause(), targetType);
    }

    /**
     * 从异常链中查找指定类型的异常
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T findCause(Throwable throwable, Class<T> targetType) {
        if (throwable == null) {
            return null;
        }
        if (targetType.isAssignableFrom(throwable.getClass())) {
            return (T) throwable;
        }
        return throwable.getCause() != null ? findCause(throwable.getCause(), targetType) : null;
    }
}
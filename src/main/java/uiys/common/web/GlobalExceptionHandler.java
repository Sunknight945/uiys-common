package uiys.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uiys.common.constant.ResultCode;
import uiys.common.data.ValidationException;
import uiys.common.exception.BusinessException;
import uiys.common.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 1. 自定义业务异常 =====
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        logRequestInfo("业务异常", e);
        // 开发环境可打印堆栈
        if (log.isDebugEnabled()) {
            log.debug("BusinessException stack trace: ", e);
        }
        return Result.error(e.getCode(), e.getMsg());
    }

    // ===== 2. 自定义校验异常（旧项目有） =====
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(ValidationException e) {
        logRequestInfo("校验异常", e);
        // 如果有详细校验结果，可一并返回
        Object data = e.getResult();
        String msg = e.getMessage();
        if (data != null) {
            // 可返回更详细的信息
            return Result.error(ResultCode.PARAM_VALID_ERROR.getCode(), msg, data);
        }
        return Result.error(ResultCode.PARAM_VALID_ERROR.getCode(), msg);
    }

    // ===== 3. 参数校验异常（@Valid 实体校验） =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> String.format("字段[%s] %s (实际值: %s)",
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()))
                .collect(Collectors.joining("; "));
        logRequestInfo("参数校验失败", e);
        return Result.error(ResultCode.PARAM_VALID_ERROR.getCode(), msg);
    }

    // ===== 4. 参数绑定异常（@Validated + 普通参数） =====
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(fieldError -> String.format("字段[%s] %s (实际值: %s)",
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()))
                .collect(Collectors.joining("; "));
        logRequestInfo("参数绑定失败", e);
        return Result.error(ResultCode.PARAM_VALID_ERROR.getCode(), msg);
    }

    // ===== 5. 单参校验异常（@RequestParam + @NotBlank 等） =====
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        logRequestInfo("单参校验失败", e);
        return Result.error(ResultCode.PARAM_VALID_ERROR.getCode(), msg);
    }

    // ===== 6. 异常链展开（处理代理异常） =====
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        // 展开异常链，获取最底层的业务异常
        Throwable rootCause = ExceptionUtil.getRootCause(e);
        // 处理 UndeclaredThrowableException / InvocationTargetException 等
        Throwable resolved = resolveProxyException(e);
        if (resolved != e && resolved != null) {
            // 如果解析出了更深层的异常，重新处理
            if (resolved instanceof BusinessException) {
                return handleBusinessException((BusinessException) resolved);
            } else if (resolved instanceof ValidationException) {
                return handleValidationException((ValidationException) resolved);
            }
            // 否则继续
            e = (Exception) resolved;
        }

        // 如果根异常是 BusinessException，按业务异常处理
        if (rootCause instanceof BusinessException) {
            BusinessException be = (BusinessException) rootCause;
            logRequestInfo("展开后的业务异常", be);
            return Result.error(be.getCode(), be.getMsg());
        }
        if (e instanceof NoResourceFoundException) {
            NoResourceFoundException noResultException = (NoResourceFoundException) e;
            logRequestInfo("资源不存在", noResultException);
            log.error("资源不存在: ", noResultException);
            return Result.error(ResultCode.NOT_FOUND.getCode(), noResultException.getMessage());
        }
        // 兜底：记录完整错误信息
        logRequestInfo("系统异常", e);
        log.error("系统异常详情: ", e);
        return Result.error(ResultCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
    }

    // ===== 辅助方法 =====

    /**
     * 解析代理异常，获取真正的异常
     */
    private Throwable resolveProxyException(Throwable e) {
        if (e instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException ue = (UndeclaredThrowableException) e;
            Throwable undeclared = ue.getUndeclaredThrowable();
            if (undeclared instanceof InvocationTargetException) {
                InvocationTargetException ite = (InvocationTargetException) undeclared;
                Throwable target = ite.getTargetException();
                if (target != null) {
                    return target;
                }
                return ite;
            }
            return undeclared;
        }
        return e;
    }

    /**
     * 记录请求信息（带 traceId、路径、方法等）
     */
    private void logRequestInfo(String type, Throwable e) {
        HttpServletRequest request = getRequest();
        String traceId = MDC.get("traceId");
        String path = request != null ? request.getRequestURI() : "UNKNOWN";
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String params = request != null ? request.getQueryString() : "";
        String body = ""; // 可扩展获取 request body，但较复杂

        String msg = String.format("%s | traceId: %s | path: %s | method: %s | params: %s | error: %s",
                type, traceId, path, method, params, e.getMessage());

        if (e instanceof BusinessException || e instanceof ValidationException) {
            log.warn(msg);
        } else {
            log.error(msg, e);
        }
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
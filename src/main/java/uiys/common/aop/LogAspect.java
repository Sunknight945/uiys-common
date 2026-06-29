package uiys.common.aop;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uiys.common.util.DateUtils;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class LogAspect {



    // 精准匹配所有 HTTP 映射注解
    @Pointcut(
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.RequestMapping)"
    )
    public void httpEndpoint() {
    }

    @Around("httpEndpoint()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        // 放入 MDC，供全局异常处理器等使用
        MDC.put("traceId", traceId);

        HttpServletRequest request = getRequest();
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        // 入参日志
        if (log.isDebugEnabled()) {
            log.debug("[{}] 请求: {} | 参数: {}", traceId, methodName, JSONUtil.toJsonStr(args));
        } else {
            log.info("[{}] 请求: {} | 方式: {} | 路径: {}",
                    traceId,
                    methodName,
                    request != null ? request.getMethod() : "UNKNOWN",
                    request != null ? request.getRequestURI() : "UNKNOWN"
            );
        }

        // 🟢 改用 nanoTime 测量耗时，更精确、更安全
        long startNanos = System.nanoTime();
        try {
            Object result = joinPoint.proceed(args);
            long costNanos = System.nanoTime() - startNanos;

            // 🟢 使用格式化工具输出可读耗时
            if (log.isDebugEnabled()) {
                log.debug("[{}] 响应: {} | 耗时: {}", traceId, JSONUtil.toJsonStr(result), DateUtils.formatDuration(costNanos));
            } else {
                log.info("[{}] 完成: {} | 耗时: {}", traceId, methodName, DateUtils.formatDuration(costNanos));
            }
            return result;

        } catch (Throwable throwable) {
            long costNanos = System.nanoTime() - startNanos;
            log.error("[{}] 异常: {} | 耗时: {} | 错误: {}", traceId, methodName, DateUtils.formatDuration(costNanos), throwable.getMessage(), throwable);
            throw throwable;
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
package uiys.common.util;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uiys.common.dto.UserDTO;
import uiys.common.web.Result;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UserContentUtils implements HandlerInterceptor {

    private static final ThreadLocal<UserDTO> USER_HOLDER = new ThreadLocal<>();

    private static final Cache<Long, UserDTO> CACHE = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(3, TimeUnit.HOURS)
            .recordStats()
            .build();

    // ★ 改为静态变量，通过 setter 注入
    private static String userServiceUrl;

    @Value("${user.service.url:http://localhost:10002/api/user/user/queryOneUser}")
    public void setUserServiceUrl(String url) {
        UserContentUtils.userServiceUrl = url;
    }

    private static final TypeReference<Result<UserDTO>> TYPE_REF = new TypeReference<>() {
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader("X-USER-ID");
        if (StrUtil.isNotBlank(userIdStr)) {
            try {
                Long userId = Long.parseLong(userIdStr);
                USER_HOLDER.set(new UserDTO(userId)); // 占位对象
                log.debug("用户ID [{}] 已放入 ThreadLocal 占位", userId);
            } catch (NumberFormatException e) {
                log.warn("请求头 X-USER-ID 格式错误: {}", userIdStr);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserDTO user = USER_HOLDER.get();
        if (user != null) {
            log.debug("清除用户 [{}] 的 ThreadLocal", user.getId());
            USER_HOLDER.remove();
        }
    }

    /**
     * 供业务层调用，返回 Optional，安全处理无用户情况。
     * 延迟加载：只有调用此方法时才会真正去加载用户完整信息。
     */
    public static Optional<UserDTO> getCurrentUser() {
        UserDTO current = USER_HOLDER.get();
        if (current == null) {
            return Optional.empty();
        }

        // 如果已经加载成功或明确标记为失败，直接返回
        if (current.getUsername() != null) {
            return Optional.of(current);
        }

        Long userId = current.getId();
        if (userId == null) {
            return Optional.empty();
        }

        // 1. 查缓存
        UserDTO cached = CACHE.getIfPresent(userId);
        if (cached != null) {
            USER_HOLDER.set(cached);
            return Optional.of(cached);
        }

        // 2. 远程调用
        try {
            UserDTO fullUser = fetchUserFromRemote(userId);
            if (fullUser != null) {
                // 保证 username 不为 null，避免误判
                if (fullUser.getUsername() == null) {
                    fullUser.setUsername("");
                }
                CACHE.put(userId, fullUser);
                USER_HOLDER.set(fullUser);
                return Optional.of(fullUser);
            } else {
                // 远程返回 null，标记为失败，避免重复调用
                UserDTO failed = new UserDTO(userId);
                failed.setUsername("LOAD_FAILED");
                USER_HOLDER.set(failed);
                return Optional.of(failed);
            }
        } catch (Exception e) {
            log.error("获取用户信息失败, userId: {}", userId, e);
            UserDTO failed = new UserDTO(userId);
            failed.setUsername("LOAD_FAILED");
            USER_HOLDER.set(failed);
            return Optional.of(failed);
        }
    }

    private static UserDTO fetchUserFromRemote(Long userId) {
        String body = HttpRequest.post(userServiceUrl) // ★ 使用静态变量
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(new UserDTO(userId)))
                .timeout(3000)
                .execute()
                .body();
        Result<UserDTO> result = JSONUtil.toBean(JSONUtil.parseObj(body), TYPE_REF, true);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        } else {
            log.warn("用户服务返回异常(未找到id为{}的用户) code={}, msg={}", userId, result.getCode(), result.getMsg());
            return null;
        }
    }

    // 删除 main 方法，或移到测试包
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 6000; i++) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId((long) i);
            CACHE.put((long) i, userDTO);
            if (i==5555){
                TimeUnit.SECONDS.sleep(1);
                System.out.println("caffeine.asMap() = " + CACHE.asMap());
                System.out.println(userDTO.getId());
            }
        }
    }

}

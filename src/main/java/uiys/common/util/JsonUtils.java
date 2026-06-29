package uiys.common.util;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class JsonUtils {

    private static ObjectMapper objectMapper;

    @Resource
    private ObjectMapper springObjectMapper;

    @PostConstruct
    public void init() {
        objectMapper = springObjectMapper;
        // 如果 Spring 的 ObjectMapper 没有注册 JavaTimeModule，可以手动注册
        // 但 Spring Boot 4.x 默认已支持
    }

    public static String toJson(Object obj) {

        return objectMapper.writeValueAsString(obj);

    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);

    }
}
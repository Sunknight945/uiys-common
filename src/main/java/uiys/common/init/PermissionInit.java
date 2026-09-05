package uiys.common.init;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.SystemPropertyUtils;
import org.springframework.web.bind.annotation.*;
import uiys.common.dto.PermissionAutoCreateDTO;
import uiys.common.dto.PermissionDTO;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@EnableConfigurationProperties(PermissionInitProperty.class)
@ConditionalOnProperty(prefix = "uiys.permission.init", name = "enabled", havingValue = "true")
public class PermissionInit {
    ApplicationContext context;
    PermissionInitProperty property;

    public PermissionInit(ApplicationContext applicationContext, PermissionInitProperty property) {
        this.context = applicationContext;
        this.property = property;
    }

    //    @PostConstruct
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        String applicationName = context.getId();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("开始为:{},生成permission {}, 超级慢.", applicationName, stopWatch);
        Set<Class<?>> allClass;
        try {
            // 扫描全部 classpath，不限制包
            allClass = scanAllClasspath();
        } catch (Exception e) {
            log.error("开始生成permission,全局扫描class失败", e);
            return;
        }

        List<PermissionDTO> permissionDTOS = new ArrayList<>();
        for (Class<?> aClass : allClass) {
            RestController annotation = aClass.getAnnotation(RestController.class);
            if (annotation != null) {
                RequestMapping mapping = aClass.getAnnotation(RequestMapping.class);
                if (mapping != null) {
                    String[] value = mapping.value();
                    String perFix = String.join(",", value);
                    perFix = perFix.startsWith("/") ? perFix : "/" + perFix;
                    log.info("value: {}", perFix);
                    Method[] methods = aClass.getMethods();
                    for (Method method : methods) {

                        PermissionDTO permissionDTO = new PermissionDTO();
                        permissionDTO.setPermissionModule(applicationName);

                        if (method.isAnnotationPresent(GetMapping.class)) {
                            GetMapping getMapping = method.getAnnotation(GetMapping.class);
                            String next = String.join(",", getMapping.value());
                            next = next.startsWith("/") ? next : "/" + next;
                            String url = perFix + next;
                            permissionDTO.setMethod("GET");
                            permissionDTO.setPermissionCode(url);
                            permissionDTO.setPermissionName(applicationName + "模块" + url + "权限");
                            permissionDTO.setResourcePath(url);
                            permissionDTO.setDescription(applicationName + "模块" + url + "权限");
                            permissionDTOS.add(permissionDTO);
                            continue;
                        }
                        if (method.isAnnotationPresent(PostMapping.class)) {
                            PostMapping postMapping = method.getAnnotation(PostMapping.class);
                            String next = String.join(",", postMapping.value());
                            next = next.startsWith("/") ? next : "/" + next;
                            String url = perFix + next;
                            permissionDTO.setMethod("POST");
                            permissionDTO.setPermissionCode(url);
                            permissionDTO.setPermissionName(applicationName + "模块" + url + "权限");
                            permissionDTO.setResourcePath(url);
                            permissionDTO.setDescription(applicationName + "模块" + url + "权限");
                            permissionDTOS.add(permissionDTO);
                            continue;
                        }
                        if (method.isAnnotationPresent(DeleteMapping.class)) {
                            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                            String next = String.join(",", deleteMapping.value());
                            next = next.startsWith("/") ? next : "/" + next;
                            String url = perFix + next;
                            permissionDTO.setMethod("DELETE");
                            permissionDTO.setPermissionCode(url);
                            permissionDTO.setPermissionName(applicationName + "模块" + url + "权限");
                            permissionDTO.setResourcePath(url);
                            permissionDTO.setDescription(applicationName + "模块" + url + "权限");
                            permissionDTOS.add(permissionDTO);
                            continue;
                        }
                        if (method.isAnnotationPresent(PutMapping.class)) {
                            PutMapping putMapping = method.getAnnotation(PutMapping.class);
                            String next = String.join(",", putMapping.value());
                            next = next.startsWith("/") ? next : "/" + next;
                            String url = perFix + next;
                            permissionDTO.setPermissionCode(url);
                            permissionDTO.setPermissionName(applicationName + "模块" + url + "权限");
                            permissionDTO.setResourcePath(url);
                            permissionDTO.setDescription(applicationName + "模块" + url + "权限");
                            permissionDTO.setMethod("PUT");
                            permissionDTOS.add(permissionDTO);
                            continue;
                        }
                    }
                }
            }
        }
        PermissionAutoCreateDTO autoCreateDTO = new PermissionAutoCreateDTO();
        autoCreateDTO.setPermissionModule(applicationName);
        autoCreateDTO.setPermissions(permissionDTOS);
        String url = property.getCreatePermissionUrl();
        String body = HttpRequest.post(/*"http://localhost:10002/api/user/permission/createPermissionByModule"*/url)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(autoCreateDTO))   // cn.hutool.json.JSONUtil 或你们项目的 JsonUtils
                .timeout(60000)
                .execute()
                .body();
        stopWatch.stop();
        log.info("初始化结束 {}, 超级慢, {}", stopWatch, body);
    }

    /**
     * 扫描 classpath 下所有 class，无需指定包名
     */
    private Set<Class<?>> scanAllClasspath() throws IOException {
        Set<Class<?>> classes = new HashSet<>();
        // 全局匹配所有class
        String scanPattern = SystemPropertyUtils.resolvePlaceholders(
                PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "com/uiys/**/*.class");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
        Resource[] resources = resolver.getResources(scanPattern);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            try {
                Class<?> clazz = ClassUtils.forName(className, classLoader);
                classes.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // 第三方依赖缺失类，直接跳过，不阻断启动
            }
        }
        return classes;
    }

}

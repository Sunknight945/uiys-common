package uiys.common.init;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Table;
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
import uiys.common.util.TransUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@EnableConfigurationProperties(InitProperties.class)
@ConditionalOnProperty(prefix = "uiys.common.init", name = "enabled", havingValue = "true")
public class InitContainer {


    ApplicationContext context;


    public InitContainer(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    //    @PostConstruct
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("初始化开始 {}, 超级慢.", stopWatch);
        Map<String, String> tableNameMap = new HashMap<>();
        Set<Class<?>> allClass;
        try {
            // 扫描全部 classpath，不限制包
            allClass = scanAllClasspath();
        } catch (Exception e) {
            log.error("全局扫描class失败", e);
            return;
        }

        for (Class<?> aClass : allClass) {
            TableName annotation = aClass.getAnnotation(TableName.class);
            if (annotation != null) {
                tableNameMap.put(aClass.getName(), annotation.value());
            }
            Table annotation1 = aClass.getAnnotation(Table.class);
            if (annotation1 != null) {
                tableNameMap.put(aClass.getName(), annotation1.name());
            }
        }
        TransUtils instance = TransUtils.getInstance();
        tableNameMap.forEach(instance::setClassNameTable);
        stopWatch.stop();
        log.info("初始化结束 {}, 超级慢.", stopWatch);
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

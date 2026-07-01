package uiys.common.init;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "uiys.common.init")
public class InitProperties {
    // 标准的 Getter 和 Setter 方法
    /**
     * 是否启用初始化容器.
     */
    private boolean enabled = false;

}
package uiys.common.init;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "uiys.permission.init")
public class PermissionInitProperty {
    private Boolean enabled = false;
    private String createPermissionUrl = "";
}

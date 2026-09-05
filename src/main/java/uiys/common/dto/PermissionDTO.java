package uiys.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDTO {

    private Integer deleted;

    private Integer version;

    private String permissionModule;

    private String method;

    private String permissionCode;

    private String permissionName;

    private String resourcePath;

    private String description;

}

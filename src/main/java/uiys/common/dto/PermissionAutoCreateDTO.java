package uiys.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PermissionAutoCreateDTO implements Serializable {

    private String permissionModule;

    private List<PermissionDTO> permissions;


}

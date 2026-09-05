package uiys.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfoDto implements Serializable {
    private Long userId;
    private UserDTO user;
}

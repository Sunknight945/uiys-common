package uiys.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String avatar;

    private String phone;

    private String email;

    public UserDTO(Long id) {
        this.id = id;
    }
}

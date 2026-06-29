package uiys.common.data;

import lombok.Data;

@Data
public class PQVo<T> {
    private Long pageNum;
    private Long pageSize;
    private T vo;
}

package uiys.common.data;

import lombok.Data;

@Data
public class PQVo<T> {
    private Long pageNum;
    private Long pageSize;
    private T vo;

    private Long limitNum;
    private Long offsetNum;


    public Long getLimitNum() {
        return pageSize;
    }

    public Long getOffsetNum() {
        return (pageNum - 1) * pageSize;
    }
}

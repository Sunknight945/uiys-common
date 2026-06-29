package uiys.common.exception;

import lombok.Getter;
import uiys.common.constant.ResultCode;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String msg;

    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.msg = msg;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getName());
        this.code = resultCode.getCode();
        this.msg = resultCode.getName();
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getName(), cause);
        this.code = resultCode.getCode();
        this.msg = resultCode.getName();
    }

    public BusinessException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.msg = msg;
    }
}
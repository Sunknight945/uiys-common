package uiys.common.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uiys.common.constant.ResultCode;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    // ----- 成功响应 -----
    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getName(), data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }
    public static <T> Result<T> success(Integer code,String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    // ----- 失败响应 -----
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getName(), null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), msg, null);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getName(), null);
    }

    public static <T>Result<T> error(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}
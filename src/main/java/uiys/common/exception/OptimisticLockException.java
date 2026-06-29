package uiys.common.exception;


import uiys.common.constant.ResultCode;

public class OptimisticLockException extends BusinessException {

    public OptimisticLockException() {
        super(ResultCode.DATA_EXPIRED);
    }

    public OptimisticLockException(String message) {
        super(ResultCode.DATA_EXPIRED.getCode(), message);
    }
}
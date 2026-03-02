package io.promoengine.exception;

public class RuleSetException extends RuntimeException {

    private final int errorCode;

    public RuleSetException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}

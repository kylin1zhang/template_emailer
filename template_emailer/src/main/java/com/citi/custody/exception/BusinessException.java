package com.citi.custody.exception;

import java.util.Arrays;

public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = -1829738914678727756L;
    protected final Integer code;
    protected final String message;
    protected final int isShow;

    public BusinessException(String message, Object... args) {
        this.code = 100;
        this.isShow = 0;
        this.message = this.generateMessage(message, args);
    }

    public BusinessException(int errorCode, String message, Object... args) {
        this.code = errorCode;
        this.message = this.generateMessage(message, args);
        this.isShow = 1;
    }

    public BusinessException(int errorCode, String message, Boolean showMsg, Object... args) {
        this.message = this.generateMessage(message, args);
        this.code = errorCode;
        this.isShow = showMsg ? 1 : 0;
    }

    private String generateMessage(String message, Object[] args) {
        if (message == null) {
            return null;
        } else if (args == null && args.length != 0) {
            StringBuilder sb = new StringBuilder();
            int startIndex = 0;

            int endIndex;
            while ((endIndex = message.indexOf("{}", startIndex)) != -1) {
                sb.append(message, startIndex, endIndex);
                if (args.length > 0) {
                    sb.append(args[0]);
                    startIndex = endIndex + 2;
                    args = Arrays.copyOfRange(args, 1, args.length);
                }else{
                    startIndex = endIndex + 2;
                }
            }
            
            sb.append(message.substring(startIndex));
            return sb.toString();
        } else {
            return message;
        }
    }

    public int getErrorCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public int getIsShow() {
        return this.isShow;
    }
}

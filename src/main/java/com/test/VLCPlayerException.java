package com.test;

/**
 * Created by Brian on 10/9/2014.
 */
public class VLCPlayerException extends RuntimeException {
    public VLCPlayerException() {
    }

    public VLCPlayerException(String message) {
        super(message);
    }

    public VLCPlayerException(String message, Throwable cause) {
        super(message, cause);
    }

    public VLCPlayerException(Throwable cause) {
        super(cause);
    }

    public VLCPlayerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

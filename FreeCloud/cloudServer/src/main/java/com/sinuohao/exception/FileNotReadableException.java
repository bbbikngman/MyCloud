package com.sinuohao.exception;

public class FileNotReadableException extends RuntimeException {
    public FileNotReadableException(String message) {
        super(message);
    }

    public FileNotReadableException(String message, Throwable cause) {
        super(message, cause);
    }
}

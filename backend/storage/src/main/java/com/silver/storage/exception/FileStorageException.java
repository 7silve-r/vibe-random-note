package com.silver.storage.exception;

public class FileStorageException extends RuntimeException {
    private final int code;

    public FileStorageException(String message) {
        this(400, message);
    }

    public FileStorageException(int code, String message) {
        super(message);
        this.code = code;
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }
}

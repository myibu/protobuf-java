package com.github.myibu.proto;

public class ProtoProcessingException extends RuntimeException {
    public ProtoProcessingException() {
    }

    public ProtoProcessingException(String message) {
        super(message);
    }


    public ProtoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtoProcessingException(Throwable cause) {
        super(cause);
    }
}

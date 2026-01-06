package com.cbrviewer.exception;

public class LlmConfigNotFoundException extends RuntimeException {
    public LlmConfigNotFoundException(String message) {
        super(message);
    }
}

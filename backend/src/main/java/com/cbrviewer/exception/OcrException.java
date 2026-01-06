package com.cbrviewer.exception;

/**
 * Exception thrown when OCR processing fails.
 * This includes timeout errors, API failures, parsing errors, and retry exhaustion.
 */
public class OcrException extends RuntimeException {

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}

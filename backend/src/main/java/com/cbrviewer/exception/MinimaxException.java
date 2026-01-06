package com.cbrviewer.exception;

/**
 * Custom exception for Minimax TTS API errors.
 * Thrown when TTS synthesis fails due to API errors, timeouts, or configuration issues.
 */
public class MinimaxException extends Exception {

    /**
     * Constructs a new MinimaxException with the specified detail message.
     *
     * @param message the detail message explaining the error
     */
    public MinimaxException(String message) {
        super(message);
    }

    /**
     * Constructs a new MinimaxException with the specified detail message and cause.
     *
     * @param message the detail message explaining the error
     * @param cause the underlying cause of the error
     */
    public MinimaxException(String message, Throwable cause) {
        super(message, cause);
    }
}

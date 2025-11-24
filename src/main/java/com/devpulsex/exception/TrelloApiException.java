package com.devpulsex.exception;

public class TrelloApiException extends RuntimeException {
    public TrelloApiException(String message) { super(message); }
    public TrelloApiException(String message, Throwable cause) { super(message, cause); }
}

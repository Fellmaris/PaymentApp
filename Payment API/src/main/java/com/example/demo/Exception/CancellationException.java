package com.example.demo.Exception;

public class CancellationException extends RuntimeException {
    public CancellationException(String message) {
        super(message);
    }
}
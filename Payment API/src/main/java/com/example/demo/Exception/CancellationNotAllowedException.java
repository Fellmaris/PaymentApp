package com.example.demo.Exception;

public class CancellationNotAllowedException extends CancellationException {
    public CancellationNotAllowedException(String message) {
        super(message);
    }
}

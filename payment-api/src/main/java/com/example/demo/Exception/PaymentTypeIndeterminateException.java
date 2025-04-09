package com.example.demo.Exception;

public class PaymentTypeIndeterminateException extends CancellationException {
    public PaymentTypeIndeterminateException(String message) {
        super(message);
    }
}
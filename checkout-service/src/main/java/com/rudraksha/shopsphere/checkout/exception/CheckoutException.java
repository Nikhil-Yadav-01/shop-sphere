package com.rudraksha.shopsphere.checkout.exception;

import org.springframework.http.HttpStatus;

public class CheckoutException extends RuntimeException {

    private final HttpStatus status;

    public CheckoutException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public CheckoutException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

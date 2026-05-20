package com.rudraksha.shopsphere.checkout.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class OrderServiceUnavailableException extends RuntimeException {
    
    public OrderServiceUnavailableException(String message) {
        super(message);
    }

    public OrderServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

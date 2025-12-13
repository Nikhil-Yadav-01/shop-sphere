package com.rudraksha.shopsphere.returns.exception;

public class InvalidReturnException extends RuntimeException {
    public InvalidReturnException(String message) {
        super(message);
    }

    public InvalidReturnException(String message, Throwable cause) {
        super(message, cause);
    }
}

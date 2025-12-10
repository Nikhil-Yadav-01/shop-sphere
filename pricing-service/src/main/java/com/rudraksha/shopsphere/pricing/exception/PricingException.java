package com.rudraksha.shopsphere.pricing.exception;

public class PricingException extends RuntimeException {
    public PricingException(String message) {
        super(message);
    }
    
    public PricingException(String message, Throwable cause) {
        super(message, cause);
    }
}

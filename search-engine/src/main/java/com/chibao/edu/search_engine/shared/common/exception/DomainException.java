package com.chibao.edu.search_engine.shared.common.exception;

/**
 * Base exception for all domain exceptions.
 * Domain exceptions represent business rule violations.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

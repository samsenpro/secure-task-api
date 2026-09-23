package com.example.secureapi.exception;

/**
 * Petición bien formada que viola una regla de negocio (HTTP 422).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

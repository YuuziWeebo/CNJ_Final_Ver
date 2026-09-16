package com.glucoze.thesismanagement.common.exception;

public class DomainRuleViolationException extends IllegalArgumentException {

    public DomainRuleViolationException(String message) {
        super(message);
    }

    public DomainRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}

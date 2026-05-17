package com.example.Exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policy) {
        super(
            "Policy not found: "
            + policy
        );
    }
}
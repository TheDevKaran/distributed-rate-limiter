package com.example.DTO;

public class CheckResponse {

    private boolean allowed;

    public CheckResponse(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isAllowed() {
        return allowed;
    }
}
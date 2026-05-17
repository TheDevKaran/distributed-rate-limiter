package com.example.Exception;

public class UnsupportedAlgorithmException
        extends RuntimeException {

    public UnsupportedAlgorithmException(
            String algo
    ) {

        super(
            "Unknown algorithm: "
            + algo
        );
    }
}
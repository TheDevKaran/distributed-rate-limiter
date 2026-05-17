package com.example.Exception;

import com.example.Entity.AlgorithmType;

public class UnsupportedAlgorithmException
        extends RuntimeException {

    public UnsupportedAlgorithmException(
        Object algo
) {

    super(
        "Unknown algorithm: "
        + algo
    );
}
}
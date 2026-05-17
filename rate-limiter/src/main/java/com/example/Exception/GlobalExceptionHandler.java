package com.example.Exception;

import org.springframework.http.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.example.DTO.ErrorResponse;

import org.springframework.web.bind.MethodArgumentNotValidException;
import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            PolicyNotFoundException.class
    )
    public ResponseEntity<?> handlePolicyNotFound(
            PolicyNotFoundException ex
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.NOT_FOUND
                )
                .body(
                        Map.of(
                                "timestamp",
                                LocalDateTime.now(),

                                "status",
                                404,

                                "error",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            UnsupportedAlgorithmException.class
    )
    public ResponseEntity<?> handleAlgo(
            UnsupportedAlgorithmException ex
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.BAD_REQUEST
                )
                .body(
                        Map.of(
                                "timestamp",
                                LocalDateTime.now(),

                                "status",
                                400,

                                "error",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<?> handleGeneric(
            Exception ex
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(
                        Map.of(
                                "timestamp",
                                LocalDateTime.now(),

                                "status",
                                500,

                                "error",
                                "Something went wrong"
                        )
                );
    }

        @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<?> validation(
            MethodArgumentNotValidException ex
    ) {

        String msg =
            ex.getBindingResult()
            .getFieldError()
            .getDefaultMessage();

        return ResponseEntity
            .badRequest()
            .body(
                new ErrorResponse(
                    LocalDateTime.now(),
                    400,
                    msg
                )
            );
    }

    @ExceptionHandler(
        HttpRequestMethodNotSupportedException.class
        )
        public ResponseEntity<?> methodNotAllowed(
        HttpRequestMethodNotSupportedException ex
        ) {

        return ResponseEntity
                .status(
                HttpStatus.METHOD_NOT_ALLOWED
                )
                .body(
                new ErrorResponse(
                        LocalDateTime.now(),
                        405,
                        ex.getMessage()
                )
                );
        }
}
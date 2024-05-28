package com.amcglynn.myenergi.exception;

public class ClientException extends RuntimeException {
    public ClientException(int statusCode) {
        super("Failed with status code " + statusCode);
    }

    public ClientException(int statusCode, String message) {
        super("Failed with status code " + statusCode + " and message: " + message);
    }
}

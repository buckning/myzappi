package com.amcglynn.lwa;

public class ClientException extends RuntimeException {
    public ClientException(int statusCode) {
        super("Failed with status code " + statusCode);
    }
}

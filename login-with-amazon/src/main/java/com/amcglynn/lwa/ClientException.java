package com.amcglynn.lwa;

public class ClientException extends RuntimeException {
    public ClientException() {
        super("Failed making request");
    }
    public ClientException(int statusCode) {
        super("Failed with status code " + statusCode);
    }
}

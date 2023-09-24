package com.amcglynn.myzappi.api.rest;

public class ServerException extends RuntimeException {

    private int status;

    public ServerException(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

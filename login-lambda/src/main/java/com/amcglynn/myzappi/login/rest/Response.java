package com.amcglynn.myzappi.login.rest;

import java.util.Optional;

public class Response {
    private int status;
    private String body;

    public Response(int status) {
        this.status = status;
    }

    public Response(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public Optional<String> getBody() {
        return Optional.ofNullable(body);
    }

    public int getStatus() {
        return status;
    }
}

package com.amcglynn.myzappi.login.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Response {
    private int status;
    private String body;
    private Map<String, String> headers = new HashMap<>();

    public Response(int status) {
        this.status = status;
    }

    public Response(int status, Map<String, String> headers) {
        this(status);
        this.headers = headers;
    }

    public Response(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public Optional<String> getBody() {
        return Optional.ofNullable(body);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatus() {
        return status;
    }
}

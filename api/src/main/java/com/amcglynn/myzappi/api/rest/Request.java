package com.amcglynn.myzappi.api.rest;

import com.amcglynn.myzappi.core.model.UserId;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private UserId userId;  // user authenticated in this request. Null if not authenticated.
    private final RequestMethod method;
    private final String path;
    private String body;
    @Getter
    private final Map<String, String> headers;
    @Getter
    private final Map<String, String> queryStringParameters;

    public Request(UserId userId, RequestMethod method, String path, String body) {
        this(method, path, body);
        this.userId = userId;
    }

    public Request(RequestMethod method, String path) {
        this.method = method;
        this.path = path;
        this.headers = new HashMap<>();
        this.queryStringParameters = new HashMap<>();
    }

    public Request(RequestMethod method, String path, String body) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = new HashMap<>();
        this.queryStringParameters = new HashMap<>();
    }

    public Request(RequestMethod method, String path, String body, Map<String, String> headers, Map<String, String> queryStringParameters) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = headers;
        if (queryStringParameters != null) {
            this.queryStringParameters = queryStringParameters;
        } else {
            this.queryStringParameters = new HashMap<>();
        }
    }

    public RequestMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getBody() {
        return body;
    }

    public void setUserId(String userId) {
        this.userId = UserId.from(userId);
    }
}

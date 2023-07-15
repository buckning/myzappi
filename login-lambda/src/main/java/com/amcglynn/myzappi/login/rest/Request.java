package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.login.UserId;

public class Request {
    private UserId userId;  // user authenticated in this request. Null if not authenticated.
    private RequestMethod method;
    private String path;
    private String body;

    public Request(UserId userId, RequestMethod method, String path, String body) {
        this(method, path, body);
        this.userId = userId;
    }

    public Request(RequestMethod method, String path, String body) {
        this.method = method;
        this.path = path;
        this.body = body;
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

    public void setUserId(UserId userId) {
        this.userId = userId;
    }
}

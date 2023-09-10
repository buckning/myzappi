package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.login.Session;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Request {
    private UserId userId;  // user authenticated in this request. Null if not authenticated.
    private RequestMethod method;
    private String path;
    private String body;
    private Session session;
    @Getter
    private Map<String, String> headers;

    public Request(UserId userId, RequestMethod method, String path, String body) {
        this(method, path, body);
        this.userId = userId;
    }

    public Request(RequestMethod method, String path, String body) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = new HashMap<>();
    }

    public Request(RequestMethod method, String path, String body, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = headers;
    }

    public RequestMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public UserId getUserId() {
        return userId == null? UserId.from(session.getUserId()) : userId;
    }

    public Optional<Session> getSession() {
        return Optional.ofNullable(session);
    }

    public String getBody() {
        return body;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setUserId(String userId) {
        this.userId = UserId.from(userId);
    }
}

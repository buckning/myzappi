package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.login.Session;
import com.amcglynn.myzappi.login.UserId;

import java.util.Optional;

public class Request {
    private UserId userId;  // user authenticated in this request. Null if not authenticated.
    private RequestMethod method;
    private String path;
    private String body;
    private Session session;

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
        return new UserId(session.getUserId());
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
}

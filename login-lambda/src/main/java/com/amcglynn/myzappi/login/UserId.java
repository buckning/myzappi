package com.amcglynn.myzappi.login;

public class UserId {
    private String id;

    public UserId(String userId) {
        this.id = userId;
    }

    @Override
    public String toString() {
        return id;
    }
}

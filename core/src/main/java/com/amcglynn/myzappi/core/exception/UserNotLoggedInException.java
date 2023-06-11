package com.amcglynn.myzappi.core.exception;

public class UserNotLoggedInException extends RuntimeException {

    public UserNotLoggedInException(String userId) {
        super("User not logged in - " + userId);
    }
}

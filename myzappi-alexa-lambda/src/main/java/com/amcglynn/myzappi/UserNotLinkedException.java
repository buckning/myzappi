package com.amcglynn.myzappi;

public class UserNotLinkedException extends RuntimeException {

    public UserNotLinkedException(String userId) {
        super("User not linked - " + userId);
    }
}

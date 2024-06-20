package com.amcglynn.myzappi;

import lombok.Getter;

public class UserNotLinkedException extends RuntimeException {

    @Getter
    private String applicationId;
    public UserNotLinkedException(String userId, String applicationId) {
        super("User not linked - " + userId);
        this.applicationId = applicationId;
    }
}

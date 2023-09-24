package com.amcglynn.myzappi.core.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class AlexaUserId {
    private final String id;

    public static AlexaUserId from(String userId) {
        return new AlexaUserId(userId);
    }

    private AlexaUserId(String userId) {
        this.id = userId;
    }

    @Override
    public String toString() {
        return id;
    }
}

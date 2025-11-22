package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class UserId {
    private final String id;

    public static UserId from(String userId) {
        return new UserId(userId);
    }

    private UserId(String userId) {
        this.id = userId;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}

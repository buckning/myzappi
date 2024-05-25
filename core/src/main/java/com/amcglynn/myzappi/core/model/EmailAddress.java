package com.amcglynn.myzappi.core.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailAddress {

    @NonNull
    private final String emailAddress;

    public static EmailAddress from(String emailAddress) {
        return new EmailAddress(emailAddress);
    }

    public String toString() {
        return emailAddress;
    }
}

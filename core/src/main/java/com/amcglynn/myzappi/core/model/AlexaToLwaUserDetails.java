package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AlexaToLwaUserDetails {
    private final String alexaUserId;
    private final String lwaUserId;
    private final String zoneId;
}

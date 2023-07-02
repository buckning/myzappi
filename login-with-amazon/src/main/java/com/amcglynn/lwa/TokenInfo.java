package com.amcglynn.lwa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class TokenInfo {

    @JsonProperty("aud")
    private String audienceClaim;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("exp")
    private Long expires;

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("iat")
    private Long issuedAt;
}

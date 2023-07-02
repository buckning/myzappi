package com.amcglynn.lwa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LwaUserProfile {
    @JsonProperty("user_id")
    String userId;
    String name;
    String email;
}

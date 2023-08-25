package com.amcglynn.lwa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SkillMessageRequest {

    private long expiresAfterSeconds;
    private Object data;
}

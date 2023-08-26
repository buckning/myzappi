package com.amcglynn.sqs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SkillMessage {
    private long expiresAfterSeconds;
    private Object data;
}

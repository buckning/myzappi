package com.amcglynn.sqs;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class MyZappiSqsEvent {
    private String type;
    private String alexaBaseUrl;
    private String lwaUserId;
    private String alexaUserId;
}

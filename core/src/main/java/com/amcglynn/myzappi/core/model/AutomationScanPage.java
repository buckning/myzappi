package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class AutomationScanPage {
    private List<UserAutomations> userAutomations;
    private Map<String, String> lastEvaluatedKey;

    public boolean hasMore() {
        return lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class UserAutomations {
        private UserId userId;
        private List<Automation> automations;
    }
}

package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class StateReconcileRequest {
    private String requestId;
    private UserId userId;
    private int attempt;
    private Action action;
}

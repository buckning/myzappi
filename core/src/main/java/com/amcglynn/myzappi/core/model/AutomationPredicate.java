package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class AutomationPredicate {
    private String type;
    private String target;
    private AutomationOperator operator;
    private String value;

    public Optional<String> getTarget() {
        return Optional.ofNullable(target);
    }
}

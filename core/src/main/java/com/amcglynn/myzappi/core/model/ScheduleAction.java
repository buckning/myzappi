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
public class ScheduleAction {

    private String type;
    private String value;
    private String target;

    public ScheduleAction(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Optional<String> getTarget() {
        return Optional.ofNullable(target);
    }
}

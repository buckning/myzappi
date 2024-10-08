package com.amcglynn.lwa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * ASK SDK does not deserialize the timestamps correctly and throws an exception when getting reminders, so this is done as a workaround
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Reminders {
    private int totalCount;
    private List<Reminder> alerts;
}

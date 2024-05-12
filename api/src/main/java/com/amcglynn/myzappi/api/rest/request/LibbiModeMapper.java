package com.amcglynn.myzappi.api.rest.request;

import com.amcglynn.myenergi.LibbiMode;

import java.util.Map;
import java.util.Optional;

public class LibbiModeMapper {
    private static final Map<String, LibbiMode> MODES = Map.of(
            "on", LibbiMode.ON,
            "off", LibbiMode.OFF);

    public Optional<LibbiMode> getLibbiMode(String mode) {
        var valueFromMap = MODES.get(mode.toLowerCase().trim());

        if (valueFromMap != null) {
            return Optional.of(valueFromMap);
        }
        return Optional.empty();
    }
}

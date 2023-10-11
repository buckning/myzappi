package com.amcglynn.myzappi.mappers;

import com.amcglynn.myenergi.EddiMode;

import java.util.Map;
import java.util.Optional;

public class AlexaEddiModeMapper {
    private static final Map<String, EddiMode> MODES = Map.of("normal", EddiMode.NORMAL,
            "stopped", EddiMode.STOPPED,
            "stop", EddiMode.STOPPED);

    public Optional<EddiMode> getEddiMode(String mode) {
        var valueFromMap = MODES.get(mode.toLowerCase().trim());

        if (valueFromMap != null) {
            return Optional.of(valueFromMap);
        }
        return Optional.empty();
    }
}

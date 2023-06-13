package com.amcglynn.myzappi.mappers;

import com.amcglynn.myenergi.ZappiChargeMode;

import java.util.Map;
import java.util.Optional;

public class AlexaZappiChargeModeMapper {
    private static final Map<String, ZappiChargeMode> CHARGE_MODES = Map.of("eco plus", ZappiChargeMode.ECO_PLUS,
            "ecoplus", ZappiChargeMode.ECO_PLUS,
            "eco +", ZappiChargeMode.ECO_PLUS,
            "eco+", ZappiChargeMode.ECO_PLUS,
            "eco", ZappiChargeMode.ECO,
            "stop", ZappiChargeMode.STOP,
            "fast", ZappiChargeMode.FAST);

    public Optional<ZappiChargeMode> getZappiChargeMode(String mode) {
        var valueFromMap = CHARGE_MODES.get(mode.toLowerCase().trim());

        if (valueFromMap != null) {
            return Optional.of(valueFromMap);
        }
        return Optional.empty();
    }
}

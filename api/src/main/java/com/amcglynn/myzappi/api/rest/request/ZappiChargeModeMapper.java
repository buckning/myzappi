package com.amcglynn.myzappi.api.rest.request;

import com.amcglynn.myenergi.ZappiChargeMode;

import java.util.Map;
import java.util.Optional;

public class ZappiChargeModeMapper {
    private static final Map<String, ZappiChargeMode> CHARGE_MODES = Map.of(
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

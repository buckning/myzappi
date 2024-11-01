package com.amcglynn.myzappi.service;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

public class ControlPanelBuilder {

    private static final String BORDERLESS = "borderless";
    private static final String OUTLINED = "outlined";
    private static final Map<String, ZappiChargeMode> BUTTON_STYLES = Map.of(
            "stopStyle", ZappiChargeMode.STOP,
            "fastStyle", ZappiChargeMode.FAST,
            "ecoStyle", ZappiChargeMode.ECO,
            "ecoPlusStyle", ZappiChargeMode.ECO_PLUS
    );

    public RenderDocumentDirective buildControlPanel(HandlerInput handlerInput) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var summary = getZappiServiceOrThrow(handlerInput).getStatusSummary();

        return RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(buildDocument(summary.get(0), summary.get(0).getChargeMode()))
                .build();
    }

    public RenderDocumentDirective buildControlPanel(HandlerInput handlerInput, ZappiChargeMode newChargeMode) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var summary = getZappiServiceOrThrow(handlerInput).getStatusSummary();

        return RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(buildDocument(summary.get(0), newChargeMode))
                .build();
    }

    @SneakyThrows
    private Map<String, Object> buildDocument(ZappiStatusSummary summary, ZappiChargeMode chargeMode) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType = new TypeReference<>() {
        };

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("apl/zappi-control-panel.json");

        var contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        contents = contents.replace("${payload.importing}", new KiloWatt(summary.getGridImport()).toString());
        contents = contents.replace("${payload.exporting}", new KiloWatt(summary.getGridExport()).toString());
        contents = contents.replace("${payload.chargeRate}", new KiloWatt(summary.getEvChargeRate()).toString());
        contents = contents.replace("${payload.generating}", new KiloWatt(summary.getGenerated()).toString());
        contents = contents.replace("${payload.chargeAdded}", summary.getChargeAddedThisSession().toString());

        contents = contents.replace("${stopStyle}", getButtonStyle("stopStyle", chargeMode));
        contents = contents.replace("${fastStyle}", getButtonStyle("fastStyle", chargeMode));
        contents = contents.replace("${ecoStyle}", getButtonStyle("ecoStyle", chargeMode));
        contents = contents.replace("${ecoPlusStyle}", getButtonStyle("ecoPlusStyle", chargeMode));

        return mapper.readValue(contents, documentMapType);
    }

    private String getButtonStyle(String button, ZappiChargeMode chargeMode) {
        if (BUTTON_STYLES.get(button) == chargeMode) {
            return OUTLINED;
        }
        return BORDERLESS;
    }
}

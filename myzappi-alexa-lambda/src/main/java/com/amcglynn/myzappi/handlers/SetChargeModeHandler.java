package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.mappers.AlexaZappiChargeModeMapper;

import java.util.List;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class SetChargeModeHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final AlexaZappiChargeModeMapper mapper;
    private static final List<ZappiChargeMode> VALID_CHARGE_MODES = List.of(ZappiChargeMode.STOP, ZappiChargeMode.FAST,
            ZappiChargeMode.ECO, ZappiChargeMode.ECO_PLUS);

    public SetChargeModeHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        mapper = new AlexaZappiChargeModeMapper();
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetChargeMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());

        var request = handlerInput.getRequestEnvelope().getRequest();
        var intentRequest = (IntentRequest) request;
        var slots = intentRequest.getIntent().getSlots();
        var chargeModeSlot = slots.get("ChargeMode");

        var mappedChargeMode = mapper.getZappiChargeMode(chargeModeSlot.getValue().toLowerCase());

        if (mappedChargeMode.isEmpty() || !validChargeMode(mappedChargeMode.get())) {
            // it should not be possible to get to this block since Alexa should only allow requests with valid values in the slot
            return handlerInput.getResponseBuilder()
                    .withSpeech("Sorry, I don't recognise that charge mode.")
                    .withSimpleCard(Brand.NAME, "Sorry, I don't recognise that charge mode.")
                    .build();
        }
        var chargeMode = mappedChargeMode.get();

        zappiService.setChargeMode(chargeMode);
        return handlerInput.getResponseBuilder()
                .withSpeech("Changed charging mode to " + chargeMode.getDisplayName() + ". This may take a few minutes.")
                .withSimpleCard(Brand.NAME, "Changed charging mode to "
                        + chargeMode.getDisplayName() + ". This may take a few minutes.")
                .build();
    }

    private boolean validChargeMode(ZappiChargeMode chargeMode) {
        return VALID_CHARGE_MODES.contains(chargeMode);
    }
}

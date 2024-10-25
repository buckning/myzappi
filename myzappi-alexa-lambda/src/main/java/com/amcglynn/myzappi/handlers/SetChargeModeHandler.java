package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.mappers.AlexaZappiChargeModeMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class SetChargeModeHandler implements RequestHandler {

    private final MyEnergiService.Builder zappiServiceBuilder;
    private final AlexaZappiChargeModeMapper mapper;
    private final UserIdResolverFactory userIdResolverFactory;
    private final ExecutorService executorService;

    public SetChargeModeHandler(MyEnergiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                                ExecutorService executorService) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        mapper = new AlexaZappiChargeModeMapper();
        this.userIdResolverFactory = userIdResolverFactory;
        this.executorService = executorService;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetChargeMode"));
    }

    @SneakyThrows
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiServiceOrThrow();

        var request = handlerInput.getRequestEnvelope().getRequest();
        var intentRequest = (IntentRequest) request;
        var slots = intentRequest.getIntent().getSlots();
        var chargeModeSlot = slots.get("ChargeMode");

        var mappedChargeMode = mapper.getZappiChargeMode(chargeModeSlot.getValue().toLowerCase());

        if (mappedChargeMode.isEmpty()) {
            // it should not be possible to get to this block since Alexa should only allow requests with valid values in the slot
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "unrecognised-charge-mode"))
                    .withSimpleCard(Brand.NAME, "Sorry, I don't recognise that charge mode.")
                    .withShouldEndSession(false)
                    .build();
        }
        var newChargeMode = mappedChargeMode.get();

        var status = executorService.submit(() ->  zappiService.getStatusSummary());

        zappiService.setChargeMode(newChargeMode);

        var voiceResponse = voiceResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));
        var cardResponse = cardResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));

        try {
            var zappiStatus = status.get().get(0);
            var currentChargeMode = zappiStatus.getChargeMode();

            if (chargeModeEscalated(currentChargeMode, newChargeMode) && !new EvStatusSummary(zappiStatus).isConnected()) {
                voiceResponse += " " + voiceResponse(handlerInput, "connect-ev");
                cardResponse += "\n" + cardResponse(handlerInput, "connect-ev");
            }
        } catch (ExecutionException exception) {
            log.info("Failed to get Zappi status when changing the charge mode", exception);
        }

        return handlerInput.getResponseBuilder()
                .withShouldEndSession(false)
                .withSpeech(voiceResponse)
                .withSimpleCard(Brand.NAME, cardResponse)
                .build();
    }

    /**
     * Check if the new charge mode is escalated from the existing charge mode. Escalated meaning the new charge mode
     * pushes a greater charge rate to the car from the previous charge mode. Stop -> Eco+ -> Eco -> Fast -> Boost
     * If the user is escalating the charge mode, they would likely want to know if their car is not plugged in.
     * @param existingChargeMode the current charge mode
     * @param newChargeMode the new charge mode requested by the user
     * @return if the new charge mode is escalated from the existing charge mode
     */
    private boolean chargeModeEscalated(ZappiChargeMode existingChargeMode, ZappiChargeMode newChargeMode) {
        return newChargeMode.ordinal() < existingChargeMode.ordinal();
    }
}

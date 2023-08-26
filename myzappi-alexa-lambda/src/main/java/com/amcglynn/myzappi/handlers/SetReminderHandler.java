package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class SetReminderHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetReminder"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        if (!userNotGrantedPermissions(handlerInput)) {
            return handlerInput.getResponseBuilder()
                    .withAskForPermissionsConsentCard(List.of("alexa::alerts:reminders:skill:readwrite"))
                    .withSpeech(voiceResponse(handlerInput, "grantReminderPermission"))
                    .build();
        }
        return Optional.empty();
    }

    private boolean userNotGrantedPermissions(HandlerInput handlerInput) {
        var permissions = handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions();
        return permissions != null && permissions.getConsentToken() != null;
    }
}

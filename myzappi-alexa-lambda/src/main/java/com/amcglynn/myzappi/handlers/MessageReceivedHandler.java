package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myzappi.core.dal.AlexaToLwaLookUpRepository;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * This is invoked by Alexa message events and not through a voice intent.
 */
@Slf4j
public class MessageReceivedHandler implements RequestHandler {

    private final ReminderServiceFactory reminderServiceFactory;
    private final MyEnergiService.Builder zappiServiceBuilder;
    private final AlexaToLwaLookUpRepository userLookupRepository;

    public MessageReceivedHandler(ReminderServiceFactory reminderServiceFactory, MyEnergiService.Builder zappiServiceBuilder, AlexaToLwaLookUpRepository userLookupRepository) {
        this.reminderServiceFactory = reminderServiceFactory;
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userLookupRepository = userLookupRepository;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return "Messaging.MessageReceived".equals(handlerInput.getRequest().getType());
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var reminderService = reminderServiceFactory.newReminderService(handlerInput);

        var alexaUserId = handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getUserId();
        log.info("Message received event for Alexa user {}", alexaUserId);
        var lwaUser = userLookupRepository.read(alexaUserId);

        lwaUser.ifPresentOrElse(userDetails -> {
            log.info("User found for Alexa user {} - lwaUserId = {}", alexaUserId, userDetails.getLwaUserId());
            var zappiService = zappiServiceBuilder.build(userDetails::getLwaUserId);
            var summary = new EvStatusSummary(zappiService.getZappiServiceOrThrow().getStatusSummary().get(0));
            reminderService.handleReminderMessage(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions().getConsentToken(),
                    alexaUserId,
                    userDetails.getZoneId(),
                    summary::isConnected);
        }, () -> log.info("No user found for Alexa user {}", alexaUserId));

        return handlerInput.getResponseBuilder()
                .build();
    }
}
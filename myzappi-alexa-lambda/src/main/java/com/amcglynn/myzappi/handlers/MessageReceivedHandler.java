package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myzappi.core.dal.AlexaToLwaLookUpRepository;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * This is invoked by Alexa message events and not through a voice intent.
 */
@Slf4j
public class MessageReceivedHandler implements RequestHandler {

    private final ReminderServiceFactory reminderServiceFactory;
    private final ZappiService.Builder zappiServiceBuilder;
    private final AlexaToLwaLookUpRepository userLookupRepository;

    public MessageReceivedHandler(ReminderServiceFactory reminderServiceFactory, ZappiService.Builder zappiServiceBuilder, AlexaToLwaLookUpRepository userLookupRepository) {
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
        var lwaUser = userLookupRepository.read(alexaUserId);

        lwaUser.ifPresent(userDetails -> {
            var zappiService = zappiServiceBuilder.build(userDetails::getLwaUserId);
            var summary = new EvStatusSummary(zappiService.getStatusSummary().get(0));
            reminderService.handleReminderMessage(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions().getConsentToken(),
                    alexaUserId,
                    userDetails.getZoneId(),
                    summary::isConnected);
        });

        return handlerInput.getResponseBuilder()
                .build();
    }
}
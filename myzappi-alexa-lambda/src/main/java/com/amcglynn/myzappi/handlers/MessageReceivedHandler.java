package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.ServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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

    public MessageReceivedHandler(ReminderServiceFactory reminderServiceFactory, ZappiService.Builder zappiServiceBuilder, AmazonDynamoDB dynamoDB) {
        this.reminderServiceFactory = reminderServiceFactory;
        this.zappiServiceBuilder = zappiServiceBuilder;
        userLookupRepository = new AlexaToLwaLookUpRepository(dynamoDB);
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        log.info("Handler request type = {}", handlerInput.getRequest().getType());
        return "Messaging.MessageReceived".equals(handlerInput.getRequest().getType());
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var reminderService = reminderServiceFactory.newReminderService(handlerInput);
        try {
            var lwaUser = userLookupRepository.getLwaUserId(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getUserId());

            log.info("lwaUser = {}", lwaUser);

            lwaUser.ifPresentOrElse(lwaUserId -> {
                log.info("Resovled user {}", lwaUserId);
                // this needs to be replaced with a call to the DB to resolve the LWA user ID from the Alexa one
                var zappiService = zappiServiceBuilder.build(() -> lwaUserId);
                var summary = new EvStatusSummary(zappiService.getStatusSummary().get(0));
//                if (summary.isConnected()) {
                    // TODO schedule SQS job again for reminder start time - 5 minutes
                    log.info("E.V. is connected so updating reminder start time by 24 hours");
                    // TODO update by reminder start time + 24 hours
                    var alertToken = reminderService.delayBy24Hours(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions().getConsentToken());
                    log.info("Updated reminder {}", alertToken);
//                }
            }, () -> log.info("user not found, {}", handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getUserId()));


        } catch (ServiceException e) {
            log.error("Error when setting reminder update {}", e.getBody());
        } catch (Exception e) {
            log.error("Unexpected error ", e);
        }

        return handlerInput.getResponseBuilder()
                .build();
    }
}
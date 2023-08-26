package com.amcglynn.myzappi.service;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;

public class ReminderServiceFactory {

    public ReminderService newReminderService(HandlerInput handlerInput) {
        return new ReminderService(handlerInput.getServiceClientFactory().getReminderManagementService());
    }
}

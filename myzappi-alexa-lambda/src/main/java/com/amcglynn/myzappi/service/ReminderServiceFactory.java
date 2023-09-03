package com.amcglynn.myzappi.service;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amcglynn.lwa.LwaClient;

public class ReminderServiceFactory {

    private final SchedulerService schedulerService;

    public ReminderServiceFactory(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    public ReminderService newReminderService(HandlerInput handlerInput) {
        return new ReminderService(handlerInput.getServiceClientFactory().getReminderManagementService(), new LwaClient(),
                schedulerService);
    }
}

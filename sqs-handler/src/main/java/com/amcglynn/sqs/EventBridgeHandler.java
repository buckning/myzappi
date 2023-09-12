package com.amcglynn.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.service.ZappiService;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

/**
 * This handles an event bridge scheduler event, gets the user token and then calls the async postSkillMessage API
 * which the myzappi Alexa receives and handles the reminder updates.
 *
 * The flow is
 * 1. User asks Alexa to remind them to plug in their car
 * 1a. Alexa myzappi skill sets up a recurring reminder for the requested time
 * 1b. Alexa myzappi skill sets up a one time schedule 5 minutes before the reminder time to call back the reminder lambda (this class). The user ID is stored in the schedule body.
 * 2. 5 minutes before the time AWS eventbridge scheduler invokes this lambda (this class)
 * 2a. This class is primed with the Alexa messaging client ID and secret (from environment variables) and uses this to get a token
 * 2b. The token from 2a is used to post a skill message for the user.
 * 3. The myzappi skill gets invoked from the asynchronous request from 2b. The post skill request in myzappi will have
 *      the user in the request so it can read the Zappi information and delay reminders and create another schedule
 *      again and the process repeats
 */
@Slf4j
public class EventBridgeHandler implements RequestHandler<Object, Void> {

    private final Properties properties;
    private ServiceManager serviceManager;
    @Setter(AccessLevel.PACKAGE)
    private LwaClient lwaClient;
    private MyZappiScheduleHandler zappiScheduleHandler;

    public EventBridgeHandler() {
        properties = new Properties();
        serviceManager = new ServiceManager(properties);
        lwaClient = new LwaClient();
//        zappiScheduleHandler = new MyZappiScheduleHandler(new ZappiService.Builder(serviceManager.getLoginService(), serviceManager.getEncryptionService()));
    }

    EventBridgeHandler(Properties properties, LwaClient lwaClient, MyZappiScheduleHandler myZappiScheduleHandler) {
        this.properties = properties;
        this.lwaClient = lwaClient;
        this.zappiScheduleHandler = myZappiScheduleHandler;
    }

    public Void handleRequest(Object event, Context context) {

        var map = (LinkedHashMap<String, String>) event;
        try {
            if (map.isEmpty()) {
                log.info("Invalid event, exiting...");
                return null;
            }

            // event needs Alexa URL, LWA user ID and Alexa user ID
            var body = new MyZappiScheduleEvent(map);
            log.info("Received event class {}, body {}", event.getClass(), event);

            if (body.isAlexaReminder()) {
                var token = lwaClient.getMessagingToken(properties.getAlexaClientId(), properties.getAlexaClientSecret());
                log.info("Generated token {}", token.getTokenType());
                lwaClient.postSkillMessage(body.getAlexaBaseUrl(), body.getAlexaUserId(), token.getAccessToken(), new SkillMessage(3600, new MyZappiScheduleEvent()));
            }
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
        return null;
    }
}

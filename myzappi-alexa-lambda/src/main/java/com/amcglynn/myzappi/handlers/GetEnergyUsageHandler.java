package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryVoiceResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

@Slf4j
public class GetEnergyUsageHandler implements RequestHandler {

    private final ZappiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public GetEnergyUsageHandler(ZappiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappyServiceBuilder = zappyServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetEnergyUsage"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        // expected date format is 2023-05-06
        var date = parseSlot(handlerInput, "date");
        if (date.isEmpty() || date.get().length() != 10) {
            return getInvalidInputResponse(handlerInput);
        }
        log.info("Requested date is {}, localDate = {}", date.get(), LocalDate.now());
        var localDate = LocalDate.parse(date.get(), DateTimeFormatter.ISO_DATE);
        if (isInvalid(localDate)) {
            return getInvalidRequestedDateResponse(handlerInput);
        }

        // bug here due to DST when requesting energy usage for "today" when it is between 12AM - 1AM

        var zappiService = zappyServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));
        var history = zappiService.getEnergyUsage(localDate);

        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiDaySummaryVoiceResponse(history).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiDaySummaryCardResponse(history).toString())
                .build();
    }

    private Optional<Response> getInvalidRequestedDateResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("I cannot give you usage data for a time in the future.")
                .withSimpleCard(Brand.NAME,
                        "I cannot give you usage data for a time in the future.")
                .build();
    }

    private Optional<Response> getInvalidInputResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Please ask me for energy usage for a specific day.")
                .withSimpleCard(Brand.NAME,
                        "Please ask me for energy usage for a specific day.")
                .build();
    }

    private boolean isInvalid(LocalDate date) {
        return date.isAfter(LocalDate.now());
    }

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }
}

package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.graphing.EnergyUsageVisualisation;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryVoiceResponse;
import com.amcglynn.myzappi.service.GraphManagementService;
import com.amcglynn.myzappi.service.S3Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetEnergyUsageHandler implements RequestHandler {

    private final MyEnergiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;
    private final GraphManagementService graphManagementService;

    public GetEnergyUsageHandler(MyEnergiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                                 UserZoneResolver userZoneResolver) {
        this.zappyServiceBuilder = zappyServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
        this.graphManagementService = new GraphManagementService(new S3Service());
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetEnergyUsage"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var responseBuilder = handlerInput.getResponseBuilder();
        // expected date format is 2023-05-06
        var date = parseDate(handlerInput);
        if (date.isEmpty() || date.get().length() != 10) {
            return getInvalidInputResponse(handlerInput);
        }
        var userTimeZone = userZoneResolver.getZoneId(handlerInput);
        var localDate = LocalDate.parse(date.get(), DateTimeFormatter.ISO_DATE);

        if (isInvalid(localDate, userTimeZone)) {
            return getInvalidRequestedDateResponse(handlerInput);
        }

        var zappiService = zappyServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiServiceOrThrow();
        var rawHistory = zappiService.getRawEnergyHistory(localDate, userTimeZone);
        var history = new ZappiDaySummary(rawHistory);

        if (hasDisplayInterface(handlerInput)) {
            var visualisation = new EnergyUsageVisualisation(getScreenSize(handlerInput));
            var imageContent = visualisation.generateGraph(rawHistory, history, userTimeZone);

            var presignedUrl = graphManagementService.saveGraph(handlerInput, imageContent);

            var renderDocumentDirective = RenderDocumentDirective.builder()
                    .withToken("zappidaysummaryToken")
                    .withDocument(buildDocument(presignedUrl.toString()))
                    .build();
            responseBuilder.addDirective(renderDocumentDirective);
        }

        return responseBuilder
                .withSpeech(new ZappiDaySummaryVoiceResponse(locale, history).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiDaySummaryCardResponse(locale, history).toString())
                .withShouldEndSession(false)
                .build();
    }

    @SneakyThrows
    private Map<String, Object> buildDocument(String imageSourceUrl) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType = new TypeReference<>() {
        };

        InputStream inputStream = GetEnergyUsageGraphHandler.class.getClassLoader().getResourceAsStream("apl-energy-usage-graph.json");

        var contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        contents = contents.replace("${payload.backgroundEnergyUsageGraph}", imageSourceUrl);

        return mapper.readValue(contents, documentMapType);
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }

    private Dimension getScreenSize(HandlerInput handlerInput) {
        var viewPort = handlerInput.getRequestEnvelope().getContext().getViewport();
        return new Dimension(viewPort.getPixelWidth().intValue(), viewPort.getPixelHeight().intValue());
    }

    private Optional<Response> getInvalidRequestedDateResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "invalid-future-date"))
                .withSimpleCard(Brand.NAME,
                        cardResponse(handlerInput, "invalid-future-date"))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidInputResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "request-specific-date"))
                .withSimpleCard(Brand.NAME,
                        cardResponse(handlerInput, "request-specific-date"))
                .withShouldEndSession(false)
                .build();
    }

    /**
     * Invalid if date is in the future. Requesting for today is accepted but not future dates.
     * @param date today or historical date
     * @param userTimeZone time-zone of the user
     * @return true if invalid
     */
    private boolean isInvalid(LocalDate date, ZoneId userTimeZone) {
        return date.isAfter(LocalDate.now(userTimeZone));
    }

    private Optional<String> parseDate(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("date");
    }
}

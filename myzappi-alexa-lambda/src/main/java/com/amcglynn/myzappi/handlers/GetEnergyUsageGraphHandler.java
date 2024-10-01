package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetEnergyUsageGraphHandler implements RequestHandler {

    private final MyEnergiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;

    public GetEnergyUsageGraphHandler(MyEnergiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                                      UserZoneResolver userZoneResolver) {
        this.zappyServiceBuilder = zappyServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetEnergyUsageGraph"));
    }

    @SneakyThrows
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        if (!hasDisplayInterface(handlerInput)) {
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "graph-not-supported"))
                    .withSimpleCard(Brand.NAME,
                            cardResponse(handlerInput, "graph-not-supported"))
                    .withShouldEndSession(true)
                    .build();
        }

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType =
                new TypeReference<>() {};

        Map<String, Object> document = mapper.readValue(
                new File("apl-energy-usage-graph.json"),
                documentMapType);


        RenderDocumentDirective renderDocumentDirective = RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(document)
                .build();

        return handlerInput.getResponseBuilder()
                .withSpeech("Checkout out the graph")
                .withSimpleCard(Brand.NAME, "Checkout out the graph")
                .addDirective(renderDocumentDirective)
                .withShouldEndSession(false)
                .build();
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }
}

package com.amcglynn.myzappi.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.controller.EndpointRouter;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ApiRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final EndpointRouter endpointRouter;

    private final Properties properties;

    ApiRequestHandler(EndpointRouter endpointRouter, Properties properties) {
        this.properties = properties;
        this.endpointRouter = endpointRouter;
    }

    public ApiRequestHandler() {
        this.properties = new Properties();
        var serviceManager = new ServiceManager(properties);
        this.endpointRouter = new EndpointRouter(serviceManager);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setHeaders(Map.of());

        if (input.getHttpMethod() == null) {
            responseEvent.setStatusCode(200);
            log.info("Ignoring request with no HTTP method");
            return responseEvent;
        }

        var request = new Request(RequestMethod.valueOf(input.getHttpMethod()),
                input.getPath(),
                input.getBody(),
                input.getHeaders(),
                input.getQueryStringParameters());

        var response = endpointRouter.route(request);

        responseEvent.setStatusCode(response.getStatus());
        var responseHeaders = new HashMap<>(response.getHeaders());
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("Access-Control-Allow-Methods", "DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT");
        responseHeaders.put("Access-Control-Allow-Credentials", "true");

        if (input.getHeaders().containsKey("origin")) {
            var origin = input.getHeaders().get("origin");
            if ("https://www.myzappiunofficial.com".equals(origin) ||
                    "https://myzappiunofficial.com".equals(origin) ||
                    "http://localhost:4200".equals(origin)) {
                responseHeaders.put("Access-Control-Allow-Origin", origin);
            }
        }
        responseHeaders.put("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token");
        responseEvent.setHeaders(responseHeaders);

        response.getBody().ifPresent(responseEvent::setBody
        );

        return responseEvent;
    }
}

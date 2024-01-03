package com.amcglynn.myzappi.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import com.amcglynn.myzappi.api.service.SessionService;
import com.amcglynn.myzappi.api.service.TokenService;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.api.rest.controller.EndpointRouter;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CompleteLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final EndpointRouter endpointRouter;

    private final Properties properties;
    private AuthenticationService authenticationService;

    CompleteLoginHandler(EndpointRouter endpointRouter, Properties properties) {
        this.properties = properties;
        this.endpointRouter = endpointRouter;
    }

    public CompleteLoginHandler() {
        // TODO set up the redirect URL to /login
        this.properties = new Properties();
        var serviceManager = new ServiceManager(properties);
        this.endpointRouter = new EndpointRouter(serviceManager);
        this.authenticationService = new AuthenticationService(new TokenService(new LwaClientFactory()),
                new SessionService(new SessionRepository(serviceManager.getAmazonDynamoDB())));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setHeaders(Map.of());
        var responseHeaders = new HashMap<String, String>();

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

        Response response;
        if (request.getPath().equals("/authenticate")) {
            var session = authenticationService.authenticateLwaToken(request);

            if (session.isEmpty()) {
                log.info("User not authenticated");
                response = new Response(401);
            } else {
                response = new Response(200, responseHeaders);
                response.getHeaders().put("Set-Cookie", "sessionID=" + session.get().getSessionId() +
                        "; Max-Age=" + Session.DEFAULT_TTL.getSeconds() + "; Path=/; Secure; SameSite=None; HttpOnly; domain=.myzappiunofficial.com");
            }
        } else {
            response = endpointRouter.route(request);
        }

        responseEvent.setStatusCode(response.getStatus());
        responseHeaders.putAll(response.getHeaders());
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        responseHeaders.put("Access-Control-Allow-Credentials", "true");
        if (input.getHeaders().containsKey("origin")) {
            var origin = input.getHeaders().get("origin");
            if ("https://www.myzappiunofficial.com".equals(origin) ||
                    "https://myzappiunofficial.com".equals(origin) ||
                    "http://localhost:4200".equals(origin)) {
                responseHeaders.put("Access-Control-Allow-Origin", origin);
            }
        }
        responseHeaders.put("Access-Control-Allow-Headers", "content-type");
        responseEvent.setHeaders(responseHeaders);

        response.getBody().ifPresent(responseEvent::setBody
        );

        return responseEvent;
    }
}

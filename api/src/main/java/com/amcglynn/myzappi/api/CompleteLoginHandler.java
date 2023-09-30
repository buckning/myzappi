package com.amcglynn.myzappi.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.api.rest.controller.EndpointRouter;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CompleteLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LoginService loginService;
    private final SessionManagementService sessionManagementService;
    private final TemplateEngine templateEngine;
    private final EndpointRouter endpointRouter;

    private final Properties properties;
    private final String featureToggleUser;

    CompleteLoginHandler(LoginService loginService, SessionManagementService sessionManagementService,
                         EndpointRouter endpointRouter, Properties properties) {
        this.sessionManagementService = sessionManagementService;
        this.loginService = loginService;
        this.properties = properties;
        this.endpointRouter = endpointRouter;
        this.featureToggleUser = "notConfiguredForTest";

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    public CompleteLoginHandler() {
        // TODO set up the redirect URL to /login
        this.properties = new Properties();
        var serviceManager = new ServiceManager(properties);
        this.loginService = serviceManager.getLoginService();
        this.endpointRouter = new EndpointRouter(serviceManager);
        this.featureToggleUser = properties.getDevFeatureToggle();
        this.sessionManagementService = new SessionManagementService(new SessionRepository(serviceManager.getAmazonDynamoDB()),
                serviceManager.getEncryptionService(), new LwaClientFactory());

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
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
        var session = sessionManagementService.handle(input, responseEvent);
        session.ifPresent(request::setSession);

        var response = endpointRouter.route(request);

        responseEvent.setStatusCode(response.getStatus());
        var responseHeaders = new HashMap<>(response.getHeaders());
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
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

    private boolean devFeatureEnabled(Request request) {
        if (request.getSession().isEmpty()) {
            return false;
        }
        return featureToggleUser.equals(request.getUserId().toString());
    }
}

package com.amcglynn.myzappi.login;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.login.rest.EndpointRouter;
import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
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

        var request = new Request(RequestMethod.valueOf(input.getHttpMethod()), input.getPath(), input.getBody());

        var session = sessionManagementService.handle(input, responseEvent);
        session.ifPresent(request::setSession);

        // All JSON APIs are handled here
        if (!"/".equals(request.getPath())) {
            var response = endpointRouter.route(request);

            responseEvent.setStatusCode(response.getStatus());
            var responseHeaders = new HashMap<>(response.getHeaders());
            responseHeaders.put("Content-Type", "application/json");
            responseEvent.setHeaders(responseHeaders);

            response.getBody().ifPresent(body -> {
                responseEvent.setBody(body);
            });
            // TODO convert to JSON instead of string
            return responseEvent;
        }


        // all rendered HTML is here
        var thymeleafContext = new org.thymeleaf.context.Context();
        thymeleafContext.setVariable("loggedIn", session.isPresent());

        if (session.isPresent()) {
            var creds = loginService.readCredentials(session.get().getUserId());
            thymeleafContext.setVariable("registered", creds.isPresent());
            creds.ifPresent(credentials ->
                    thymeleafContext.setVariable("existingSerialNumber", "Your Zappi: " + credentials.getZappiSerialNumber()));
        }

        if ("GET".equals(input.getHttpMethod())) {
            return buildPage(responseEvent, thymeleafContext);
        }

        return responseEvent;
    }

    private boolean devFeatureEnabled(Request request) {
        if (request.getSession().isEmpty()) {
            return false;
        }
        return featureToggleUser.equals(request.getUserId().toString());
    }

    private APIGatewayProxyResponseEvent buildPage(APIGatewayProxyResponseEvent response, org.thymeleaf.context.Context thymeleafContext) {
        response.setStatusCode(200);
        var responseHeaders = new HashMap<>(response.getHeaders());
        responseHeaders.put("Content-Type", "text/html");
        response.setHeaders(responseHeaders);

        thymeleafContext.setVariable("pageTitle", Brand.NAME);
        thymeleafContext.setVariable("welcomeMessage", "Welcome to " + Brand.NAME + "!");
        thymeleafContext.setVariable("serialNumber", Brand.ZAPPI + " Serial Number:");
        thymeleafContext.setVariable("apiKey", Brand.ZAPPI + " API Key:");
        thymeleafContext.setVariable("apiUrl", properties.getLoginUrl());
        thymeleafContext.setVariable("logoutUrl", properties.getLogoutUrl());
        thymeleafContext.setVariable("registerUrl", properties.getRegisterUrl());

        // Process the Thymeleaf template
        String htmlContent = templateEngine.process("login", thymeleafContext);
        response.setBody(htmlContent.replace("\\/", "/"));
        return response;
    }
}

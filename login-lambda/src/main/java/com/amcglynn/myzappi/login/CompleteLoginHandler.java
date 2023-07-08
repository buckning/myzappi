package com.amcglynn.myzappi.login;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.service.LoginService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CompleteLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LoginService loginService;
    private final SessionManagementService sessionManagementService;
    private final TemplateEngine templateEngine;

    private final Properties properties;

    CompleteLoginHandler(LoginService loginService, SessionManagementService sessionManagementService,
                         Properties properties) {
        this.sessionManagementService = sessionManagementService;
        this.loginService = loginService;
        this.properties = properties;

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    public CompleteLoginHandler() {
        this.properties = new Properties();
        var serviceManager = new ServiceManager(properties);
        this.loginService = serviceManager.getLoginService();
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
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(Map.of());

        var thymeleafContext = new org.thymeleaf.context.Context();
        var session = sessionManagementService.handle(input, response);
        thymeleafContext.setVariable("loggedIn", session.isPresent());

        if (session.isPresent()) {
            var creds = loginService.readCredentials(session.get().getUserId());
            thymeleafContext.setVariable("registered", creds.isPresent());
            creds.ifPresent(credentials ->
                    thymeleafContext.setVariable("existingSerialNumber", "Your Zappi: " + credentials.getZappiSerialNumber()));


            if ("DELETE".equals(input.getHttpMethod())) {
                System.out.println("Delete received");
                if (creds.isPresent()) {
                    System.out.println("deleting creds for " + creds.get().getUserId());
                    loginService.delete(creds.get().getUserId());
                    response.setStatusCode(204);
                    return response;
                }
            }
        }

        if ("GET".equals(input.getHttpMethod())) {
            session.ifPresent(s -> handleLogout(input, response, s));
            if (session.isPresent()) {
                if (handleLogout(input, response, session.get())) {
                    return response;
                }
            }
            return buildPage(response, thymeleafContext);
        }
        session.ifPresent(value -> registerCredentials(value, input, response));

        return response;
    }

    private boolean handleLogout(APIGatewayProxyRequestEvent input, APIGatewayProxyResponseEvent response, Session session) {
        var map = input.getQueryStringParameters();
        if (map == null) {
            return false;
        }

        var logoutParam = map.get("logout");

        if (logoutParam == null) {
            return false;
        }

        if ("true".equals(logoutParam)) {
            System.out.println("Logging out session " + session.getSessionId());
            sessionManagementService.invalidateSession(session);
            var responseHeaders = new HashMap<>(response.getHeaders());
            responseHeaders.put("Set-Cookie", "sessionID=" + session.getSessionId() + "; expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure; HttpOnly");
            responseHeaders.put("Location", "https://myzappiunofficial.com");
            response.setHeaders(responseHeaders);
            response.setStatusCode(302);
            return true;
        }

        return false;
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

        // Process the Thymeleaf template
        String htmlContent = templateEngine.process("login", thymeleafContext);
        response.setBody(htmlContent.replace("\\/", "/"));
        return response;
    }

    /**
     * Legacy login is the non-Login with Amazon approach, where Alexa generates a login code and this needs to be
     * * entered on the login page with other details
     */
    private void registerCredentials(Session session, APIGatewayProxyRequestEvent input, APIGatewayProxyResponseEvent response) {
        try {
            var body = new ObjectMapper().readValue(input.getBody(), new TypeReference<CompleteLoginRequest>() {
            });

            var serialNumber = body.getSerialNumber().replaceAll("\\s", "").toLowerCase();

            if ("12345678".equals(serialNumber) && "myDemoApiKey".equals(body.getApiKey().trim())) {
                loginService.register(session.getUserId(),
                        SerialNumber.from(serialNumber),
                        SerialNumber.from(serialNumber), body.getApiKey().trim());
                response.setStatusCode(202);
                var responseHeaders = new HashMap<>(response.getHeaders());
                responseHeaders.put("Content-Type", "application/json");
                response.setHeaders(responseHeaders);
                return;
            }

            var zappiSerialNumber = discover(serialNumber, body.getApiKey().trim());

            if (zappiSerialNumber.isPresent()) {
                loginService.register(session.getUserId(),
                        SerialNumber.from(zappiSerialNumber.get()), // zappi serial number may be different to gateway/hub
                        SerialNumber.from(serialNumber), body.getApiKey().trim());
                response.setStatusCode(202);
                var responseHeaders = new HashMap<>(response.getHeaders());
                responseHeaders.put("Content-Type", "application/json");
                response.setHeaders(responseHeaders);
            } else {
                System.err.println("Could not find Zappi for system");
                response.setStatusCode(409);
            }

        } catch (JsonProcessingException e) {
            response.setStatusCode(400);
            e.printStackTrace();
        }
    }

    private Optional<String> discover(String serialNumber, String apiKey) {
        var client = new MyEnergiClient(serialNumber, apiKey);
        try {
            var zappis = client.getStatus().stream()
                    .filter(statusResponse -> statusResponse.getZappi() != null).findFirst();
            if (zappis.isPresent() && zappis.get().getZappi().size() > 0) {
                return Optional.of(zappis.get().getZappi().get(0).getSerialNumber());
            }
            System.out.println("Zappi device not found");
        } catch (ClientException e) {
            System.out.println("Unexpected error " + e.getMessage());
        }
        return Optional.empty();
    }
}

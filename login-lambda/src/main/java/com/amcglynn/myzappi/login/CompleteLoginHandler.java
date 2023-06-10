package com.amcglynn.myzappi.login;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.model.CompleteLoginState;
import com.amcglynn.myzappi.core.service.LoginCode;
import com.amcglynn.myzappi.core.service.LoginService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Collections;

public class CompleteLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LoginService loginService;
    private final TemplateEngine templateEngine;
    private final Properties properties;

    public CompleteLoginHandler() {
        this.properties = new Properties();
        var serviceManager = new ServiceManager(properties);
        this.loginService = serviceManager.getLoginService();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        if ("GET".equals(input.getHttpMethod())) {
            response.setStatusCode(200);
            response.setHeaders(Collections.singletonMap("Content-Type", "text/html"));


            // Create a Thymeleaf context and set variables
            var thymeleafContext = new org.thymeleaf.context.Context();
            thymeleafContext.setVariable("pageTitle", Brand.NAME);
            thymeleafContext.setVariable("welcomeMessage", "Welcome to " + Brand.NAME + "!");
            thymeleafContext.setVariable("loginCode", Brand.NAME + " Code:");
            thymeleafContext.setVariable("apiKey", Brand.NAME + " API Key:");
            thymeleafContext.setVariable("apiUrl", properties.getLoginUrl());

            // Process the Thymeleaf template
            String htmlContent = templateEngine.process("login", thymeleafContext);
            response.setBody(htmlContent.replace("\\/", "/"));
            return response;
        }

        try {
            var body = new ObjectMapper().readValue(input.getBody(), new TypeReference<CompleteLoginRequest>(){});

            var loginCode = body.getLoginCode().replaceAll("\\s","").toLowerCase();

            var result = loginService.completeLogin(LoginCode.from(loginCode), body.getApiKey().trim());
            if (CompleteLoginState.COMPLETE.equals(result.getState())) {
                response.setStatusCode(202);
                response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            } else {
                response.setStatusCode(404);
                response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            }

        } catch (JsonProcessingException e) {
            response.setStatusCode(400);
            e.printStackTrace();
        }

        return response;
    }
}

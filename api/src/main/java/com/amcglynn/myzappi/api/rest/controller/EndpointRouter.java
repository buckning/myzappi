package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionRepository;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import com.amcglynn.myzappi.api.service.SessionService;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.api.LwaClientFactory;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.api.service.TokenService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EndpointRouter {

    private final Map<String, RestController> handlers;
    private final AuthenticationService authenticationService;
    private final Properties properties;

    public EndpointRouter(ServiceManager serviceManager) {
        this(serviceManager, new HubController(
                        new RegistrationService(serviceManager.getLoginService(), serviceManager.getDevicesRepository(), new MyEnergiClientFactory())),
                new DevicesController(new RegistrationService(serviceManager.getLoginService(), serviceManager.getDevicesRepository(), new MyEnergiClientFactory())),
                new TariffController(serviceManager.getTariffService()),
                new LwaClientFactory());
    }

    public EndpointRouter(ServiceManager serviceManager, HubController hubController, DevicesController devicesController,
                          TariffController tariffController,
                          LwaClientFactory lwaClientFactory) {
        this(hubController, devicesController, tariffController, lwaClientFactory,
                new ScheduleController(serviceManager.getScheduleService()), serviceManager);
    }

    public EndpointRouter(HubController hubController, DevicesController devicesController, TariffController tariffController,
                          LwaClientFactory lwaClientFactory,
                          ScheduleController scheduleController, ServiceManager serviceManager) {
        this(hubController, devicesController, tariffController,
                new AuthenticationService(new TokenService(lwaClientFactory),
                        new SessionService(new SessionRepository(serviceManager.getAmazonDynamoDB()))),
                scheduleController,
                new EnergyCostController(serviceManager.getMyEnergiServiceBuilder(), serviceManager.getTariffService()),
                serviceManager.getProperties());
    }

    public EndpointRouter(HubController hubController, DevicesController devicesController, TariffController tariffController,
                          AuthenticationService authenticationService,
                          ScheduleController scheduleController, EnergyCostController energyCostController,
                          Properties properties) {
        this(hubController, devicesController, tariffController, authenticationService, scheduleController, energyCostController, new LogoutController(authenticationService), properties);
    }

    public EndpointRouter(HubController hubController, DevicesController devicesController,
                          TariffController tariffController,
                          AuthenticationService authenticationService,
                          ScheduleController scheduleController, EnergyCostController energyCostController,
                          LogoutController logoutController,
                          Properties properties) {

        handlers = new HashMap<>();
        handlers.put("/hub", hubController);
        handlers.put("/logout", logoutController);
        handlers.put("/v2/hub", hubController);
        handlers.put("/hub/refresh", hubController);
        handlers.put("/tariff", tariffController);
        handlers.put("/schedules", scheduleController);
        handlers.put("/schedules/{scheduleId}", scheduleController);
        handlers.put("/devices", devicesController);
        handlers.put("/devices/{deviceId}", devicesController);
        handlers.put("/energy-cost", energyCostController);

        this.authenticationService = authenticationService;
        this.properties = properties;
    }

    public Response route(Request request) {
        var session = authenticationService.authenticate(request);

        if (session.isEmpty()) {
            log.info("User not authenticated");
            return new Response(401);
        }

        handleAdminUserOnBehalfOf(request);

        try {
            log.info("Processing request {} {}", request.getMethod(), request.getPath());
            var controller = getController(request.getPath());

            if (controller.isEmpty()) {
                log.info("Controller not found for {}", request.getPath());
                return new Response(404);
            }
            log.info("Found controller {}", controller.getClass());
            var response = controller.get().handle(request);
            updateSessionCookie(request, session.get(), response);
            return response;
        } catch (ServerException e) {
            return new Response(e.getStatus());
        }
    }

    private Optional<RestController> getController(String path) {
        for (String pattern : handlers.keySet()) {
            if (matches(path, pattern)) {
                return Optional.of(handlers.get(pattern));
            }
        }
        return Optional.empty();
    }

    private boolean matches(String url, String pattern) {
        // Basic pattern matching logic to handle URLs with path variables
        if (pattern.equals(url)) {
            return true;
        }

        // Handle path variables by comparing segments
        String[] urlSegments = url.split("/");
        String[] patternSegments = pattern.split("/");

        if (urlSegments.length != patternSegments.length) {
            return false;
        }

        for (int i = 0; i < urlSegments.length; i++) {
            if (!patternSegments[i].equals(urlSegments[i]) && !patternSegments[i].startsWith("{")) {
                return false;
            }
        }

        return true;
    }

    private void updateSessionCookie(Request request, Session session, Response response) {
        var sessionFromHeaders = authenticationService.getSessionIdFromCookie(request.getHeaders());
        if (sessionFromHeaders.isEmpty()) {
            response.getHeaders().put("Set-Cookie", "sessionID=" + session.getSessionId() +
                    "; Max-Age=604800; Path=/; Secure; SameSite=None; HttpOnly; domain=.myzappiunofficial.com");
        }
    }

    private void handleAdminUserOnBehalfOf(Request request) {
        if (properties.getAdminUser().equals(request.getUserId().toString())
                && request.getHeaders().containsKey("on-behalf-of")
                && request.getMethod() == RequestMethod.GET) {
            log.info("Admin user detected, running API as {} on behalf of {}", request.getUserId(), request.getHeaders().get("on-behalf-of"));
            request.setUserId(request.getHeaders().get("on-behalf-of"));
        }
    }
}

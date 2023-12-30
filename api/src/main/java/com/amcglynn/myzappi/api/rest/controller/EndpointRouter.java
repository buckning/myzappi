package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.MyEnergiClientFactory;
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

@Slf4j
public class EndpointRouter {

    private final Map<String, RestController> handlers;
    private final AuthenticationService authenticationService;
    private final Properties properties;

    public EndpointRouter(ServiceManager serviceManager) {
        this(serviceManager, new HubController(
                        new RegistrationService(serviceManager.getLoginService(), serviceManager.getDevicesRepository(), new MyEnergiClientFactory())),
                new TariffController(serviceManager.getTariffService()),
                new LwaClientFactory());
    }

    public EndpointRouter(ServiceManager serviceManager, HubController hubController, TariffController tariffController,
                          LwaClientFactory lwaClientFactory) {
        this(hubController, tariffController, lwaClientFactory,
                new ScheduleController(serviceManager.getScheduleService()), serviceManager);
    }

    public EndpointRouter(HubController hubController, TariffController tariffController,
                          LwaClientFactory lwaClientFactory,
                          ScheduleController scheduleController, ServiceManager serviceManager) {
        this(hubController, tariffController,
                new AuthenticationService(new TokenService(lwaClientFactory),
                        new SessionService(new SessionRepository(serviceManager.getAmazonDynamoDB()))),
                scheduleController,
                new EnergyCostController(serviceManager.getZappiServiceBuilder(), serviceManager.getTariffService()),
                serviceManager.getProperties());
    }

    public EndpointRouter(HubController hubController, TariffController tariffController,
                          AuthenticationService authenticationService,
                          ScheduleController scheduleController, EnergyCostController energyCostController,
                          Properties properties) {

        handlers = new HashMap<>();
        handlers.put("/hub", hubController);
        handlers.put("/v2/hub", hubController);
        handlers.put("/hub/refresh", hubController);
        handlers.put("/tariff", tariffController);
        handlers.put("/schedule", scheduleController);
        handlers.put("/schedules", scheduleController);
        handlers.put("/energy-cost", energyCostController);

        this.authenticationService = authenticationService;
        this.properties = properties;
    }

    public Response route(Request request) {
        if (RequestMethod.OPTIONS == request.getMethod()) {
            return new Response(204);
        }

        if (!isAuthenticated(request)) {
            log.info("User not authenticated");
            return new Response(401);
        }

        handleAdminUserOnBehalfOf(request);

        try {
            log.info("{} {}", request.getMethod(), request.getPath());
            var controller = handlers.get(request.getPath());

            if (controller == null && request.getPath().startsWith("/schedules/")) {
                controller = handlers.get("/schedules");
            }
            if (controller == null) {
                log.info("Controller not found for {}", request.getPath());
                return new Response(404);
            }
            log.info("Found controller {}", controller.getClass());
            return controller.handle(request);
        } catch (ServerException e) {
            return new Response(e.getStatus());
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

    private boolean isAuthenticated(Request request) {
        return authenticationService.authenticate(request).isPresent();
    }
}

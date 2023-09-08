package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.dal.ScheduleRepository;
import com.amcglynn.myzappi.login.LwaClientFactory;
import com.amcglynn.myzappi.login.SessionManagementService;
import com.amcglynn.myzappi.login.SessionRepository;
import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
import com.amcglynn.myzappi.login.rest.Response;
import com.amcglynn.myzappi.login.rest.ServerException;
import com.amcglynn.myzappi.login.service.RegistrationService;
import com.amcglynn.myzappi.login.service.TokenService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EndpointRouter {

    private final Map<String, RestController> handlers;
    private final AuthenticateController authenticateController;

    public EndpointRouter(ServiceManager serviceManager) {
        this(serviceManager, new HubController(new RegistrationService(serviceManager.getLoginService())),
                new TariffController(serviceManager.getTariffService()),
                new LwaClientFactory());
    }

    public EndpointRouter(ServiceManager serviceManager, HubController hubController, TariffController tariffController,
                          LwaClientFactory lwaClientFactory) {
        this(hubController, tariffController, new SessionManagementService(new SessionRepository(serviceManager.getAmazonDynamoDB()),
                serviceManager.getEncryptionService(), lwaClientFactory), lwaClientFactory,
                new ScheduleController(new ScheduleRepository(serviceManager.getAmazonDynamoDB())));
    }

    public EndpointRouter(HubController hubController, TariffController tariffController,
                          SessionManagementService sessionManagementService, LwaClientFactory lwaClientFactory,
                          ScheduleController scheduleController) {
        this(hubController, tariffController,
                new AuthenticateController(new TokenService(lwaClientFactory), sessionManagementService),
                new LogoutController(sessionManagementService),
                scheduleController);
    }

    public EndpointRouter(HubController hubController, TariffController tariffController,
                          AuthenticateController authenticateController, LogoutController logoutController,
                          ScheduleController scheduleController) {

        handlers = new HashMap<>();
        handlers.put("/hub", hubController);
        handlers.put("/tariff", tariffController);
        handlers.put("/authenticate", authenticateController);
        handlers.put("/logout", logoutController);
        handlers.put("/schedule", scheduleController);

        this.authenticateController = authenticateController;
    }

    public Response route(Request request) {
        if (RequestMethod.OPTIONS == request.getMethod()) {
            return new Response(204);
        }

        // POST /authenticate does not require a sessionId
        if (!isAuthenticated(request)) {
            if (request.getSession().isEmpty()) {
                log.info("User not authenticated");
                return new Response(401);
            }
        }
        try {
            log.info("{} {}", request.getMethod(), request.getPath());
            var controller = handlers.get(request.getPath());
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

    private boolean isAuthenticated(Request request) {
        var routeEndpoint = request.getMethod() + " " + request.getPath();

        if ("POST /authenticate".equals(routeEndpoint)) {
            return true;
        }

        if (request.getHeaders().containsKey("Authorization")) {
            return isValidBearerToken(request, request.getHeaders().get("Authorization"));
        }

        return request.getSession().isPresent();
    }

    private boolean isValidBearerToken(Request request, String authorization) {
        // split token
        if (authorization == null) {
            return false;
        }

        var tokens = authorization.split("Bearer ");
        if (tokens.length == 2) {
            return authenticateController.isAuthenticated(request, tokens[1]);
        }

        return false;
    }
}

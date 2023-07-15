package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.login.LwaClientFactory;
import com.amcglynn.myzappi.login.SessionManagementService;
import com.amcglynn.myzappi.login.SessionRepository;
import com.amcglynn.myzappi.login.service.RegistrationService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EndpointRouter {

    private Map<String, RestController> handlers;

    public EndpointRouter(ServiceManager serviceManager) {
        handlers = new HashMap<>();
        handlers.put("POST /hub", new HubController(new RegistrationService(serviceManager.getLoginService())));
        handlers.put("DELETE /hub", new HubController(new RegistrationService(serviceManager.getLoginService())));
        handlers.put("GET /logout", new LogoutController(new SessionManagementService(new SessionRepository(serviceManager.getAmazonDynamoDB()),
                serviceManager.getEncryptionService(), new LwaClientFactory())));
    }
    public Response route(Request request) {
        if (request.getSession().isEmpty()) {
            log.info("User not authenticated");
            return new Response(401);
        }
        try {
            log.info("{} {}", request.getMethod(), request.getPath());
            var controller = handlers.get(request.getMethod() + " " + request.getPath());
            if (controller == null) {
                log.info("Controller not found for {} {}", request.getMethod(), request.getPath());
                return new Response(404);
            }
            log.info("Found controller {}", controller.getClass());
            return controller.handle(request);
        } catch (ServerException e) {
            return new Response(e.getStatus());
        }
    }
}

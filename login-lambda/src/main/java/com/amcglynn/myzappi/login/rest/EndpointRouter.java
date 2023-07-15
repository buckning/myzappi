package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.login.service.RegistrationService;

import java.util.HashMap;
import java.util.Map;

public class EndpointRouter {

    private Map<String, RestController> handlers;

    public EndpointRouter(ServiceManager serviceManager) {
        handlers = new HashMap<>();
        handlers.put("POST /hub", new HubController(new RegistrationService(serviceManager.getLoginService())));
        handlers.put("DELETE /hub", new HubController(new RegistrationService(serviceManager.getLoginService())));
    }
    public Response route(Request request) {
        try {
            System.out.println(request.getMethod()  + " " + request.getPath());
            var controller = handlers.get(request.getMethod() + " " + request.getPath());
            if (controller == null) {
                System.out.println("Controller not found for " + request.getMethod() + " " + request.getPath());
                return new Response(404);
            }
            System.out.println("Found controller " + controller.getClass());
            return controller.handle(request);
        } catch (ServerException e) {
            return new Response(e.getStatus());
        }
    }
}

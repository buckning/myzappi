package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
import com.amcglynn.myzappi.login.rest.Response;
import com.amcglynn.myzappi.login.rest.RestController;
import com.amcglynn.myzappi.login.rest.ServerException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScheduleController implements RestController {

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            return new Response(200, request.getBody());
        }
        if (request.getMethod() == RequestMethod.GET) {
            return new Response(200, "{}");
        }
        if (request.getMethod() == RequestMethod.DELETE) {
            return new Response(204);
        }
        log.info("Unsupported method for schedule - " + request.getMethod());
        throw new ServerException(404);
    }
}
package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.Response;

public interface RestController {
    Response handle(Request request);
}

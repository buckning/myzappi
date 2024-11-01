package com.amcglynn.myzappi.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.ResponseInterceptor;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.service.ControlPanelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AplResponseInterceptor implements ResponseInterceptor {

    @Override
    public void process(HandlerInput handlerInput, Optional<Response> response) {
        if (response.isPresent()) {
            var responseBuilder = response.get();
            if (responseBuilder.getDirectives().isEmpty() && hasDisplayInterface(handlerInput)) {
                responseBuilder.getDirectives()
                        .add(new ControlPanelBuilder().buildControlPanel(handlerInput));
            }
        }
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }
}

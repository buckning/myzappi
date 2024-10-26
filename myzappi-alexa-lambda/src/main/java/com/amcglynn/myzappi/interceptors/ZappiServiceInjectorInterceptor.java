package com.amcglynn.myzappi.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ZappiServiceInjectorInterceptor implements RequestInterceptor {

    private final MyEnergiService.Builder myenergiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final ExecutorService executorService;

    public ZappiServiceInjectorInterceptor(MyEnergiService.Builder myenergiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.myenergiServiceBuilder = myenergiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.executorService = Executors.newFixedThreadPool(1);
    }

    @Override
    public void process(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();

        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        requestAttributes.put("userId", userIdResolver.getUserId());

        var zappiService = myenergiServiceBuilder.build(userIdResolver).getZappiService();
        zappiService.ifPresent(service -> {
            requestAttributes.put("zappiService", service);
            requestAttributes.put("zappiStatusSummary", executorService.submit(() ->  service.getStatusSummary()));
        });
    }
}

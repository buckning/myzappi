package com.amcglynn.myzappi.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZappiServiceInjectorInterceptor implements RequestInterceptor {

    private final MyEnergiService.Builder myenergiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public ZappiServiceInjectorInterceptor(MyEnergiService.Builder myenergiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.myenergiServiceBuilder = myenergiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public void process(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();

        var zappiService = myenergiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiService();
        zappiService.ifPresent(service -> requestAttributes.put("zappiService", service));
    }
}

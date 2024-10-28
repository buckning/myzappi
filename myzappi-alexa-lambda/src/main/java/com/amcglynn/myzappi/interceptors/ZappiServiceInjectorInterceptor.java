package com.amcglynn.myzappi.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * It is convenient to get the summary or history of the Zappi device in many intent handlers, so they are started processing
 * concurrently before the intent handler is called, so that they can be used in the intent handler if needed. This
 * means that there is more network traffic but the user experience is improved as the response is faster and can have
 * a richer Alexa APL experience.
 */
@Slf4j
public class ZappiServiceInjectorInterceptor implements RequestInterceptor {

    private final MyEnergiService.Builder myenergiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;
    private final ExecutorService executorService;

    public ZappiServiceInjectorInterceptor(MyEnergiService.Builder myenergiServiceBuilder,
                                           UserZoneResolver userZoneResolver,
                                           UserIdResolverFactory userIdResolverFactory) {
        this.myenergiServiceBuilder = myenergiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.executorService = Executors.newFixedThreadPool(2);
        this.userZoneResolver = userZoneResolver;
    }

    @Override
    public void process(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();

        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        requestAttributes.put("userId", userIdResolver.getUserId());

        var userZoneId = userZoneResolver.getZoneId(handlerInput);
        requestAttributes.put("zoneId", userZoneId);

        var zappiService = myenergiServiceBuilder.build(userIdResolver).getZappiService();
        zappiService.ifPresent(service -> {
            requestAttributes.put("zappiService", service);
            requestAttributes.put("zappiStatusSummary", executorService.submit(() ->  service.getStatusSummary()));
            requestAttributes.put("zappiHistory", executorService.submit(() ->
                    service.getHistory(LocalDate.now(userZoneId), userZoneId)));
        });
    }
}

package com.amcglynn.myzappi;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.handlers.ChargeMyCarHandler;
import com.amcglynn.myzappi.handlers.FallbackHandler;
import com.amcglynn.myzappi.handlers.GetEnergyUsageHandler;
import com.amcglynn.myzappi.handlers.GetPlugStatusHandler;
import com.amcglynn.myzappi.handlers.GoGreenHandler;
import com.amcglynn.myzappi.handlers.LaunchHandler;
import com.amcglynn.myzappi.handlers.LogoutHandler;
import com.amcglynn.myzappi.handlers.MyZappiExceptionHandler;
import com.amcglynn.myzappi.handlers.QuitHandler;
import com.amcglynn.myzappi.handlers.SetChargeModeHandler;
import com.amcglynn.myzappi.handlers.StartBoostHandler;
import com.amcglynn.myzappi.handlers.StatusSummaryHandler;
import com.amcglynn.myzappi.handlers.StopBoostHandler;

public class MyZappiSkillStreamHandler extends SkillStreamHandler {

    public MyZappiSkillStreamHandler() {
        this(new ServiceManager(new Properties()), new UserIdResolverFactory(new LwaClient()), new UserZoneResolver(new LwaClient()));
    }

    public MyZappiSkillStreamHandler(ServiceManager serviceManager, UserIdResolverFactory userIdResolverFactory,
                                     UserZoneResolver userZoneResolver) {
        super(Skills.standard()
                .withSkillId(serviceManager.getSkillId())
                .addRequestHandler(new LaunchHandler())
                .addRequestHandler(new FallbackHandler())
                .addRequestHandler(new LogoutHandler(serviceManager.getLoginService()))
                .addRequestHandler(new StatusSummaryHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new StartBoostHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory, userZoneResolver))
                .addRequestHandler(new StopBoostHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GetPlugStatusHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GetEnergyUsageHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory, userZoneResolver))
                .addRequestHandler(new SetChargeModeHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GoGreenHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new ChargeMyCarHandler(serviceManager.getZappiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new QuitHandler())
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}

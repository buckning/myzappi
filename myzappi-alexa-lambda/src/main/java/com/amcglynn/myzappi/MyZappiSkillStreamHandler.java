package com.amcglynn.myzappi;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.handlers.ChargeMyCarHandler;
import com.amcglynn.myzappi.handlers.FallbackHandler;
import com.amcglynn.myzappi.handlers.GetEnergyUsageHandler;
import com.amcglynn.myzappi.handlers.GetPlugStatusHandler;
import com.amcglynn.myzappi.handlers.GoGreenHandler;
import com.amcglynn.myzappi.handlers.LaunchHandler;
import com.amcglynn.myzappi.handlers.LoginHandler;
import com.amcglynn.myzappi.handlers.LogoutHandler;
import com.amcglynn.myzappi.handlers.MyZappiExceptionHandler;
import com.amcglynn.myzappi.handlers.QuitHandler;
import com.amcglynn.myzappi.handlers.SetChargeModeHandler;
import com.amcglynn.myzappi.handlers.StartBoostHandler;
import com.amcglynn.myzappi.handlers.StatusSummaryHandler;
import com.amcglynn.myzappi.handlers.StopBoostHandler;

public class MyZappiSkillStreamHandler extends SkillStreamHandler {

    public MyZappiSkillStreamHandler() {
        this(new ServiceManager(new Properties()));
    }

    public MyZappiSkillStreamHandler(ServiceManager serviceManager) {
        super(Skills.standard()
                .addRequestHandler(new LaunchHandler())
                .addRequestHandler(new FallbackHandler())
                .addRequestHandler(new LoginHandler(serviceManager.getLoginService()))
                .addRequestHandler(new LogoutHandler(serviceManager.getLoginService()))
                .addRequestHandler(new StatusSummaryHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new StartBoostHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new StopBoostHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new GetPlugStatusHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new GetEnergyUsageHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new SetChargeModeHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new GoGreenHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new ChargeMyCarHandler(serviceManager.getZappiServiceBuilder()))
                .addRequestHandler(new QuitHandler())
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}

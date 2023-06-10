package com.amcglynn.myzappi;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.handlers.FallbackHandler;
import com.amcglynn.myzappi.handlers.LaunchHandler;
import com.amcglynn.myzappi.handlers.LoginHandler;
import com.amcglynn.myzappi.handlers.LogoutHandler;
import com.amcglynn.myzappi.handlers.MyZappiExceptionHandler;

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
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}

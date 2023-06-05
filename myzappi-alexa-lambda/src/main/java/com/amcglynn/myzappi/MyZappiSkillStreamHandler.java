package com.amcglynn.myzappi;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amcglynn.myzappi.handlers.FallbackHandler;
import com.amcglynn.myzappi.handlers.LaunchHandler;
import com.amcglynn.myzappi.handlers.LoginHandler;
import com.amcglynn.myzappi.handlers.LogoutHandler;
import com.amcglynn.myzappi.handlers.MyZappiExceptionHandler;

public class MyZappiSkillStreamHandler extends SkillStreamHandler {

    public MyZappiSkillStreamHandler() {
        super(Skills.standard()
                .addRequestHandler(new LaunchHandler())
                .addRequestHandler(new FallbackHandler())
                .addRequestHandler(new LoginHandler())
                .addRequestHandler(new LogoutHandler())
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}

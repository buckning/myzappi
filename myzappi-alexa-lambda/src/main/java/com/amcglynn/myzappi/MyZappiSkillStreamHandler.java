package com.amcglynn.myzappi;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amcglynn.myzappi.handlers.LaunchHandler;
import com.amcglynn.myzappi.handlers.MyZappiExceptionHandler;

public class MyZappiSkillStreamHandler extends SkillStreamHandler {

    public MyZappiSkillStreamHandler() {
        super(Skills.standard()
                .addRequestHandler(new LaunchHandler())
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}

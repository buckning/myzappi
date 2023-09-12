package com.amcglynn.sqs;

import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.core.service.ZappiService;

public class MyZappiScheduleHandler {

    private ZappiService.Builder zappiServiceBuilder;

    public MyZappiScheduleHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
    }

//    public void handle(MyZappiScheduleEvent event) {
//        var zappiService = zappiServiceBuilder.build(event::getLwaUserId);
//
//        // resolve schedule from DB for scheduleId
//        // check if the schedule is non-recurring, if so, delete it from schedule-details and schedule tables
//
//        //zappiService.setChargeMode(chargeMode);
//    }
}

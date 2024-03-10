package com.amcglynn.sqs;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.ZappiService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class MyZappiScheduleHandler {

    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final ScheduleService scheduleService;

    private final Map<String, BiConsumer<MyEnergiService, String>> handlers;

    public MyZappiScheduleHandler(ScheduleService scheduleService, MyEnergiService.Builder zappiServiceBuilder) {
        this.myEnergiServiceBuilder = zappiServiceBuilder;
        this.scheduleService = scheduleService;

        handlers = Map.of(
                "setChargeMode",    (zappiService, value) -> zappiService.getZappiServiceOrThrow().setChargeMode(ZappiChargeMode.valueOf(value)),
                "setBoostKwh",      (zappiService, value) -> zappiService.getZappiServiceOrThrow().startBoost(new KiloWattHour(Double.parseDouble(value))),
                "setBoostUntil",    (zappiService, value) -> zappiService.getZappiServiceOrThrow().startSmartBoost(LocalTime.parse(value)),
                "setBoostFor",      (zappiService, value) -> zappiService.getZappiServiceOrThrow().startSmartBoost(Duration.parse(value)),
                "setEddiMode",      (zappiService, value) -> zappiService.getEddiServiceOrThrow().setEddiMode(EddiMode.valueOf(value)),
                "setEddiBoostFor",  (zappiService, value) -> zappiService.getEddiServiceOrThrow().boostEddi(parseHeater(value), parseDuration(value))
        );
    }

    public void handle(MyZappiScheduleEvent event) {
        var zappiService = myEnergiServiceBuilder.build(event::getLwaUserId);

        // resolve schedule from DB for scheduleId
        var scheduleId = event.getScheduleId();

        var schedule = scheduleService.getSchedule(scheduleId);

        if (schedule.isEmpty()) {
            return;
        }

        var handler = handlers.get(schedule.get().getAction().getType());

        if (handler == null) {
            log.info("No handler found for schedule action type {}", schedule.get().getAction().getType());
            return;
        }

        if (schedule.get().getRecurrence() == null) {
            scheduleService.deleteLocalSchedule(scheduleId);
        }
        handler.accept(zappiService, schedule.get().getAction().getValue());
    }

    private int parseHeater(String value) {
        // parse tank value from PT45M;tank=2
        var tokens = value.split(";");

        if (tokens.length < 2) {
            return 1;   // default to heater 1
        }
        value = tokens[1].split("=")[1];

        return Integer.parseInt(value);
    }

    private Duration parseDuration(String value) {
        var tokens = value.split(";");
        return Duration.parse(tokens[0]);
    }
}

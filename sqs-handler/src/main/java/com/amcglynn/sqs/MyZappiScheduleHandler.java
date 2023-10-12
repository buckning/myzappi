package com.amcglynn.sqs;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.ZappiService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class MyZappiScheduleHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final ScheduleService scheduleService;

    private final Map<String, BiConsumer<ZappiService, String>> handlers;

    public MyZappiScheduleHandler(ScheduleService scheduleService, ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.scheduleService = scheduleService;

        handlers = Map.of(
                "setChargeMode",    (zappiService, value) -> zappiService.setChargeMode(ZappiChargeMode.valueOf(value)),
                "setBoostKwh",      (zappiService, value) -> zappiService.startBoost(new KiloWattHour(Double.parseDouble(value))),
                "setBoostUntil",    (zappiService, value) -> zappiService.startSmartBoost(LocalTime.parse(value)),
                "setBoostFor",      (zappiService, value) -> zappiService.startSmartBoost(Duration.parse(value)),
                "setEddiMode",      (zappiService, value) -> zappiService.setEddiMode(EddiMode.valueOf(value)),
                "setEddiBoostFor",  (zappiService, value) -> zappiService.boostEddi(Duration.parse(value))
        );
    }

    public void handle(MyZappiScheduleEvent event) {
        var zappiService = zappiServiceBuilder.build(event::getLwaUserId);

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
}

package com.amcglynn.sqs;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.SerialNumber;
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

    private final Map<String, BiConsumer<MyEnergiService, ScheduleAction>> handlers;

    public MyZappiScheduleHandler(ScheduleService scheduleService, MyEnergiService.Builder zappiServiceBuilder) {
        this.myEnergiServiceBuilder = zappiServiceBuilder;
        this.scheduleService = scheduleService;

        handlers = Map.ofEntries(
                Map.entry("setChargeMode", this::setChargeMode),
                Map.entry("setZappiMgl",    (myEnergiService, scheduleAction) -> myEnergiService.getZappiServiceOrThrow()
                        .setMgl(SerialNumber.from(scheduleAction.getTarget().get()), Integer.parseInt(scheduleAction.getValue()))),
                Map.entry("setSmartBoost",   (myEnergiService, scheduleAction) -> myEnergiService.getZappiServiceOrThrow()
                        .startSmartBoost(parseSmartBoostKwh(scheduleAction), parseSmartBoostTime(scheduleAction))),
                Map.entry("setBoostKwh",      (myEnergiService, scheduleAction) -> myEnergiService.getZappiServiceOrThrow()
                        .startBoost(new KiloWattHour(Double.parseDouble(scheduleAction.getValue())))),
                Map.entry("setBoostUntil",    (myEnergiService, scheduleAction) -> myEnergiService.getZappiServiceOrThrow()
                        .startSmartBoost(LocalTime.parse(scheduleAction.getValue()))),
                Map.entry("setBoostFor",      (myEnergiService, scheduleAction) -> myEnergiService.getZappiServiceOrThrow()
                        .startSmartBoost(Duration.parse(scheduleAction.getValue()))),
                Map.entry("setEddiMode",      (myEnergiService, scheduleAction) -> myEnergiService.getEddiServiceOrThrow()
                        .setEddiMode(EddiMode.valueOf(scheduleAction.getValue()))),
                Map.entry("setEddiBoostFor",  (myEnergiService, scheduleAction) -> myEnergiService.getEddiServiceOrThrow()
                        .boostEddi(parseHeater(scheduleAction.getValue()), parseDuration(scheduleAction.getValue()))),
                Map.entry("setLibbiChargeFromGrid", (myEnergiService, scheduleAction) -> myEnergiService.getLibbiService()
                        .get().setChargeFromGrid(myEnergiService.getUserId(), SerialNumber.from(scheduleAction.getTarget().get()), Boolean.parseBoolean(scheduleAction.getValue()))),
                Map.entry("setLibbiChargeTarget", (myEnergiService, scheduleAction) -> myEnergiService.getLibbiService()
                        .get().setChargeTarget(myEnergiService.getUserId(), SerialNumber.from(scheduleAction.getTarget().get()), Integer.parseInt(scheduleAction.getValue()))),
                Map.entry("setLibbiEnabled", (myEnergiService, scheduleAction) -> myEnergiService.getLibbiService()
                        .get().setMode(SerialNumber.from(scheduleAction.getTarget().get()), parseLibbiMode(scheduleAction)))
        );
    }

    private LibbiMode parseLibbiMode(ScheduleAction scheduleAction) {
        var enabled = Boolean.parseBoolean(scheduleAction.getValue());
        if (enabled) {
            return LibbiMode.ON;
        }
        return LibbiMode.OFF;
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

        zappiService.getZappiService().ifPresent(zappiSvc -> zappiSvc.setLocalTimeSupplier(() -> LocalTime.now(schedule.get().getZoneId())));
        handler.accept(zappiService, schedule.get().getAction());
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

    private LocalTime parseSmartBoostTime(ScheduleAction scheduleAction) {
        // smart boost scheduleAction format = "kwh;time"
        var tokens = scheduleAction.getValue().split(";");
        return LocalTime.parse(tokens[1]);
    }

    private KiloWattHour parseSmartBoostKwh(ScheduleAction scheduleAction) {
        // smart boost scheduleAction format = "kwh;time"
        var tokens = scheduleAction.getValue().split(";");
        return new KiloWattHour(Double.parseDouble(tokens[0]));
    }

    private void setChargeMode(MyEnergiService myEnergiService, ScheduleAction scheduleAction) {
        // this is for backwards compatibility with old schedules where the target was not set
        if (scheduleAction.getTarget().isPresent()) {
            myEnergiService.getZappiServiceOrThrow()
                    .setChargeMode(SerialNumber.from(scheduleAction.getTarget().get()), ZappiChargeMode.valueOf(scheduleAction.getValue()));
            return;
        }
        myEnergiService.getZappiServiceOrThrow()
                .setChargeMode(ZappiChargeMode.valueOf(scheduleAction.getValue()));
    }
}

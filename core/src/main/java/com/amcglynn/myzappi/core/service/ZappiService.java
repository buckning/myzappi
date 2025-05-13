package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.Phase;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.SerialNumber;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class ZappiService {

    private MyEnergiClient client;
    private Supplier<LocalTime> localTimeSupplier;

    public ZappiService(MyEnergiClient client) {
        this.client = client;
        this.localTimeSupplier = LocalTime::now;
    }

    public void setLocalTimeSupplier(Supplier<LocalTime> localTimeSupplier) {
        this.localTimeSupplier = localTimeSupplier;
    }

    public List<ZappiStatusSummary> getStatusSummary() {
        return client.getZappiStatus().getZappi()
                .stream().map(ZappiStatusSummary::new).collect(Collectors.toList());
    }

    public ZappiStatusSummary getStatusSummary(SerialNumber serialNumber) {
        return client.getZappiStatus(serialNumber.toString()).getZappi()
                .stream().map(ZappiStatusSummary::new).findAny().get();
    }

    public void setChargeMode(ZappiChargeMode chargeMode) {
        client.setZappiChargeMode(chargeMode);
    }

    public void setChargeMode(SerialNumber serialNumber, ZappiChargeMode chargeMode) {
        client.setZappiChargeMode(serialNumber.toString(), chargeMode);
    }

    public void startBoost(final KiloWattHour targetChargeAmount) {
        client.boost(targetChargeAmount);
    }

    /**
     * @deprecated don't use this method. It makes some bad assumptions with the predicted kilowatt-hours to add. There
     * can be factors that could cause the end time not possible to achieve, like the onboard charge rate of the EV.
     *
     * Start Smart Boost for a certain duration. The myenergi APIs expect everything to be rounded to the nearest 15 minutes.
     * @param duration minimum duration of the boost. This will be rounded to the nearest 15 minutes of the requested time.
     * @return end time of the boost
     */
    @Deprecated
    public LocalTime startSmartBoost(final Duration duration) {
        var boostEndTime = roundToNearest15Mins(duration);

        // get the zappi charge added this session
        // get the zappi phase
        // if the phase is single phase, the charge rate is 7.3kW
        // if the phase is 3 phase, the charge rate is 22kW
        // take into account the charge already in the EV so add the charge on top of what is already in the EV.

        // https://support.myenergi.com/hc/en-gb/articles/5780558509201-ECO-ECO-charge-rates-in-a-three-phase-zappi

        var status = client.getZappiStatus();
        var zappiStatus = status.getZappi().get(0);
        var chargeAlreadyInEv = zappiStatus.getChargeAddedThisSessionKwh();

        var phase = Phase.from(zappiStatus.getPhase());
        var kiloWattHoursToAdd = duration.toMinutes() * (phase.getMaxChargeRate() / 60);
        var chargeNeeded = chargeAlreadyInEv + kiloWattHoursToAdd;
        client.boost(boostEndTime, clampBoost(Math.floor(chargeNeeded)));
        return boostEndTime;
    }

    public KiloWattHour clampBoost(double kwh) {
        if (kwh < 0) {
            return new KiloWattHour(0);
        } else if (kwh > 99) {
            return new KiloWattHour(99);
        }

        return new KiloWattHour(kwh);
    }

    /**
     * @deprecated don't use this method. It makes some bad assumptions with the predicted kilowatt-hours to add. There
     * can be factors that could cause the end time not possible to achieve, like the onboard charge rate of the EV.
     *
     * @param endTime expected end time (which may not be accurate)
     * @return expected end time
     */
    @Deprecated
    public LocalTime startSmartBoost(final LocalTime endTime) {
        var boostEndTime = roundToNearest15Mins(endTime);

        var duration = calculateDuration(localTimeSupplier.get(), boostEndTime);

        startSmartBoost(duration);
        return boostEndTime;
    }

    /**
     * Start smart boost.
     * @param kiloWattHours Target kilowatt-hours to reach
     * @param endTime finish charging at - localtime from the user. No zone information is required, myenergi will figure this out based on the zappi configuration
     */
    public void startSmartBoost(final KiloWattHour kiloWattHours, final LocalTime endTime) {
        var boostEndTime = roundToNearest15Mins(endTime);
        client.boost(boostEndTime, clampBoost(Math.floor(kiloWattHours.getDouble())));
    }

    private Duration calculateDuration(LocalTime time1, LocalTime time2) {
        if (time2.isBefore(time1)) {
            // subtract a second from midnight to get the last second of the day otherwise the duration will be from
            // midnight to time1 and not time1 to midnight
            LocalTime oneSecondToMidnight = LocalTime.MIDNIGHT.minus(1, ChronoUnit.SECONDS);
            Duration untilMidnight = Duration.between(time1, oneSecondToMidnight).plus(1, ChronoUnit.SECONDS);
            Duration fromMidnightToTime2 = Duration.between(LocalTime.MIDNIGHT, time2);
            return untilMidnight.plus(fromMidnightToTime2);   // adjust for the second taken away above
        } else {
            return Duration.between(time1, time2);
        }
    }

    public void stopBoost() {
        client.stopBoost();
    }

    /**
     * Get energy usage for a date. Note that this will return the values for the day in UTC.
     * @param localDate specific date to retrieve energy usage
     * @return summary of energy usage for the day
     */
    public ZappiDaySummary getEnergyUsage(LocalDate localDate) {
        return new ZappiDaySummary(client.getZappiHistory(localDate).getReadings());
    }

    /**
     * Return a time-zone adjusted energy usage for a date. This will return readings in UTC but they will be adjusted
     * by an offset.
     * @param localDate specific date to retrieve energy usage
     * @param userZone time zone of the consumer.
     * @return summary of energy usage for the day
     */
    public ZappiDaySummary getEnergyUsage(LocalDate localDate, ZoneId userZone) {
        var utcTime = LocalTime.of(0, 0);   // start from 0:00AM local time for the user and then convert that to UTC for the API
        var userTime = utcTime.atDate(localDate).atZone(userZone)
                .withZoneSameInstant(ZoneId.of("UTC"));
        return new ZappiDaySummary(client.getZappiHistory(userTime.toLocalDate(), userTime.toLocalTime().getHour()).getReadings());
    }

    public void unlockZappi() {
        client.unlockZappi();
    }

    public List<ZappiHistory> getHourlyHistory(LocalDate date, ZoneId userZone) {
        var utcTime = LocalTime.of(0, 0);   // start from 0:00AM local time for the user and then convert that to UTC for the API
        var userTime = utcTime.atDate(date).atZone(userZone)
                .withZoneSameInstant(ZoneId.of("UTC"));
        return client.getZappiHourlyHistory(userTime.toLocalDate(), userTime.toLocalTime().getHour()).getReadings();
    }

    public List<ZappiHistory> getHistory(LocalDate date, ZoneId userZone) {
        var utcTime = LocalTime.of(0, 0);   // start from 0:00AM local time for the user and then convert that to UTC for the API
        var userTime = utcTime.atDate(date).atZone(userZone)
                .withZoneSameInstant(ZoneId.of("UTC"));
        return client.getZappiHistory(userTime.toLocalDate(), userTime.toLocalTime().getHour()).getReadings();
    }

    private LocalTime roundToNearest15Mins(Duration duration) {
        var targetChargeEndTime = localTimeSupplier.get().plus(duration);
        return roundToNearest15Mins(targetChargeEndTime);
    }

    private LocalTime roundToNearest15Mins(LocalTime endTime) {
        var overflow15Minutes = endTime.getMinute() % 15;
        if (overflow15Minutes > 7) {
            endTime = endTime.minus(endTime.getSecond(), ChronoUnit.SECONDS);
            return endTime.plus((15 - overflow15Minutes), ChronoUnit.MINUTES);
        }
        endTime = endTime.minus(endTime.getSecond(), ChronoUnit.SECONDS);
        return endTime.minus(overflow15Minutes, ChronoUnit.MINUTES);
    }

    public List<ZappiHistory> getRawEnergyHistory(LocalDate localDate, ZoneId userTimeZone) {
        var utcTime = LocalTime.of(0, 0);   // start from 0:00AM local time for the user and then convert that to UTC for the API
        var userTime = utcTime.atDate(localDate).atZone(userTimeZone)
                .withZoneSameInstant(ZoneId.of("UTC"));
        return client.getZappiHistory(userTime.toLocalDate(), userTime.toLocalTime().getHour()).getReadings();
    }

    /**
     * Set the Minimum Green Level (MGL) for the Zappi. This is a value between 1 and 100.
     */
    public void setMgl(SerialNumber serialNumber, int mgl) {
        client.setZappiMgl(serialNumber.toString(), mgl);
    }
}

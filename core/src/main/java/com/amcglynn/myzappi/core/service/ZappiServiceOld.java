package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.MockMyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class ZappiServiceOld {

    private MyEnergiClient client;
    private Supplier<LocalTime> localTimeSupplier;
    private final boolean hasEddi;

    private ZappiServiceOld(LoginService loginService, String user) {
        var userId = UserId.from(user);
        var creds = loginService.readCredentials(userId);
        var devices = loginService.readDevices(userId);
        if (creds.isEmpty()) {
            throw new UserNotLoggedInException(user);
        }
        var eddiSerialNumberOpt = getEddiSerialNumber(devices);
        hasEddi = eddiSerialNumberOpt.isPresent();

        log.info("Found eddi {}", eddiSerialNumberOpt);
        var decryptedApiKey = creds.get().getApiKey();
        var hubSerialNumber = creds.get().getSerialNumber().toString();
        // find zappi serial number from devices
        var zappiSerialNumber = getZappiSerialNumber(devices);

        if ("12345678".equals(hubSerialNumber) && "myDemoApiKey".equals(decryptedApiKey)) {
            client = new MockMyEnergiClient();
        } else{
            client = new MyEnergiClient(zappiSerialNumber.toString(), hubSerialNumber,
                    eddiSerialNumberOpt.map(SerialNumber::toString).orElse(null), decryptedApiKey);
        }

        // Zappi control APIs work off of local time and not UTC. Times in the retrieve Zappi information API is in UTC.
        localTimeSupplier = () -> LocalTime.now(ZoneId.of("Europe/London"));
    }

    private Optional<SerialNumber> getEddiSerialNumber(List<MyEnergiDevice> devices) {
        return devices.stream()
                .filter(EddiDevice.class::isInstance)
                .findFirst()
                .map(MyEnergiDevice::getSerialNumber);
    }

    private SerialNumber getZappiSerialNumber(List<MyEnergiDevice> devices) {
        return devices.stream()
                .filter(ZappiDevice.class::isInstance)
                .findFirst()
                .map(MyEnergiDevice::getSerialNumber)
                .get();
    }

    /**
     * Only for unit testing
     * @param client mock client
     */
    protected void setClient(MyEnergiClient client) {
        this.client = client;
    }

    public void setLocalTimeSupplier(Supplier<LocalTime> localTimeSupplier) {
        this.localTimeSupplier = localTimeSupplier;
    }

    public List<ZappiStatusSummary> getStatusSummary() {
        return client.getZappiStatus().getZappi()
                .stream().map(ZappiStatusSummary::new).collect(Collectors.toList());
    }

    public void setChargeMode(ZappiChargeMode chargeMode) {
        client.setZappiChargeMode(chargeMode);
    }

    public void startBoost(final KiloWattHour targetChargeAmount) {
        client.boost(targetChargeAmount);
    }

    /**
     * Start Smart Boost for a certain duration. The myenergi APIs expect everything to be rounded to the nearest 15 minutes.
     * @param duration minimum duration of the boost. This will be rounded to the nearest 15 minutes of the requested time.
     * @return end time of the boost
     */
    public LocalTime startSmartBoost(final Duration duration) {
        var boostEndTime = roundToNearest15Mins(duration);
        client.boost(boostEndTime);
        return boostEndTime;
    }

    public LocalTime startSmartBoost(final LocalTime endTime) {
        var boostEndTime = roundToNearest15Mins(endTime);
        client.boost(boostEndTime);
        return boostEndTime;
    }

    public void stopBoost() {
        client.stopBoost();
    }

    public void setEddiMode(EddiMode mode) {
        if (!hasEddi) {
            log.info("Eddi not configured");
            throw new MissingDeviceException("Eddi not available");
        }
        log.info("Setting eddi mode to {}", mode);
        client.setEddiMode(mode);
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
            return endTime.plus((15 - overflow15Minutes), ChronoUnit.MINUTES);
        }
        return endTime.minus(overflow15Minutes, ChronoUnit.MINUTES);
    }

    public void boostEddi(Duration duration) {
        boostEddi(1, duration);
    }

    public void stopEddiBoost() {
        stopEddiBoost(1);
    }

    public void boostEddi(int heaterNumber, Duration duration) {
        validateEddi(heaterNumber);
        client.boostEddi(duration, heaterNumber);
    }

    public void stopEddiBoost(int heaterNumber) {
        validateEddi(heaterNumber);
        client.stopEddiBoost(heaterNumber);
    }

    private void validateEddi(int heaterNumber) {
        if (!hasEddi) {
            log.info("Eddi not configured");
            throw new MissingDeviceException("Eddi not available");
        }

        if (heaterNumber < 1 || heaterNumber > 2) {
            log.info("Invalid heater number {}", heaterNumber);
            throw new IllegalArgumentException("Invalid heater number");
        }
    }

    public static class Builder {
        private final LoginService loginService;

        public Builder(LoginService loginService) {
            this.loginService = loginService;
        }

//        public ZappiService build(UserIdResolver userIdResolver) {
//            return new ZappiService(loginService, userIdResolver.getUserId());
//        }
    }
}

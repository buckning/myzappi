package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ZappiService {

    private MyEnergiClient client;
    private Supplier<LocalTime> localTimeSupplier;

    private ZappiService(LoginService loginService, EncryptionService encryptionService, String user) {
        var creds = loginService.readCredentials(user);
        if (creds.isEmpty() || !loginService.isLoggedIn(creds.get())) {
            throw new UserNotLoggedInException(user);
        }

        client = new MyEnergiClient(creds.get().getSerialNumber().toString(),
                encryptionService.decrypt(creds.get().getEncryptedApiKey().get()));
        localTimeSupplier = LocalTime::now;
    }

    /**
     * Only for unit testing
     * @param client mock client
     */
    protected void setClient(MyEnergiClient client) {
        this.client = client;
    }

    /**
     * Only for unit testing
     * @param localTimeSupplier mock LocalTime supplier
     */
    protected void setLocalTimeSupplier(Supplier<LocalTime> localTimeSupplier) {
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

    public ZappiDaySummary getEnergyUsage(LocalDate localDate) {
        return new ZappiDaySummary(client.getZappiHistory(localDate).getReadings());
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

    public static class Builder {
        private final LoginService loginService;
        private final EncryptionService encryptionService;

        public Builder(LoginService loginService, EncryptionService encryptionService) {
            this.loginService = loginService;
            this.encryptionService = encryptionService;
        }

        public ZappiService build(String userId) {
            return new ZappiService(loginService, encryptionService, userId);
        }
    }
}

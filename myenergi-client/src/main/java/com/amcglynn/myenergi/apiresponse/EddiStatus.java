package com.amcglynn.myenergi.apiresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class EddiStatus {
    @JsonProperty("deviceClass")
    private String deviceClass = "EDDI";
    @JsonProperty("sno")
    private String serialNumber;
    @JsonProperty("bsm")
    private int boostMode; // 1 if boosting
    @JsonProperty("che")
    private double energyTransferredKwh; // total kWh transferred this session
    @JsonProperty("div")
    private long diversionAmountWatts;
    @JsonProperty("gen")
    private long solarGenerationWatts;
    @JsonProperty("grd")
    private long gridWatts;
    @JsonProperty("hno")
    private int activeHeater;
    @JsonProperty("ht1")
    private String tank1Name;
    @JsonProperty("ht2")
    private String tank2Name;
    @JsonProperty("sta")
    private int status; // 1=Paused, 3=Diverting, 4=Boost, 5=Max Temp Reached, 6=Stopped

    public EddiStatus(String serialNumber, int status) {
        this.serialNumber = serialNumber;
        this.status = status;
    }

    public static EddiStatusBuilder builder() {
        return new EddiStatusBuilder();
    }

    public static class EddiStatusBuilder {
        private final EddiStatus eddiStatus = new EddiStatus();

        public EddiStatusBuilder serialNumber(String serialNumber) {
            eddiStatus.setSerialNumber(serialNumber);
            return this;
        }
        public EddiStatusBuilder boostMode(int boostMode) {
            eddiStatus.setBoostMode(boostMode);
            return this;
        }
        public EddiStatusBuilder energyTransferredKwh(double energyTransferredKwh) {
            eddiStatus.setEnergyTransferredKwh(energyTransferredKwh);
            return this;
        }
        public EddiStatusBuilder diversionAmountWatts(long diversionAmountWatts) {
            eddiStatus.setDiversionAmountWatts(diversionAmountWatts);
            return this;
        }
        public EddiStatusBuilder generatedWatts(long generatedWatts) {
            eddiStatus.setSolarGenerationWatts(generatedWatts);
            return this;
        }
        public EddiStatusBuilder gridWatts(long gridWatts) {
            eddiStatus.setGridWatts(gridWatts);
            return this;
        }
        public EddiStatusBuilder activeHeater(int activeHeater) {
            eddiStatus.setActiveHeater(activeHeater);
            return this;
        }
        public EddiStatusBuilder tank1Name(String tank1Name) {
            eddiStatus.setTank1Name(tank1Name);
            return this;
        }
        public EddiStatusBuilder tank2Name(String tank2Name) {
            eddiStatus.setTank2Name(tank2Name);
            return this;
        }
        public EddiStatusBuilder status(int status) {
            eddiStatus.setStatus(status);
            return this;
        }
        public EddiStatus build() {
            return eddiStatus;
        }
    }
}

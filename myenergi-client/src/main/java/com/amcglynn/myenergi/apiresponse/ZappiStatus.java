package com.amcglynn.myenergi.apiresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZappiStatus {
    @Getter
    @JsonProperty("sno")
    private String serialNumber;
    @Getter
    @JsonProperty("gen")
    private Long solarGeneration = 0L;
    @Getter
    @JsonProperty("div")
    private Long carDiversionAmountWatts = 0L;
    @Getter
    @JsonProperty("che")
    private Double chargeAddedThisSessionKwh = 0.0;
    @Getter
    @JsonProperty("grd")
    private Long gridWatts = 0L; // minus means pushing back to grid, positive means importing
    @Getter
    @JsonProperty("zmo")
    private int zappiChargeMode;  //1=Fast, 2=Eco, 3=Eco+, 4=Stopped
    @Getter
    @JsonProperty("lck")
    private int lockStatus = -1;
    // mgl = minimum green level - https://support.myenergi.com/hc/en-gb/articles/15587880239249-What-is-the-Minimum-Green-Level-MGL
    @Getter
    @JsonProperty("mgl")
    private int mgl = -1;
    @Getter
    @JsonProperty("sta")
    private int chargeStatus;

    @Getter
    @JsonProperty("pha")
    private int phase;

    @Getter
    @JsonProperty("fwv")
    private String firmwareVersion;
    @Getter
    @JsonProperty("pst")        // plug status
    private String evConnectionStatus;

    public ZappiStatus(String serialNumber, Long solarGeneration, Long carDiversionAmountWatts, Double chargeAddedThisSessionKwh, Long gridWatts, int zappiChargeMode, int chargeStatus, String evConnectionStatus) {
        this.serialNumber = serialNumber;
        this.solarGeneration = solarGeneration;
        this.carDiversionAmountWatts = carDiversionAmountWatts;
        this.chargeAddedThisSessionKwh = chargeAddedThisSessionKwh;
        this.gridWatts = gridWatts;
        this.zappiChargeMode = zappiChargeMode;
        this.chargeStatus = chargeStatus;
        this.evConnectionStatus = evConnectionStatus;
    }

    public ZappiStatus(String serialNumber, Long solarGeneration, Long carDiversionAmountWatts, Double chargeAddedThisSessionKwh, Long gridWatts, int zappiChargeMode, int chargeStatus, String evConnectionStatus, int lockStatus, String firmwareVersion) {
        this(serialNumber, solarGeneration, carDiversionAmountWatts, chargeAddedThisSessionKwh, gridWatts, zappiChargeMode, chargeStatus, evConnectionStatus);
        this.lockStatus = lockStatus;
        this.firmwareVersion = firmwareVersion;
    }

    public ZappiStatus() {
    }

    public static ZappiStatusBuilder builder() {
        return new ZappiStatusBuilder();
    }

    @JsonProperty("sno")
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @JsonProperty("gen")
    public void setSolarGeneration(Long solarGeneration) {
        this.solarGeneration = solarGeneration;
    }

    @JsonProperty("div")
    public void setCarDiversionAmountWatts(Long carDiversionAmountWatts) {
        this.carDiversionAmountWatts = carDiversionAmountWatts;
    }

    @JsonProperty("che")
    public void setChargeAddedThisSessionKwh(Double chargeAddedThisSessionKwh) {
        this.chargeAddedThisSessionKwh = chargeAddedThisSessionKwh;
    }

    @JsonProperty("grd")
    public void setGridWatts(Long gridWatts) {
        this.gridWatts = gridWatts;
    }

    @JsonProperty("zmo")
    public void setZappiChargeMode(int zappiChargeMode) {
        this.zappiChargeMode = zappiChargeMode;
    }

    @JsonProperty("sta")
    public void setChargeStatus(int chargeStatus) {
        this.chargeStatus = chargeStatus;
    }

    @JsonProperty("lck")
    public void setLockStatus(int lockStatus) {
        this.lockStatus = lockStatus;
    }

    @JsonProperty("pst")
    public void setEvConnectionStatus(String evConnectionStatus) {
        this.evConnectionStatus = evConnectionStatus;
    }

    public static class ZappiStatusBuilder {
        private String serialNumber;
        private Long solarGeneration;
        private Long carDiversionAmountWatts;
        private Double chargeAddedThisSessionKwh;
        private Long gridWatts;
        private int zappiChargeMode;
        private int chargeStatus;
        private String evConnectionStatus;

        ZappiStatusBuilder() {
        }

        public ZappiStatusBuilder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public ZappiStatusBuilder solarGeneration(Long solarGeneration) {
            this.solarGeneration = solarGeneration;
            return this;
        }

        public ZappiStatusBuilder carDiversionAmountWatts(Long carDiversionAmountWatts) {
            this.carDiversionAmountWatts = carDiversionAmountWatts;
            return this;
        }

        public ZappiStatusBuilder chargeAddedThisSessionKwh(Double chargeAddedThisSessionKwh) {
            this.chargeAddedThisSessionKwh = chargeAddedThisSessionKwh;
            return this;
        }

        public ZappiStatusBuilder gridWatts(Long gridWatts) {
            this.gridWatts = gridWatts;
            return this;
        }

        public ZappiStatusBuilder zappiChargeMode(int zappiChargeMode) {
            this.zappiChargeMode = zappiChargeMode;
            return this;
        }

        public ZappiStatusBuilder chargeStatus(int chargeStatus) {
            this.chargeStatus = chargeStatus;
            return this;
        }

        public ZappiStatusBuilder evConnectionStatus(String evConnectionStatus) {
            this.evConnectionStatus = evConnectionStatus;
            return this;
        }

        public ZappiStatus build() {
            return new ZappiStatus(serialNumber, solarGeneration, carDiversionAmountWatts, chargeAddedThisSessionKwh, gridWatts, zappiChargeMode, chargeStatus, evConnectionStatus);
        }

        public String toString() {
            return "ZappiStatus.ZappiStatusBuilder(serialNumber=" + this.serialNumber + ", solarGeneration=" + this.solarGeneration + ", carDiversionAmountWatts=" + this.carDiversionAmountWatts + ", chargeAddedThisSessionKwh=" + this.chargeAddedThisSessionKwh + ", gridWatts=" + this.gridWatts + ", zappiChargeMode=" + this.zappiChargeMode + ", chargeStatus=" + this.chargeStatus + ", evConnectionStatus=" + this.evConnectionStatus + ")";
        }
    }

//    {
//        "zappi": [
//        {
//            "sno": serialnumberhere,
//                "dat": "28-01-2023",
//                "tim": "14:13:21",
//                "ectp2": -860,
//                "ectt1": "Internal Load",
//                "ectt2": "Grid",
//                "ectt3": "None",
//                "bsm": 0,
//                "bst": 0,
//                "cmt": 254,
//                "dst": 1,
//                "div": 0,
//                "frq": 50.02,
//                "fwv": "3562S4.525",
//                "gen": 1666,
//                "grd": -868,
//                "pha": 1,
//                "pri": 1,
//                "sta": 5,
//                "tz": 0,
//                "vol": 2411,
//                "che": 24.43,
//                "bss": 0,
//                "lck": 23,
//                "pst": "B2",
//                "zmo": 3,
//                "pwm": 5300,
//                "zs": 2562,
//                "rdc": -3,
//                "rac": 1,
//                "rrac": -6,
//                "zsh": 10,
//                "zsl": 2,
//                "ectt4": "None",
//                "ectt5": "None",
//                "ectt6": "None",
//                "newAppAvailable": false,
//                "newBootloaderAvailable": false,
//                "beingTamperedWith": false,
//                "batteryDischargeEnabled": false,
//                "mgl": 30,
//                "sbh": 17,
//                "sbk": 5
//        }
//    ]
//    }
}

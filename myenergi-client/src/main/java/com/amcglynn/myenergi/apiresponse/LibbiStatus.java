package com.amcglynn.myenergi.apiresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LibbiStatus {

//    "deviceClass": "LIBBI",
//            "sno": %s,
//            "dat": "10-05-2024",
//            "tim": "15:19:32",
//            "ectp1": 1936,
//            "ectp2": 70,
//            "ectp3": 1734,
//            "ectt1": "Internal Load",
//            "ectt2": "Grid",
//            "ectt3": "Generation",
//            "ectp4": 58,
//            "ectp5": 1027,
//            "ectt4": "Monitor",
//            "ectt5": "DCPV",
//            "ectt6": "None",
//            "dst": 1,
//            "tz": 0,
//            "lmo": "BALANCE",
//            "sta": 5,
//            "frq": 50.02,
//            "pri": 2,
//            "soc": 90,
//            "isp": true,
//            "pha": 1,
//            "vol": 2396,
//            "mbc": 10200,
//            "mic": 5000,
//            "gen": 2723,
//            "grd": -76,
//            "div": 1936,
//            "ect1p": 1,
//            "ect2p": 1,
//            "ect3p": 1,
//            "batteryDischargingBoost": false,
//            "pvDirectlyConnected": true,
//            "g100LockoutState": "NONE",
//            "countryCode": "GBR",
//            "isVHubEnabled": true,
//            "cmt": 254,
//            "fwv": "3702S5.433",
//            "newAppAvailable": false,
//            "newBootloaderAvailable": false,
//            "productCode": "3702"

    @JsonProperty("sno")
    private String serialNumber;
    @Getter
    @JsonProperty("soc")
    private int stateOfCharge;  //0-100
    @Getter
    @JsonProperty("mbc")
    private int batterySizeWh;
    @Getter
    @JsonProperty("sta")
    private int status;

    public LibbiStatus(String serialNumber, int stateOfCharge, int batterySizeWh, int status) {
        this.serialNumber = serialNumber;
        this.stateOfCharge = stateOfCharge;
        this.batterySizeWh = batterySizeWh;
        this.status = status;
    }

    public LibbiStatus() {
    }

    public static LibbiStatusBuilder builder() {
        return new LibbiStatusBuilder();
    }

    public String getSerialNumber() {
        return this.serialNumber;
    }

    @JsonProperty("sno")
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @JsonProperty("soc")
    public void setStateOfCharge(int stateOfCharge) {
        this.stateOfCharge = stateOfCharge;
    }

    @JsonProperty("mbc")
    public void setBatterySizeWh(int batterySizeWh) {
        this.batterySizeWh = batterySizeWh;
    }

    @JsonProperty("sta")
    public void setStatus(int status) {
        this.status = status;
    }

    public static class LibbiStatusBuilder {
        private String serialNumber;
        private int stateOfCharge;
        private int batterySizeWh;
        private int status;

        LibbiStatusBuilder() {
        }

        public LibbiStatusBuilder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public LibbiStatusBuilder stateOfCharge(int stateOfCharge) {
            this.stateOfCharge = stateOfCharge;
            return this;
        }

        public LibbiStatusBuilder batterySizeWh(int batterySizeWh) {
            this.batterySizeWh = batterySizeWh;
            return this;
        }

        public LibbiStatusBuilder status(int status) {
            this.status = status;
            return this;
        }

        public LibbiStatus build() {
            return new LibbiStatus(serialNumber, stateOfCharge, batterySizeWh, status);
        }
    }
}

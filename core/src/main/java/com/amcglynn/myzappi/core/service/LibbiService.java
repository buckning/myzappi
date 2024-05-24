package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiOAuthClient;
import com.amcglynn.myzappi.core.model.SerialNumber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LibbiService {

    private final MyEnergiClient client;

    public LibbiService(MyEnergiClient client) {
        this.client = client;
    }

    public void setMode(SerialNumber serialNumber, LibbiMode mode) {
        log.info("Setting libbi mode for serial number {} to {}", serialNumber, mode);
        client.setLibbiMode(serialNumber.toString(), mode);
    }

    public void setChargeFromGrid(SerialNumber serialNumber, String email, String password, boolean chargeFromGrid) {
        log.info("Setting charge from grid for serial number {} to {}", serialNumber, chargeFromGrid);
        new MyEnergiOAuthClient(email, password).setChargeFromGrid(serialNumber.toString(), chargeFromGrid);
    }
}

//Got response from myenergi
//        {
//        "status": true,
//        "message": "",
//        "field": "",
//        "content": "Libbi mode flag successfully updated!"
//        }

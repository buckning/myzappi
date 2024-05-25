package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiOAuthClient;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LibbiService {

    private final MyEnergiClient client;
    private final LoginService loginService;

    public LibbiService(MyEnergiClient client, LoginService loginService) {
        this.client = client;
        this.loginService = loginService;
    }

    public void setMode(SerialNumber serialNumber, LibbiMode mode) {
        log.info("Setting libbi mode for serial number {} to {}", serialNumber, mode);
        client.setLibbiMode(serialNumber.toString(), mode);
    }

    public void setChargeFromGrid(UserId userId, SerialNumber serialNumber, boolean chargeFromGrid) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);
        creds.ifPresent(cred -> {
            log.info("Setting charge from grid for serial number {} to {}", serialNumber, chargeFromGrid);
                    new MyEnergiOAuthClient(cred.getEmailAddress(), cred.getPassword())
                            .setChargeFromGrid(serialNumber.toString(), chargeFromGrid);
            });
    }
}

//Got response from myenergi
//        {
//        "status": true,
//        "message": "",
//        "field": "",
//        "content": "Libbi mode flag successfully updated!"
//        }

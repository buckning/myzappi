package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutomationSnapshotMapperTest {

    private final AutomationSnapshotMapper mapper = new AutomationSnapshotMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapAccountEnergyZappiChargeRateAndLibbiStateOfChargeFromOneStatusResponse() throws Exception {
        var zappi = objectMapper.readValue("""
                {"sno":"10000001","gen":5000,"grd":-1500,"div":3200,"zmo":3,"mgl":75}
                """, ZappiStatus.class);
        var libbi = objectMapper.readValue("""
                {"sno":"20000002","soc":81,"gen":5000,"grd":-1500,"div":0}
                """, MyEnergiDeviceStatus.class);
        var response = new StatusResponse();
        response.setZappi(List.of(zappi));
        response.setLibbi(List.of(libbi));

        var snapshot = mapper.from(List.of(response));

        assertThat(snapshot.getEnergyStatus().getSolarGenerationKW().getDouble()).isEqualTo(5.0);
        assertThat(snapshot.getEnergyStatus().getExportingKW().getDouble()).isEqualTo(1.5);
        assertThat(snapshot.getZappiEvChargeRateKW(SerialNumber.from("10000001")).getDouble()).isEqualTo(3.2);
        assertThat(snapshot.getZappiChargeMode(SerialNumber.from("10000001"))).contains(ZappiChargeMode.ECO_PLUS);
        assertThat(snapshot.getZappiMinimumGreenLevel(SerialNumber.from("10000001"))).contains(75);
        assertThat(snapshot.getLibbiStateOfChargePercent(SerialNumber.from("20000002"))).contains(81);
    }
}

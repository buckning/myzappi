package com.amcglynn.myenergi.apiresponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyEnergiDeviceStatusTest {

    @Test
    void shouldReadLibbiStateOfChargeFromStatusResponse() throws Exception {
        var body = """
                {"sno":"12345678","soc":91,"gen":1500,"grd":-500,"div":300}
                """;

        var status = new ObjectMapper().readValue(body, MyEnergiDeviceStatus.class);

        assertThat(status.getSerialNumber()).isEqualTo("12345678");
        assertThat(status.getStateOfCharge()).isEqualTo(91);
    }
}

package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicesControllerTest {
    @Mock
    private RegistrationService mockRegistrationService;
    private DevicesController controller;

    @BeforeEach
    void setUp() {
        controller = new DevicesController(mockRegistrationService);
    }

    @Test
    void testListDevices() {
        when(mockRegistrationService.readDevices(UserId.from("userId")))
                .thenReturn(List.of(new ZappiDevice(SerialNumber.from("serial"))));
        var response = controller.handle(new Request(UserId.from("userId"), RequestMethod.GET, "/devices", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("[{\"serialNumber\":\"serial\",\"deviceClass\":\"ZAPPI\"}]"));
    }

    @Test
    void testListDevicesNoDevices() {
        when(mockRegistrationService.readDevices(UserId.from("userId")))
                .thenReturn(List.of());
        var response = controller.handle(new Request(UserId.from("userId"), RequestMethod.GET, "/devices", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("[]"));
    }

    @Test
    void testDeleteDevice() {
        var response = controller.handle(new Request(RequestMethod.DELETE, "/devices", "userId"));
        assertThat(response.getStatus()).isEqualTo(204);
    }
}

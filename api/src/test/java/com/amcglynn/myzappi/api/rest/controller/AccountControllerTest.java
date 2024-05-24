package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock
    private RegistrationService mockRegistrationService;
    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(mockRegistrationService);
    }

    @Test
    void register() {
        var body = """
                {"email":"user@test.com","password":"password"}
                """;
        var response = controller.register(new Request(RequestMethod.POST, "/account/register", body));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"email\":\"user@test.com\"}"));
    }
}

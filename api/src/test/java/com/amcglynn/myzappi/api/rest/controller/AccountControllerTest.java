package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.AccountSummaryResponse;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

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
    void getAccountSummary() {
        when(mockRegistrationService.getAccountSummary(UserId.from("userId")))
                .thenReturn(new AccountSummaryResponse(true, true));
        var request = new Request(RequestMethod.GET, "/account/summary", null);
        request.setUserId("userId");
        var response = controller.getAccountSummary(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("""
                    {"hubRegistered":true,"myaccountRegistered":true}\
                    """));
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

    @Test
    void registerReturns400WhenRequestBodyIsNull() {
        var request = new Request(RequestMethod.POST, "/account/register", null);
        var exception = catchThrowableOfType(() -> controller.register(request),
                ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }

    @Test
    void registerReturns400WhenRequestBodyIsMalformed() {
        var body = """
                {"email":"user@test.com","password":"pas
                """;
        var request = new Request(RequestMethod.POST, "/account/register", body);
        var exception = catchThrowableOfType(() -> controller.register(request),
                ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }
}

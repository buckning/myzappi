package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.exception.MyEnergiCredentialsNotConfiguredException;
import com.amcglynn.myzappi.core.model.LibbiStatus;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.LibbiService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.amcglynn.myzappi.TestObjectCreator.handlerInputBuilder;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetLibbiChargeTargetHandlerTest {

    @Mock
    private MyEnergiService.Builder mockMyEnergiServiceBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private LibbiService mockLibbiService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private UserIdResolver mockUserIdResolver;
    private GetLibbiChargeTargetHandler handler;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getLibbiServiceOrThrow()).thenReturn(mockLibbiService);
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResolver);
        handler = new GetLibbiChargeTargetHandler(mockMyEnergiServiceBuilder, mockUserIdResolverFactory);
        when(mockUserIdResolver.getUserId()).thenReturn("mockUserId");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder("GetLibbiChargeTarget").build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleThrowsMissingDeviceExceptionWhenLibbiDeviceNotFoundForUser() {
        when(mockMyEnergiService.getLibbiServiceOrThrow()).thenThrow(MissingDeviceException.class);
        var throwable = catchThrowable(() -> handler.handle(handlerInputBuilder("GetLibbiChargeTarget").build()));
        assertThat(throwable).isNotNull().isInstanceOf(MissingDeviceException.class);
    }

    @Test
    void testHandleGetLibbiChargeTarget() {
        when(mockLibbiService.getStatus(any())).thenReturn(LibbiStatus.builder()
                .state(LibbiState.ON)
                        .batterySizeKWh(new KiloWattHour(10.2))
                        .energyTargetKWh(new KiloWattHour(5.1))
                .build());
        var response = handler.handle(handlerInputBuilder("GetLibbiChargeTarget").build());
        assertThat(response).isPresent();
        verifySpeechInResponse(response.get(), "<speak>Battery charge target is 50%.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "Charge Target: 50%");
    }

    @Test
    void testHandleGetLibbiChargeTargetThrowsMyEnergiCredentialsNotConfiguredException() {
        when(mockLibbiService.getStatus(any())).thenReturn(LibbiStatus.builder().state(LibbiState.ON).build());
        doThrow(new MyEnergiCredentialsNotConfiguredException("mockException"))
                .when(mockLibbiService).validateMyEnergiAccountIsConfigured(UserId.from("mockUserId"));
        var exception = catchThrowableOfType(() -> handler.handle(handlerInputBuilder("GetLibbiChargeTarget").build()),
                MyEnergiCredentialsNotConfiguredException.class);
        assertThat(exception).isNotNull();
    }
}

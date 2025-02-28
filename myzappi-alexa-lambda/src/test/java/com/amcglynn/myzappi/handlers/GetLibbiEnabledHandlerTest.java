package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.LibbiStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetLibbiEnabledHandlerTest {

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
    private GetLibbiEnabledHandler handler;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getLibbiServiceOrThrow()).thenReturn(mockLibbiService);
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResolver);
        handler = new GetLibbiEnabledHandler(mockMyEnergiServiceBuilder, mockUserIdResolverFactory);
        when(mockUserIdResolver.getUserId()).thenReturn("mockUserId");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder("GetLibbiEnabled").build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleThrowsMissingDeviceExceptionWhenLibbiDeviceNotFoundForUser() {
        when(mockMyEnergiService.getLibbiServiceOrThrow()).thenThrow(MissingDeviceException.class);
        var throwable = catchThrowable(() -> handler.handle(handlerInputBuilder("GetLibbiEnabled").build()));
        assertThat(throwable).isNotNull().isInstanceOf(MissingDeviceException.class);
    }

    @Test
    void testHandleLibbiEnabled() {
        when(mockLibbiService.getStatus(any())).thenReturn(LibbiStatus.builder().state(LibbiState.ON).build());
        var response = handler.handle(handlerInputBuilder("GetLibbiEnabled").build());
        assertThat(response).isPresent();
        verifySpeechInResponse(response.get(), "<speak>Your battery is On.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "State: On");
    }

    @Test
    void testHandleLibbiDisbled() {
        when(mockLibbiService.getStatus(any())).thenReturn(LibbiStatus.builder().state(LibbiState.OFF).build());
        var response = handler.handle(handlerInputBuilder("GetLibbiEnabled").build());
        assertThat(response).isPresent();
        verifySpeechInResponse(response.get(), "<speak>Your battery is Off.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "State: Off");
    }
}

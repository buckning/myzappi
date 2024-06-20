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
class SetLibbiDisabledHandlerTest {
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
    private SetLibbiDisabledHandler handler;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getLibbiServiceOrThrow()).thenReturn(mockLibbiService);
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResolver);
        handler = new SetLibbiDisabledHandler(mockMyEnergiServiceBuilder, mockUserIdResolverFactory);
        when(mockUserIdResolver.getUserId()).thenReturn("mockUserId");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder("SetLibbiDisabled").build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleThrowsMissingDeviceExceptionWhenLibbiDeviceNotFoundForUser() {
        when(mockMyEnergiService.getLibbiServiceOrThrow()).thenThrow(MissingDeviceException.class);
        var throwable = catchThrowable(() -> handler.handle(handlerInputBuilder("SetLibbiDisabled").build()));
        assertThat(throwable).isNotNull().isInstanceOf(MissingDeviceException.class);
    }

    @Test
    void testHandleSetLibbiEnabled() {
        when(mockLibbiService.getStatus(any())).thenReturn(LibbiStatus.builder().state(LibbiState.ON).build());
        var response = handler.handle(handlerInputBuilder("SetLibbiDisabled").build());
        assertThat(response).isPresent();
        verifySpeechInResponse(response.get(), "<speak>Disabling your battery, this may take a few minutes.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "Disabling your battery, this may take a few minutes.");
    }
}

package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Application;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdResolverFactoryTest {

    @Mock
    private LwaClient mockLwaClient;

    @Test
    void returnsLwaUserIdResolverWhenAccessTokenIsPresent() {
        var requestEnvelopeBuilder = RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withSession(Session.builder().withUser(User.builder()
                                .withUserId("userId")
                                .withAccessToken("mockAccessToken").build())
                        .build());

        when(mockLwaClient.getUserId("mockAccessToken"))
                .thenReturn(Optional.of("userId"));
        var userIdResolver = new UserIdResolverFactory(mockLwaClient).newUserIdResolver(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder.build()).build());
        assertThat(userIdResolver)
                .isInstanceOf(LwaUserIdResolver.class);
        assertThat(userIdResolver.getUserId()).isEqualTo("userId");
    }

    @Test
    void throwsUserNotLinkedExceptionWhenAccessTokenIsProvidedButItDoesNotLogin() {
        var requestEnvelopeBuilder = RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withSession(Session.builder().withUser(User.builder()
                                .withUserId("test")
                                .withAccessToken("mockAccessToken").build())
                        .withApplication(Application.builder()
                                .withApplicationId("appId")
                                .build())
                        .build());

        when(mockLwaClient.getUserId("mockAccessToken"))
                .thenReturn(Optional.empty());
        var throwable = catchThrowable(() -> new UserIdResolverFactory(mockLwaClient).newUserIdResolver(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder.build()).build()));

        assertThat(throwable).isInstanceOf(UserNotLinkedException.class);
    }

    @Test
    void returnsThrowsUserNotLinkedExceptionsWhenAccessTokenIsNotPresent() {
        var throwable = catchThrowable(() -> new UserIdResolverFactory(mockLwaClient).newUserIdResolver(handlerInputBuilder().build()));
        assertThat(throwable).isNotNull().isInstanceOf(UserNotLinkedException.class);
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build())
                        .withApplication(Application.builder().withApplicationId("appId").build()).build());
    }

    private IntentRequest initIntentRequest() {
        return IntentRequest.builder()
                .withIntent(Intent.builder().build())
                .build();
    }
}

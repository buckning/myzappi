package com.amcglynn.myzappi.api.service;

import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.SessionRepository;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    @Mock
    private SessionRepository mockSessionRespository;

    private Session session;
    private final SessionId sessionId = SessionId.from("03662064-99b5-404c-b4c7-a0bd04257f95");

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        session = new Session(sessionId, UserId.from("userId"), 3600);

        when(mockSessionRespository.read(sessionId)).thenReturn(Optional.of(session));
        sessionService = new SessionService(mockSessionRespository);
        sessionService.setInstantSupplier(() -> Instant.ofEpochSecond(0L));
    }

    @Test
    void getValidSessionReturnsSessionWhenItIsInTheDb() {
        when(mockSessionRespository.read(sessionId)).thenReturn(Optional.of(session));
        var result = sessionService.getValidSession(sessionId);
        assertThat(result).isEqualTo(Optional.of(session));
    }

    @Test
    void getValidSessionReturnsEmptyOptionalWhenNoSessionExistsInDb() {
        when(mockSessionRespository.read(SessionId.from("invalidSessionId"))).thenReturn(Optional.empty());
        var result = sessionService.getValidSession(SessionId.from("invalidSessionId"));
        assertThat(result).isNotPresent();
    }

    @Test
    void getValidSessionReturnsEmptyOptionalWhenSessionHasExpired() {
        sessionService.setInstantSupplier(() -> Instant.ofEpochSecond(7201L));
        var result = sessionService.getValidSession(sessionId);
        assertThat(result).isNotPresent();
    }

    @Test
    void createSessionCreatesSessionInDb() {
        var sessionCaptor = ArgumentCaptor.forClass(Session.class);
        sessionService.createSession(UserId.from("userId"));
        verify(mockSessionRespository).write(sessionCaptor.capture());
        var session = sessionCaptor.getValue();
        assertThat(session.getSessionId()).isNotNull();
        UUID.fromString(session.getSessionId().toString()); // throws exception if not valid UUID
        assertThat(session.getUserId()).isEqualTo(UserId.from("userId"));
        assertThat(session.getTtl()).isEqualTo(3600);
    }

    @Test
    void invalidateSessionDeletesSessionFromRepository() {
        sessionService.invalidateSession(session);
        verify(mockSessionRespository).delete(session);
    }
}

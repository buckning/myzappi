package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
import com.amcglynn.myzappi.core.model.CompleteLoginState;
import com.amcglynn.myzappi.core.model.LoginCodeEntry;
import com.amcglynn.myzappi.core.model.LoginState;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private CredentialsRepository mockCredentialsRepository;
    @Mock
    private LoginCodeRepository mockLoginCodeRepository;
    @Mock
    private EncryptionService mockEncryptionService;

    @Captor
    private ArgumentCaptor<ZappiCredentials> zappiCredentialsCaptor;

    private LoginService loginService;
    private final ByteBuffer encryptedApiKey = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03});
    private final SerialNumber serialNumber = SerialNumber.from("12345678");
    private final LoginCode loginCode = LoginCode.from("abc123");
    private final String userId = "userid";
    private final Instant createdTime = Instant.now();
    private LoginCodeEntry loginCodeEntry;
    private ZappiCredentials zappiCredentials;

    @BeforeEach
    void setUp() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode, encryptedApiKey);
        loginCodeEntry = new LoginCodeEntry(loginCode, userId, createdTime);
        loginService = new LoginService(mockCredentialsRepository, mockLoginCodeRepository, mockEncryptionService);
    }

    @Test
    void testLoginReturnsExistingLoginDetailsWhenAlreadyLoggedIn() {
        when(mockCredentialsRepository.read("userid")).thenReturn(Optional.of(zappiCredentials));
        var response = loginService.login(userId, serialNumber);
        var creds = response.getCreds();
        assertThat(response.getLoginState()).isEqualTo(LoginState.LOGIN_COMPLETE);
        assertThat(creds.getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(creds.getCode()).isEqualTo(LoginCode.from("abc123"));
        assertThat(creds.getUserId()).isEqualTo("userid");
        assertThat(creds.getEncryptedApiKey()).isPresent();
        assertThat(creds.getEncryptedApiKey()).contains(encryptedApiKey);
        verify(mockCredentialsRepository, never()).write(any());
        verify(mockLoginCodeRepository, never()).write(any());
    }

    @Test
    void testLoginReturnsExistingLoginDetailsWhenLoginCodeIsAlreadyGeneratedButNotConsumed() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode);
        when(mockLoginCodeRepository.read(loginCode)).thenReturn(Optional.of(loginCodeEntry));
        when(mockCredentialsRepository.read("userid")).thenReturn(Optional.of(zappiCredentials));
        var response = loginService.login(userId, serialNumber);
        var creds = response.getCreds();
        assertThat(response.getLoginState()).isEqualTo(LoginState.EXISTING_LOGIN_CODE);
        assertThat(creds.getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(creds.getCode()).isEqualTo(LoginCode.from("abc123"));
        assertThat(creds.getUserId()).isEqualTo("userid");
        assertThat(creds.getEncryptedApiKey()).isEmpty();
        verify(mockCredentialsRepository, never()).write(any());
        verify(mockLoginCodeRepository, never()).write(any());
    }

    @Test
    void testLoginGeneratesNewLoginCodeWhenItHasExpired() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode);
        loginCodeEntry = new LoginCodeEntry(loginCode, userId, Instant.now().minus(10, ChronoUnit.DAYS));
        when(mockCredentialsRepository.read("userid")).thenReturn(Optional.of(zappiCredentials));
        when(mockLoginCodeRepository.read(loginCode)).thenReturn(Optional.of(loginCodeEntry));

        var response = loginService.login(userId, serialNumber);
        var creds = response.getCreds();

        assertThat(response.getLoginState()).isEqualTo(LoginState.RECREATED_NEW_CODE);
        assertThat(creds.getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(creds.getCode()).isNotEqualTo(LoginCode.from("abc123")); // there should be a new code generated
        assertThat(creds.getUserId()).isEqualTo("userid");
        assertThat(creds.getEncryptedApiKey()).isEmpty();

        verify(mockCredentialsRepository).write(any());
        verify(mockLoginCodeRepository).write(any());
    }

    @Test
    void testLoginGeneratesNewLoginCodeWhenItExistsInCredentialsTableButNotInCodeTable() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode);
        when(mockCredentialsRepository.read("userid")).thenReturn(Optional.of(zappiCredentials));
        when(mockLoginCodeRepository.read(loginCode)).thenReturn(Optional.empty());

        var response = loginService.login(userId, serialNumber);
        var creds = response.getCreds();

        assertThat(response.getLoginState()).isEqualTo(LoginState.RECREATED_NEW_CODE);
        assertThat(creds.getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(creds.getCode()).isNotEqualTo(LoginCode.from("abc123"));
        assertThat(creds.getUserId()).isEqualTo("userid");
        assertThat(creds.getEncryptedApiKey()).isEmpty();

        verify(mockCredentialsRepository).write(any());
        verify(mockLoginCodeRepository).write(any());
    }

    @Test
    void testLoginCreatesAndSavesLoginCredsWhenNoneExist() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode);
        var expectedZappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode);
        when(mockCredentialsRepository.read("userid")).thenReturn(Optional.empty());
        var response = loginService.login(userId, serialNumber);
        var creds = response.getCreds();
        assertThat(response.getLoginState()).isEqualTo(LoginState.NEW);
        assertThat(creds.getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(creds.getCode()).isNotNull();
        assertThat(creds.getUserId()).isEqualTo("userid");
        assertThat(creds.getEncryptedApiKey()).isEmpty();
        verify(mockCredentialsRepository).write(zappiCredentialsCaptor.capture());
        verify(mockLoginCodeRepository).write(zappiCredentialsCaptor.capture());
        assertThat(zappiCredentialsCaptor.getAllValues()).hasSize(2);
        assertEquals(zappiCredentialsCaptor.getAllValues().get(0), expectedZappiCredentials);
        assertEquals(zappiCredentialsCaptor.getAllValues().get(1), expectedZappiCredentials);
    }

    @Test
    void testCompleteLoginReturnsLoginCodeNotFoundWhenItIsNotInTheLoginCodeTable() {
        when(mockEncryptionService.encrypt("myApiKey")).thenReturn(encryptedApiKey);
        var response = loginService.completeLogin(loginCode, "myApiKey");
        assertThat(response.getState()).isEqualTo(CompleteLoginState.LOGIN_CODE_NOT_FOUND);
        assertThat(response.getZappiCredentials()).isEmpty();
        verify(mockLoginCodeRepository).delete(loginCode);
    }

    @Test
    void testCompleteLoginReturnsLoginCodeExpiredWhenItIsOlderThan1DayAndDeletesLoginCode() {
        when(mockEncryptionService.encrypt("myApiKey")).thenReturn(encryptedApiKey);
        loginCodeEntry = new LoginCodeEntry(loginCode, userId, Instant.now().minus(2, ChronoUnit.DAYS));
        when(mockLoginCodeRepository.read(loginCode)).thenReturn(Optional.of(loginCodeEntry));
        var response = loginService.completeLogin(loginCode, "myApiKey");
        assertThat(response.getState()).isEqualTo(CompleteLoginState.LOGIN_CODE_EXPIRED);
        assertThat(response.getZappiCredentials()).isEmpty();
        verify(mockLoginCodeRepository).delete(loginCode);
    }

    @Test
    void testCompleteLoginReturnsCompleteWhenLoginCodeIsValidAndAssociatedUserIsFound() {
        when(mockEncryptionService.encrypt("myApiKey")).thenReturn(encryptedApiKey);
        loginCodeEntry = new LoginCodeEntry(loginCode, userId, Instant.now());
        when(mockLoginCodeRepository.read(loginCode)).thenReturn(Optional.of(loginCodeEntry));
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.of(zappiCredentials));

        var response = loginService.completeLogin(loginCode, "myApiKey");

        assertThat(response.getState()).isEqualTo(CompleteLoginState.COMPLETE);
        verify(mockCredentialsRepository).delete(userId);
        verify(mockCredentialsRepository).write(zappiCredentialsCaptor.capture());

        assertThat(zappiCredentialsCaptor.getAllValues()).hasSize(1);
        var newZappiCreds = zappiCredentialsCaptor.getValue();
        assertThat(newZappiCreds.getUserId()).isEqualTo(userId);
        assertThat(newZappiCreds.getCode()).isEqualTo(loginCode);
        assertThat(newZappiCreds.getSerialNumber()).isEqualTo(serialNumber);
        assertThat(newZappiCreds.getEncryptedApiKey()).contains(encryptedApiKey);
        verify(mockLoginCodeRepository).delete(loginCode);
    }

    @Test
    void testCompleteLoginReturnsAssociatedUserNotFoundWhenLoginCodeIsInTheDbButThereIsNoUserInTheCredentialsTable() {
        when(mockEncryptionService.encrypt("myApiKey")).thenReturn(encryptedApiKey);
        when(mockLoginCodeRepository.read(loginCode)).thenReturn(Optional.of(loginCodeEntry));
        var response = loginService.completeLogin(loginCode, "myApiKey");
        assertThat(response.getState()).isEqualTo(CompleteLoginState.ASSOCIATED_USER_NOT_FOUND);
        assertThat(response.getZappiCredentials()).isEmpty();
    }

    @Test
    void testLogoutWhenDataFoundInDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.of(zappiCredentials));
        loginService.logout(userId);
        verify(mockCredentialsRepository).delete(userId);
        verify(mockLoginCodeRepository).delete(loginCode);
    }

    @Test
    void testLogoutWhenDataNotFoundInDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.empty());
        loginService.logout(userId);
        verify(mockCredentialsRepository, never()).delete(userId);
        verify(mockLoginCodeRepository, never()).delete(loginCode);
    }

    private void assertEquals(ZappiCredentials expected, ZappiCredentials actual) {
        assertThat(actual.getSerialNumber()).isEqualTo(expected.getSerialNumber());
        assertThat(actual.getCode()).isNotNull();
        assertThat(actual.getUserId()).isEqualTo(expected.getUserId());
        assertThat(actual.getEncryptedApiKey()).isEqualTo(expected.getEncryptedApiKey());
    }
}

package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
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

    @Captor
    private ArgumentCaptor<ZappiCredentials> zappiCredentialsCaptor;

    private LoginService loginService;
    private final ByteBuffer encryptedApiKey = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 });
    private final SerialNumber serialNumber = SerialNumber.from("12345678");
    private final LoginCode loginCode = LoginCode.from("abc123");
    private final String userId = "userid";
    private ZappiCredentials zappiCredentials;

    @BeforeEach
    void setUp() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode, encryptedApiKey);
        loginService = new LoginService(mockCredentialsRepository, mockLoginCodeRepository);
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
        when(mockCredentialsRepository.read("userid")).thenReturn(Optional.of(zappiCredentials));
        var response = loginService.login(userId, serialNumber);
        var creds = response.getCreds();
        assertThat(response.getLoginState()).isEqualTo(LoginState.GENERATED_LOGIN_CODE);
        assertThat(creds.getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(creds.getCode()).isEqualTo(LoginCode.from("abc123"));
        assertThat(creds.getUserId()).isEqualTo("userid");
        assertThat(creds.getEncryptedApiKey()).isEmpty();
        verify(mockCredentialsRepository, never()).write(any());
        verify(mockLoginCodeRepository, never()).write(any());
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

    private void assertEquals(ZappiCredentials expected, ZappiCredentials actual) {
        assertThat(actual.getSerialNumber()).isEqualTo(expected.getSerialNumber());
        assertThat(actual.getCode()).isNotNull();
        assertThat(actual.getUserId()).isEqualTo(expected.getUserId());
        assertThat(actual.getEncryptedApiKey()).isEqualTo(expected.getEncryptedApiKey());
    }
}

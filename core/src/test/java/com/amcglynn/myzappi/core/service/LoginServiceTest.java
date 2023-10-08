package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.MyEnergiDeployment;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private CredentialsRepository mockCredentialsRepository;
    @Mock
    private EncryptionService mockEncryptionService;

    private LoginService loginService;
    private final ByteBuffer encryptedApiKey = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03});
    private final SerialNumber zappiSerialNumber = SerialNumber.from("56781234");
    private final SerialNumber serialNumber = SerialNumber.from("12345678");
    private final String userId = "userid";
    private MyEnergiDeployment zappiCredentials;
    @Captor
    private ArgumentCaptor<MyEnergiDeployment> credsCaptor;

    @BeforeEach
    void setUp() {
        zappiCredentials = new MyEnergiDeployment(userId, zappiSerialNumber, serialNumber, encryptedApiKey);
        loginService = new LoginService(mockCredentialsRepository, mockEncryptionService);
    }

    @Test
    void testReadCredentialsReturnsValueFromDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.of(zappiCredentials));
        assertThat(loginService.readCredentials(userId)).isEqualTo(Optional.of(zappiCredentials));
    }

    @Test
    void testLogoutWhenDataFoundInDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.of(zappiCredentials));
        loginService.logout(userId);
        verify(mockCredentialsRepository).delete(userId);
    }

    @Test
    void testLogoutWhenDataNotFoundInDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.empty());
        loginService.logout(userId);
        verify(mockCredentialsRepository, never()).delete(userId);
    }

    @Test
    void testRegisterSavesDetailsToDb() {
        when(mockEncryptionService.encrypt(anyString())).thenReturn(encryptedApiKey);

        loginService.register(userId, zappiSerialNumber, serialNumber, null, "apiKey");

        verify(mockEncryptionService).encrypt("apiKey");
        verify(mockCredentialsRepository).write(credsCaptor.capture());

        var credsInDb = credsCaptor.getValue();
        assertThat(credsInDb).isNotNull();
        assertThat(credsInDb.getUserId()).isEqualTo(userId);
        assertThat(credsInDb.getSerialNumber()).isEqualTo(serialNumber);
        assertThat(credsInDb.getEncryptedApiKey()).isEqualTo(encryptedApiKey);
    }

    @Test
    void testDelete() {
        loginService.delete(userId);
        verify(mockCredentialsRepository).delete(userId);
    }
}

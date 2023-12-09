package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.HubCredentials;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.MyEnergiDeployment;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class LoginServiceTest {

    @Mock
    private CredentialsRepository mockCredentialsRepository;
    @Mock
    private DevicesRepository mockDevicesRepository;
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
        zappiCredentials = new MyEnergiDeployment(userId, serialNumber, encryptedApiKey);
        loginService = new LoginService(mockCredentialsRepository, mockDevicesRepository, mockEncryptionService);
        when(mockEncryptionService.decrypt(encryptedApiKey)).thenReturn("decryptedKey");
    }

    @Test
    void testReadCredentialsReturnsValueFromDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.of(zappiCredentials));
        var creds = loginService.readCredentials(UserId.from(userId));
        assertThat(creds).isPresent();
        assertThat(creds.get().getApiKey()).isEqualTo("decryptedKey");
        assertThat(creds.get().getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
    }

    @Test
    void testReadCredentialsReturnsEmptyOptionalWhenNotFoundInDb() {
        when(mockCredentialsRepository.read(userId)).thenReturn(Optional.empty());
        var creds = loginService.readCredentials(UserId.from(userId));
        assertThat(creds).isEmpty();
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
    void testRegisterWithEddiSavesDetailsToDb() {
        when(mockEncryptionService.encrypt(anyString())).thenReturn(encryptedApiKey);

        loginService.register(userId, zappiSerialNumber, serialNumber, new EddiDevice(serialNumber, "t1", "t2"), "apiKey");

        verify(mockEncryptionService).encrypt("apiKey");
        verify(mockCredentialsRepository).write(credsCaptor.capture());

        var credsInDb = credsCaptor.getValue();
        assertThat(credsInDb).isNotNull();
        assertThat(credsInDb.getUserId()).isEqualTo(userId);
        assertThat(credsInDb.getSerialNumber()).isEqualTo(serialNumber);
        assertThat(credsInDb.getEncryptedApiKey()).isEqualTo(encryptedApiKey);
    }

    @Test
    void testRefreshDeploymentDetailsWithNoEddi() {
        loginService.refreshDeploymentDetails(UserId.from(userId), zappiSerialNumber, null);
        verify(mockDevicesRepository).write(UserId.from(userId), List.of(new ZappiDevice(zappiSerialNumber)));
    }

    @Test
    void testRefreshDeploymentDetailsWithEddi() {
        loginService.refreshDeploymentDetails(UserId.from(userId), zappiSerialNumber, new EddiDevice(zappiSerialNumber, "t1", "t2"));
        verify(mockDevicesRepository).write(UserId.from(userId), List.of(new ZappiDevice(zappiSerialNumber),
                new EddiDevice(zappiSerialNumber, "t1", "t2")));
    }

    @Test
    void testDelete() {
        loginService.delete(userId);
        verify(mockCredentialsRepository).delete(userId);
    }
}

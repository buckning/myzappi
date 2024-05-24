package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.dal.MyEnergiAccountCredentialsRepository;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.EmailAddress;
import com.amcglynn.myzappi.core.model.HubCredentials;
import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentials;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
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
import java.util.ArrayList;
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
    private MyEnergiAccountCredentialsRepository mockMyEnergiAccountCredentialsRepository;
    @Mock
    private EncryptionService mockEncryptionService;

    private LoginService loginService;
    private final ByteBuffer encryptedApiKey = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03});
    private final ByteBuffer encryptedPassword = ByteBuffer.wrap(new byte[]{0x03, 0x04, 0x05});
    private final ByteBuffer encryptedEmailAddress = ByteBuffer.wrap(new byte[]{0x06, 0x07, 0x08});
    private final SerialNumber zappiSerialNumber = SerialNumber.from("56781234");
    private final SerialNumber serialNumber = SerialNumber.from("12345678");
    private final String userId = "userid";
    private MyEnergiDeployment zappiCredentials;
    @Captor
    private ArgumentCaptor<MyEnergiDeployment> credsCaptor;
    @Captor
    private ArgumentCaptor<MyEnergiAccountCredentials> myenergiCredsCaptor;

    @BeforeEach
    void setUp() {
        zappiCredentials = new MyEnergiDeployment(userId, serialNumber, encryptedApiKey);
        loginService = new LoginService(mockCredentialsRepository, mockDevicesRepository,
                mockMyEnergiAccountCredentialsRepository, mockEncryptionService);
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
    void testRegisterMyEnergiCredentials() {
        when(mockEncryptionService.encrypt("password")).thenReturn(encryptedPassword);
        when(mockEncryptionService.encrypt("user@test.com")).thenReturn(encryptedEmailAddress);

        loginService.register(userId, EmailAddress.from("user@test.com"), "password");

        verify(mockMyEnergiAccountCredentialsRepository).write(myenergiCredsCaptor.capture());
        var credsInDb = myenergiCredsCaptor.getValue();

        assertThat(credsInDb.getUserId()).isEqualTo(userId);
        assertThat(credsInDb.getEncryptedEmailAddress()).isEqualTo(encryptedEmailAddress);
        assertThat(credsInDb.getEncryptedPassword()).isEqualTo(encryptedPassword);
    }

    @Test
    void testRegisterSavesDetailsToDb() {
        when(mockEncryptionService.encrypt(anyString())).thenReturn(encryptedApiKey);

        var devices = new ArrayList<MyEnergiDevice>();
        devices.add(new ZappiDevice(zappiSerialNumber));
        loginService.register(userId, serialNumber, "apiKey", devices);

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
        var devices = new ArrayList<MyEnergiDevice>();
        devices.add(new EddiDevice(serialNumber, "t1", "t2"));
        devices.add(new ZappiDevice(zappiSerialNumber));
        loginService.register(userId, serialNumber, "apiKey", devices);

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
        var devices = new ArrayList<MyEnergiDevice>();
        devices.add(new ZappiDevice(zappiSerialNumber));
        loginService.refreshDeploymentDetails(UserId.from(userId), devices);
        verify(mockDevicesRepository).write(UserId.from(userId), devices);
    }

    @Test
    void testRefreshDeploymentDetailsWithEddi() {
        var devices = new ArrayList<MyEnergiDevice>();
        devices.add(new EddiDevice(zappiSerialNumber, "t1", "t2"));
        devices.add(new ZappiDevice(zappiSerialNumber));
        loginService.refreshDeploymentDetails(UserId.from(userId), devices);
        verify(mockDevicesRepository).write(UserId.from(userId), devices);
    }

    @Test
    void testDelete() {
        loginService.delete(userId);
        verify(mockCredentialsRepository).delete(userId);
    }
}

package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicesRepositoryTest {
    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private DevicesRepository repository;

    private final String testDevicesString = "[\n" +
            "        {\n" +
            "            \"serialNumber\": \"1234567890\",\n" +
            "            \"deviceClass\": \"ZAPPI\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"serialNumber\": \"09876543\",\n" +
            "            \"deviceClass\": \"EDDI\",\n" +
            "            \"tank1Name\": \"tank1\",\n" +
            "            \"tank2Name\": \"tank2\"\n" +
            "        }\n" +
            "    ]";

    private final String testZappiOnlyString = "[\n" +
            "        {\n" +
            "            \"serialNumber\": \"1234567890\",\n" +
            "            \"deviceClass\": \"ZAPPI\"\n" +
            "        }\n" +
            "    ]";

    private final String testEddiOnlyString = "[\n" +
            "        {\n" +
            "            \"serialNumber\": \"09876543\",\n" +
            "            \"deviceClass\": \"EDDI\",\n" +
            "            \"tank1Name\": \"tank1\",\n" +
            "            \"tank2Name\": \"tank2\"\n" +
            "        }\n" +
            "    ]";

    private final String testZappiEddiAndLibbiString = """
            [
                {
                    "serialNumber": "20000001",
                    "deviceClass": "EDDI",
                    "tank1Name": "Tank 1",
                    "tank2Name": "Tank 2"
                },
                {
                    "serialNumber": "30000001",
                    "deviceClass": "LIBBI"
                },
                {
                    "serialNumber": "10000001",
                    "deviceClass": "ZAPPI"
                }
            ]
            """;

    @BeforeEach
    void setUp() {
        repository = new DevicesRepository(mockDb);
    }

    @Test
    void testReadForUserWhoDoesNotExistReturnsEmptyList() {
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("unknownuserid"));
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEmptyList() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "devices", new AttributeValue("[]")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "devices", new AttributeValue(testDevicesString)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isInstanceOf(ZappiDevice.class);
        assertThat(result.get(0).getDeviceClass()).isEqualTo(DeviceClass.ZAPPI);
        assertThat(result.get(0).getSerialNumber()).isEqualTo(SerialNumber.from("1234567890"));
        assertThat(result.get(1)).isInstanceOf(EddiDevice.class);
        assertThat(result.get(1).getDeviceClass()).isEqualTo(DeviceClass.EDDI);
        assertThat(result.get(1).getSerialNumber()).isEqualTo(SerialNumber.from("09876543"));
        var eddi = (EddiDevice) result.get(1);
        assertThat(eddi.getTank1Name()).isEqualTo("tank1");
        assertThat(eddi.getTank2Name()).isEqualTo("tank2");
    }

    @Test
    void testReadForUserWhoHasZappiEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "devices", new AttributeValue(testZappiOnlyString)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ZappiDevice.class);
        assertThat(result.get(0).getSerialNumber()).isEqualTo(SerialNumber.from("1234567890"));
    }

    @Test
    void testReadForUserWhoHasEddiEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "devices", new AttributeValue(testEddiOnlyString)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(EddiDevice.class);
        assertThat(result.get(0).getSerialNumber()).isEqualTo(SerialNumber.from("09876543"));
    }

    @Test
    void testReadForUserWhoHasZappiEddiAndLibbiEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "devices", new AttributeValue(testZappiEddiAndLibbiString)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isInstanceOf(EddiDevice.class);
        assertThat(result.get(0).getSerialNumber()).isEqualTo(SerialNumber.from("20000001"));
        assertThat(result.get(1)).isInstanceOf(LibbiDevice.class);
        assertThat(result.get(1).getSerialNumber()).isEqualTo(SerialNumber.from("30000001"));
        assertThat(result.get(2)).isInstanceOf(ZappiDevice.class);
        assertThat(result.get(2).getSerialNumber()).isEqualTo(SerialNumber.from("10000001"));
    }

    @Test
    void testReadUnknownDeviceInDb() {
        var unknownStr = "[\n" +
                "        {\n" +
                "            \"serialNumber\": \"1234567890\",\n" +
                "            \"deviceClass\": \"UNKNOWN\"\n" +
                "        }\n" +
                "    ]";
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "devices", new AttributeValue(unknownStr)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var throwable = catchThrowableOfType(() -> repository.read(UserId.from("userid")), InvalidTypeIdException.class);
        assertThat(throwable).isNotNull();
    }

    @Test
    void testWrite() {
        repository.write(UserId.from("testUser"), List.of(new ZappiDevice(SerialNumber.from("1234567890")), new EddiDevice(SerialNumber.from("09876543"), "tank1", "tank2")));
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(2);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("devices");
        assertThat(putItemCaptor.getValue().getItem().get("user-id").getS()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().getItem().get("devices").getS())
                .isEqualTo(testDevicesString.replaceAll("\\s", ""));
    }

    @Test
    void testWriteZappiOnly() {
        repository.write(UserId.from("testUser"), List.of(new ZappiDevice(SerialNumber.from("1234567890"))));
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(2);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("devices");
        assertThat(putItemCaptor.getValue().getItem().get("user-id").getS()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().getItem().get("devices").getS())
                .isEqualTo(testZappiOnlyString.replaceAll("\\s", ""));
    }

    @Test
    void testWriteEddiOnly() {
        repository.write(UserId.from("testUser"), List.of(new EddiDevice(SerialNumber.from("09876543"), "tank1", "tank2")));
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(2);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("devices");
        assertThat(putItemCaptor.getValue().getItem().get("user-id").getS()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().getItem().get("devices").getS())
                .isEqualTo(testEddiOnlyString.replaceAll("\\s", ""));
    }

    @Test
    void testDelete() {
        repository.delete(UserId.from("userid"));
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("devices");
        assertThat(deleteItemCaptor.getValue().getKey().get("user-id").getS()).isEqualTo("userid");
    }
}

package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffRepositoryTest {
    @Mock
    private DynamoDbClient mockDb;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;

    private TariffRepository repository;

    private final String testTariffString = "[\n" +
            "        {\n" +
            "            \"start\": \"00:00\",\n" +
            "            \"end\": \"08:00\",\n" +
            "            \"name\": \"Tariff1\",\n" +
            "            \"importCostPerKwh\": 0.2092,\n" +
            "            \"exportCostPerKwh\": 0.21\n" +
            "        },\n" +
            "        {\n" +
            "            \"start\": \"08:00\",\n" +
            "            \"end\": \"17:00\",\n" +
            "            \"name\": \"Tariff2\",\n" +
            "            \"importCostPerKwh\": 0.4241,\n" +
            "            \"exportCostPerKwh\": 0.10\n" +
            "        },\n" +
            "        {\n" +
            "            \"start\": \"17:00\",\n" +
            "            \"end\": \"19:00\",\n" +
            "            \"name\": \"Tariff3\",\n" +
            "            \"importCostPerKwh\": 0.5,\n" +
            "            \"exportCostPerKwh\": 0.2\n" +
            "        },\n" +
            "        {\n" +
            "            \"start\": \"19:00\",\n" +
            "            \"end\": \"23:00\",\n" +
            "            \"name\": \"Tariff4\",\n" +
            "            \"importCostPerKwh\": 10.01,\n" +
            "            \"exportCostPerKwh\": 5.21\n" +
            "        },\n" +
            "        {\n" +
            "            \"start\": \"23:00\",\n" +
            "            \"end\": \"24:00\",\n" +
            "            \"name\": \"Tariff5\",\n" +
            "            \"importCostPerKwh\": 2.9,\n" +
            "            \"exportCostPerKwh\": 0.7\n" +
            "        }\n" +
            "    ]";

    @BeforeEach
    void setUp() {
        repository = new TariffRepository(mockDb);
    }

    @Test
    void testReadForUserWhoDoesNotExistReturnsEmptyOptional() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        var result = repository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDbButEmptyTariffList() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("user-id", stringValue("testuser"),
                        "tariffs", stringValue("[]"),
                        "currency", stringValue("EUR")))
                .build());
        var result = repository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getCurrency()).isEqualTo("EUR");
        assertThat(result.get().getTariffs()).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("user-id", stringValue("testuser"),
                        "tariffs", stringValue(testTariffString),
                        "currency", stringValue("EUR")))
                .build());
        var result = repository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getCurrency()).isEqualTo("EUR");
        var tariffs = result.get().getTariffs();
        assertThat(tariffs).hasSize(5);
        verifyTariffsAreEqual(tariffs.get(0), new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.2092, 0.21));
        verifyTariffsAreEqual(tariffs.get(1), new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(17, 0), 0.4241, 0.1));
        verifyTariffsAreEqual(tariffs.get(2), new Tariff("Tariff3", LocalTime.of(17, 0), LocalTime.of(19, 0), 0.5, 0.2));
        verifyTariffsAreEqual(tariffs.get(3), new Tariff("Tariff4", LocalTime.of(19, 0), LocalTime.of(23, 0), 10.01, 5.21));
        verifyTariffsAreEqual(tariffs.get(4), new Tariff("Tariff5", LocalTime.of(23, 0), LocalTime.of(0, 0), 2.9, 0.7));
    }

    private void verifyTariffsAreEqual(Tariff tariffFromDb, Tariff expected) {
        assertThat(tariffFromDb.getName()).isEqualTo(expected.getName());
        assertThat(tariffFromDb.getStart()).isEqualTo(expected.getStart());
        assertThat(tariffFromDb.getEnd()).isEqualTo(expected.getEnd());
        assertThat(tariffFromDb.getExportCostPerKwh()).isEqualTo(expected.getExportCostPerKwh());
        assertThat(tariffFromDb.getImportCostPerKwh()).isEqualTo(expected.getImportCostPerKwh());
    }

    @Test
    void testWrite() {
        var tariffs = List.of(new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.2092, 0.21),
                new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(17, 0), 0.4241, 0.1),
                new Tariff("Tariff3", LocalTime.of(17, 0), LocalTime.of(19, 0), 0.5, 0.2),
                new Tariff("Tariff4", LocalTime.of(19, 0), LocalTime.of(23, 0),10.01, 5.21),
                new Tariff("Tariff5", LocalTime.of(23, 0), LocalTime.of(0, 0), 2.9, 0.7));
        var dayTariff = new DayTariff("EUR", tariffs);
        repository.write("testUser", dayTariff);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().item()).hasSize(3);
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("tariff");
        assertThat(putItemCaptor.getValue().item().get("currency").s()).isEqualTo("EUR");
        assertThat(putItemCaptor.getValue().item().get("user-id").s()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().item().get("tariffs").s())
                .isEqualTo("[{\"start\":\"00:00\",\"end\":\"08:00\",\"name\":\"Tariff1\",\"importCostPerKwh\":0.2092,\"exportCostPerKwh\":0.21}," +
                        "{\"start\":\"08:00\",\"end\":\"17:00\",\"name\":\"Tariff2\",\"importCostPerKwh\":0.4241,\"exportCostPerKwh\":0.1}," +
                        "{\"start\":\"17:00\",\"end\":\"19:00\",\"name\":\"Tariff3\",\"importCostPerKwh\":0.5,\"exportCostPerKwh\":0.2}," +
                        "{\"start\":\"19:00\",\"end\":\"23:00\",\"name\":\"Tariff4\",\"importCostPerKwh\":10.01,\"exportCostPerKwh\":5.21}," +
                        "{\"start\":\"23:00\",\"end\":\"00:00\",\"name\":\"Tariff5\",\"importCostPerKwh\":2.9,\"exportCostPerKwh\":0.7}]");
    }
}

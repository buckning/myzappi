package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffRepositoryTest {
    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;

    private TariffRepository repository;

    private final String testTariffV2String = "[\n" +
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
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDbButEmptyTariffList() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "tariffs", new AttributeValue("[]"),
                "currency", new AttributeValue("EUR")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getCurrency()).isEqualTo("EUR");
        assertThat(result.get().getTariffs()).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "tariffs", new AttributeValue(testTariffV2String),
                "currency", new AttributeValue("EUR")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
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
        assertThat(putItemCaptor.getValue().getItem()).hasSize(3);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("tariff");
        assertThat(putItemCaptor.getValue().getItem().get("currency").getS()).isEqualTo("EUR");
        assertThat(putItemCaptor.getValue().getItem().get("user-id").getS()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().getItem().get("tariffs").getS())
                .isEqualTo("[{\"start\":\"00:00\",\"end\":\"08:00\",\"name\":\"Tariff1\",\"importCostPerKwh\":0.2092,\"exportCostPerKwh\":0.21}," +
                        "{\"start\":\"08:00\",\"end\":\"17:00\",\"name\":\"Tariff2\",\"importCostPerKwh\":0.4241,\"exportCostPerKwh\":0.1}," +
                        "{\"start\":\"17:00\",\"end\":\"19:00\",\"name\":\"Tariff3\",\"importCostPerKwh\":0.5,\"exportCostPerKwh\":0.2}," +
                        "{\"start\":\"19:00\",\"end\":\"23:00\",\"name\":\"Tariff4\",\"importCostPerKwh\":10.01,\"exportCostPerKwh\":5.21}," +
                        "{\"start\":\"23:00\",\"end\":\"00:00\",\"name\":\"Tariff5\",\"importCostPerKwh\":2.9,\"exportCostPerKwh\":0.7}]");
    }
}

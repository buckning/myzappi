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
    private final String testTariffString = "[\n" +
            "        {\n" +
            "            \"startTime\": 0,\n" +
            "            \"endTime\": 8,\n" +
            "            \"name\": \"Tariff1\",\n" +
            "            \"importCostPerKwh\": 0.2092,\n" +
            "            \"exportCostPerKwh\": 0.21\n" +
            "        },\n" +
            "        {\n" +
            "            \"startTime\": 8,\n" +
            "            \"endTime\": 17,\n" +
            "            \"name\": \"Tariff2\",\n" +
            "            \"importCostPerKwh\": 0.4241,\n" +
            "            \"exportCostPerKwh\": 0.10\n" +
            "        },\n" +
            "        {\n" +
            "            \"startTime\": 17,\n" +
            "            \"endTime\": 19,\n" +
            "            \"name\": \"Tariff3\",\n" +
            "            \"importCostPerKwh\": 0.5,\n" +
            "            \"exportCostPerKwh\": 0.2\n" +
            "        },\n" +
            "        {\n" +
            "            \"startTime\": 19,\n" +
            "            \"endTime\": 23,\n" +
            "            \"name\": \"Tariff4\",\n" +
            "            \"importCostPerKwh\": 10.01,\n" +
            "            \"exportCostPerKwh\": 5.21\n" +
            "        },\n" +
            "        {\n" +
            "            \"startTime\": 23,\n" +
            "            \"endTime\": 24,\n" +
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
                "tariffs", new AttributeValue(testTariffString),
                "currency", new AttributeValue("EUR")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getCurrency()).isEqualTo("EUR");
        var tariffs = result.get().getTariffs();
        assertThat(tariffs).hasSize(5);
        verifyTariffsAreEqual(tariffs.get(0), new Tariff("Tariff1", 0, 8, 0.2092, 0.21));
        verifyTariffsAreEqual(tariffs.get(1), new Tariff("Tariff2", 8, 17, 0.4241, 0.1));
        verifyTariffsAreEqual(tariffs.get(2), new Tariff("Tariff3", 17, 19, 0.5, 0.2));
        verifyTariffsAreEqual(tariffs.get(3), new Tariff("Tariff4", 19, 23, 10.01, 5.21));
        verifyTariffsAreEqual(tariffs.get(4), new Tariff("Tariff5", 23, 24, 2.9, 0.7));
    }

    private void verifyTariffsAreEqual(Tariff tariffFromDb, Tariff expected) {
        assertThat(tariffFromDb.getName()).isEqualTo(expected.getName());
        assertThat(tariffFromDb.getEndTime()).isEqualTo(expected.getEndTime());
        assertThat(tariffFromDb.getStartTime()).isEqualTo(expected.getStartTime());
        assertThat(tariffFromDb.getExportCostPerKwh()).isEqualTo(expected.getExportCostPerKwh());
        assertThat(tariffFromDb.getImportCostPerKwh()).isEqualTo(expected.getImportCostPerKwh());
    }

    @Test
    void testWrite() {
        var tariffs = List.of(new Tariff("Tariff1", 0, 8, 0.2092, 0.21),
                new Tariff("Tariff2", 8, 17, 0.4241, 0.1),
                new Tariff("Tariff3", 17, 19, 0.5, 0.2),
                new Tariff("Tariff4", 19, 23, 10.01, 5.21),
                new Tariff("Tariff5", 23, 24, 2.9, 0.7));
        var dayTariff = new DayTariff("EUR", tariffs);
        repository.write("testUser", dayTariff);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(3);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("tariff");
        assertThat(putItemCaptor.getValue().getItem().get("currency").getS()).isEqualTo("EUR");
        assertThat(putItemCaptor.getValue().getItem().get("user-id").getS()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().getItem().get("tariffs").getS())
                .isEqualTo("[{\"startTime\":0,\"endTime\":8,\"name\":\"Tariff1\",\"importCostPerKwh\":0.2092,\"exportCostPerKwh\":0.21}," +
                        "{\"startTime\":8,\"endTime\":17,\"name\":\"Tariff2\",\"importCostPerKwh\":0.4241,\"exportCostPerKwh\":0.1}," +
                        "{\"startTime\":17,\"endTime\":19,\"name\":\"Tariff3\",\"importCostPerKwh\":0.5,\"exportCostPerKwh\":0.2}," +
                        "{\"startTime\":19,\"endTime\":23,\"name\":\"Tariff4\",\"importCostPerKwh\":10.01,\"exportCostPerKwh\":5.21}," +
                        "{\"startTime\":23,\"endTime\":24,\"name\":\"Tariff5\",\"importCostPerKwh\":2.9,\"exportCostPerKwh\":0.7}]");
    }
}

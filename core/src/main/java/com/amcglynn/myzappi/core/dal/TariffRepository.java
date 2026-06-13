package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class TariffRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "tariff";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String TARIFFS_COLUMN = "tariffs";
    private static final String CURRENCY_COLUMN = "currency";

    public TariffRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Optional<DayTariff> read(String userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(USER_ID_COLUMN, stringValue(userId)))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }

        var tariffs = result.item().get(TARIFFS_COLUMN).s();
        var tariffList = new ObjectMapper().readValue(tariffs, new TypeReference<List<Tariff>>() {
        });
        var currency = result.item().get(CURRENCY_COLUMN).s();
        return Optional.of(new DayTariff(currency, tariffList));
    }

    @SneakyThrows
    public void write(String userId, DayTariff dayTariff) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(userId));
        item.put(CURRENCY_COLUMN, stringValue(dayTariff.getCurrency()));
        var tariffsString = new ObjectMapper().writeValueAsString(dayTariff.getTariffs());
        item.put(TARIFFS_COLUMN, stringValue(tariffsString));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    public void delete(String userId) {
        System.out.println("Deleting tariff for user: " + userId);
        var request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(USER_ID_COLUMN, stringValue(userId)))
                .build();
        dbClient.deleteItem(request);
    }
}

package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class TariffRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "tariff";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String TARIFFS_COLUMN = "tariffs";
    private static final String CURRENCY_COLUMN = "currency";

    public TariffRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Optional<DayTariff> read(String userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }

        var tariffs = result.getItem().get(TARIFFS_COLUMN).getS();
        var tariffList = new ObjectMapper().readValue(tariffs, new TypeReference<List<Tariff>>() {
        });
        var currency = result.getItem().get(CURRENCY_COLUMN).getS();
        return Optional.of(new DayTariff(currency, tariffList));
    }

    @SneakyThrows
    public void write(String userId, DayTariff dayTariff) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(userId));
        item.put(CURRENCY_COLUMN, new AttributeValue(dayTariff.getCurrency()));
        var tariffsString = new ObjectMapper().writeValueAsString(dayTariff.getTariffs());
        item.put(TARIFFS_COLUMN, new AttributeValue(tariffsString));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(String userId) {
        System.out.println("Deleting tariff for user: " + userId);
        var request = new DeleteItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId));
        dbClient.deleteItem(request);
    }
}

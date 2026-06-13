package com.amcglynn.myzappi.core.dal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlexaToLwaLookUpRepositoryTest {
    @Mock
    private DynamoDbClient mockDb;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private AlexaToLwaLookUpRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AlexaToLwaLookUpRepository(mockDb);
    }

    @Test
    void testReadForUserWhoDoesNotExistReturnsEmptyOptional() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        var result = repository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("alexa-user-id", stringValue("mockAlexaUser"),
                        "lwa-user-id", stringValue("mockLwaUser"),
                        "zone-id", stringValue("Europe/Dublin")))
                .build());
        var result = repository.read("mockAlexaUser");
        assertThat(result).isPresent();
        assertThat(result.get().getLwaUserId()).isEqualTo("mockLwaUser");
        assertThat(result.get().getAlexaUserId()).isEqualTo("mockAlexaUser");
        assertThat(result.get().getZoneId()).isEqualTo("Europe/Dublin");
    }

    @Test
    void testWrite() {
        repository.write("mockAlexaUser", "mockLwaUser", "Europe/Dublin");
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().item()).hasSize(3);
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("alexa-to-lwa-users-lookup");
        assertThat(putItemCaptor.getValue().item().get("lwa-user-id").s()).isEqualTo("mockLwaUser");
        assertThat(putItemCaptor.getValue().item().get("alexa-user-id").s()).isEqualTo("mockAlexaUser");
        assertThat(putItemCaptor.getValue().item().get("zone-id").s())
                .isEqualTo("Europe/Dublin");
    }

    @Test
    void testDelete() {
        repository.delete("mockAlexaUser");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("alexa-to-lwa-users-lookup");
        assertThat(deleteItemCaptor.getValue().key().get("alexa-user-id").s()).isEqualTo("mockAlexaUser");
    }
}

package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlexaToLwaLookUpRepositoryTest {
    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

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
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("alexa-user-id", new AttributeValue("mockAlexaUser"),
                "lwa-user-id", new AttributeValue("mockLwaUser"),
                "zone-id", new AttributeValue("Europe/Dublin")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
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
        assertThat(putItemCaptor.getValue().getItem()).hasSize(3);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("alexa-to-lwa-users-lookup");
        assertThat(putItemCaptor.getValue().getItem().get("lwa-user-id").getS()).isEqualTo("mockLwaUser");
        assertThat(putItemCaptor.getValue().getItem().get("alexa-user-id").getS()).isEqualTo("mockAlexaUser");
        assertThat(putItemCaptor.getValue().getItem().get("zone-id").getS())
                .isEqualTo("Europe/Dublin");
    }

    @Test
    void testDelete() {
        repository.delete("mockAlexaUser");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("alexa-to-lwa-users-lookup");
        assertThat(deleteItemCaptor.getValue().getKey().get("alexa-user-id").getS()).isEqualTo("mockAlexaUser");
    }
}

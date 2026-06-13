package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationRepositoryTest {

    @Mock
    private DynamoDbClient mockDb;
    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;
    @Captor
    private ArgumentCaptor<ScanRequest> scanRequestCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    private AutomationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AutomationRepository(mockDb);
    }

    @Test
    void readReturnsEmptyListWhenDefinitionRowDoesNotExist() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        var result = repository.read(UserId.from("user-1"));

        assertThat(result).isEmpty();
    }

    @Test
    void writeStoresDefinitionsAsAutomationsJsonBlob() throws Exception {
        repository.write(UserId.from("user-1"), List.of(automation("automation-1", 1)));

        verify(mockDb).putItem(putItemCaptor.capture());
        var request = putItemCaptor.getValue();
        assertThat(request.tableName()).isEqualTo("automation");
        assertThat(request.item().get("user-id").s()).isEqualTo("user-1");
        var automations = objectMapper.readValue(request.item().get("automations").s(),
                new TypeReference<List<Automation>>() {
                });
        assertThat(automations).hasSize(1);
        assertThat(automations.get(0).getAutomationId()).isEqualTo("automation-1");
    }

    @Test
    void deleteRemovesUserDefinitionRow() {
        repository.delete(UserId.from("user-1"));

        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("automation");
        assertThat(deleteItemCaptor.getValue().key().get("user-id").s()).isEqualTo("user-1");
    }

    @Test
    void scanReturnsUserIdsDefinitionsAndLastEvaluatedKey() {
        var lastEvaluatedKey = Map.of("user-id", stringValue("user-2"));
        var result = ScanResponse.builder()
                .items(List.of(
                        Map.of("user-id", stringValue("user-1"),
                                "automations", stringValue("""
                                        [{"automationId":"automation-1","priority":1,"predicate":{"type":"ENERGY_EXPORTING_KW","operator":"GREATER_THAN","value":"2.0"},"action":{"type":"setChargeMode","target":"10000001","value":"ECO_PLUS"}}]
                                        """))))
                .lastEvaluatedKey(lastEvaluatedKey)
                .build();
        when(mockDb.scan(any(ScanRequest.class))).thenReturn(result);

        var page = repository.scan(Map.of("user-id", "previous-user"), 25);

        verify(mockDb).scan(scanRequestCaptor.capture());
        assertThat(scanRequestCaptor.getValue().tableName()).isEqualTo("automation");
        assertThat(scanRequestCaptor.getValue().limit()).isEqualTo(25);
        assertThat(scanRequestCaptor.getValue().exclusiveStartKey().get("user-id").s()).isEqualTo("previous-user");
        assertThat(page.hasMore()).isTrue();
        assertThat(page.getLastEvaluatedKey()).isEqualTo(Map.of("user-id", "user-2"));
        assertThat(page.getUserAutomations()).hasSize(1);
        assertThat(page.getUserAutomations().get(0).getUserId()).isEqualTo(UserId.from("user-1"));
        assertThat(page.getUserAutomations().get(0).getAutomations()).hasSize(1);
    }

    @Test
    void scanSkipsMalformedRowsAndKeepsValidRowsInTheSameBatch() {
        var result = ScanResponse.builder()
                .items(List.of(
                        Map.of("user-id", stringValue("bad-user"),
                                "automations", stringValue("not-json")),
                        Map.of("user-id", stringValue("good-user"),
                                "automations", stringValue("""
                                [{"automationId":"automation-1","priority":1,"predicate":{"type":"ENERGY_EXPORTING_KW","operator":"GREATER_THAN","value":"2.0"},"action":{"type":"setChargeMode","target":"10000001","value":"ECO_PLUS"}}]
                                """))))
                .build();
        when(mockDb.scan(any(ScanRequest.class))).thenReturn(result);

        var page = repository.scan(null, 25);

        assertThat(page.getUserAutomations()).hasSize(1);
        assertThat(page.getUserAutomations().get(0).getUserId()).isEqualTo(UserId.from("good-user"));
    }

    private Automation automation(String id, int priority) {
        return Automation.builder()
                .automationId(id)
                .priority(priority)
                .predicate(AutomationPredicate.builder()
                        .type("ENERGY_EXPORTING_KW")
                        .operator(AutomationOperator.GREATER_THAN)
                        .value("2.0")
                        .build())
                .action(AutomationAction.builder()
                        .type("setChargeMode")
                        .target("10000001")
                        .value("ECO_PLUS")
                        .build())
                .build();
    }
}

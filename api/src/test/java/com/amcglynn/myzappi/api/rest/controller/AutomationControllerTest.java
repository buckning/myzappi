package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.exception.AutomationValidationException;
import com.amcglynn.myzappi.core.exception.CapacityReachedException;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationOptions;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.automation.AutomationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationControllerTest {

    @Mock
    private AutomationService service;
    @Captor
    private ArgumentCaptor<List<String>> orderedIdsCaptor;

    private AutomationController controller;
    private final UserId userId = UserId.from("user-1");

    @BeforeEach
    void setUp() {
        controller = new AutomationController(service);
    }

    @Test
    void getOptionsReturnsAutomationOptions() {
        when(service.getOptions()).thenReturn(AutomationOptions.builder()
                .predicates(List.of(AutomationOptions.PredicateOption.builder()
                        .type("ENERGY_EXPORTING_KW")
                        .valueType("DECIMAL")
                        .build()))
                .operators(List.of("GREATER_THAN", "LESS_THAN"))
                .actions(List.of(AutomationOptions.ActionOption.builder()
                        .type("setChargeMode")
                        .deviceClass(DeviceClass.ZAPPI)
                        .build()))
                .build());

        var response = controller.getOptions(new Request(userId, RequestMethod.GET, "/automations/options", null));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent();
        assertThat(response.getBody().get()).contains("ENERGY_EXPORTING_KW");
    }

    @Test
    void getAutomationsReturnsDefinitionsWithoutState() {
        when(service.listAutomations(userId)).thenReturn(List.of(automation()));

        var response = controller.getAutomations(new Request(userId, RequestMethod.GET, "/automations", null));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent();
        assertThat(response.getBody().get()).contains("\"automations\"");
        assertThat(response.getBody().get()).doesNotContain("lastTriggeredAt");
    }

    @Test
    void createAutomationReturnsCreatedDefinition() {
        when(service.createAutomation(eq(userId), any())).thenReturn(automation());

        var response = controller.createAutomation(new Request(userId, RequestMethod.POST, "/automations", body()));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent();
        assertThat(response.getBody().get()).contains("\"automationId\":\"automation-1\"");
    }

    @Test
    void createAutomationReturnsBadRequestForMalformedJson() {
        var serverException = catchThrowableOfType(() -> controller.createAutomation(
                new Request(userId, RequestMethod.POST, "/automations", "{")), ServerException.class);

        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void createAutomationReturnsConflictForMissingDevice() {
        when(service.createAutomation(eq(userId), any())).thenThrow(new MissingDeviceException("missing"));

        var serverException = catchThrowableOfType(() -> controller.createAutomation(
                new Request(userId, RequestMethod.POST, "/automations", body())), ServerException.class);

        assertThat(serverException.getStatus()).isEqualTo(409);
    }

    @Test
    void createAutomationReturnsTooManyRequestsForCapacityReached() {
        when(service.createAutomation(eq(userId), any())).thenThrow(new CapacityReachedException("limit"));

        var serverException = catchThrowableOfType(() -> controller.createAutomation(
                new Request(userId, RequestMethod.POST, "/automations", body())), ServerException.class);

        assertThat(serverException.getStatus()).isEqualTo(429);
    }

    @Test
    void createAutomationReturnsBadRequestForValidationFailure() {
        when(service.createAutomation(eq(userId), any())).thenThrow(new AutomationValidationException("bad"));

        var serverException = catchThrowableOfType(() -> controller.createAutomation(
                new Request(userId, RequestMethod.POST, "/automations", body())), ServerException.class);

        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void patchActiveUpdatesOnlyActiveState() {
        var response = controller.updateAutomation(new Request(userId, RequestMethod.PATCH,
                "/automations/automation-1", "{\"active\":false}"));

        assertThat(response.getStatus()).isEqualTo(204);
        verify(service).setActive(userId, "automation-1", false);
    }

    @Test
    void patchActiveReturnsBadRequestForMalformedJson() {
        var serverException = catchThrowableOfType(() -> controller.updateAutomation(
                new Request(userId, RequestMethod.PATCH, "/automations/automation-1", "{")), ServerException.class);

        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void reorderPrioritiesAcceptsFullOrderedIdList() {
        var response = controller.reorderAutomations(new Request(userId, RequestMethod.PUT,
                "/automations/priorities", "{\"automationIds\":[\"b\",\"a\"]}"));

        assertThat(response.getStatus()).isEqualTo(204);
        verify(service).reorder(eq(userId), orderedIdsCaptor.capture());
        assertThat(orderedIdsCaptor.getValue()).containsExactly("b", "a");
    }

    @Test
    void deleteAutomationReturnsNoContent() {
        var response = controller.deleteAutomation(new Request(userId, RequestMethod.DELETE,
                "/automations/automation-1", null));

        assertThat(response.getStatus()).isEqualTo(204);
        verify(service).deleteAutomation(userId, "automation-1");
    }

    private Automation automation() {
        return Automation.builder()
                .automationId("automation-1")
                .active(true)
                .priority(1)
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

    private String body() {
        return """
                {"predicate":{"type":"ENERGY_EXPORTING_KW","operator":"GREATER_THAN","value":"2.0"},"action":{"type":"setChargeMode","target":"10000001","value":"ECO_PLUS"}}
                """;
    }
}

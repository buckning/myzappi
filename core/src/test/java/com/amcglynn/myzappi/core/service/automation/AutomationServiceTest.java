package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.dal.AutomationRepository;
import com.amcglynn.myzappi.core.dal.AutomationStateRepository;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTest {

    @Mock
    private AutomationRepository automationRepository;
    @Mock
    private AutomationStateRepository automationStateRepository;
    @Mock
    private AutomationValidator automationValidator;
    @Mock
    private AutomationOptionsService automationOptionsService;
    @Mock
    private Clock clock;
    @Captor
    private ArgumentCaptor<List<Automation>> automationsCaptor;

    private AutomationService service;
    private final UserId userId = UserId.from("user-1");

    @BeforeEach
    void setUp() {
        service = new AutomationService(automationRepository, automationStateRepository, automationValidator,
                automationOptionsService, clock);
    }

    @Test
    void createAppendsPriorityGeneratesIdDefaultsActiveAndTrimsName() {
        when(automationRepository.read(userId)).thenReturn(List.of(existing("existing", 1)));
        when(clock.localDateTime(ZoneId.of("UTC"))).thenReturn(LocalDateTime.of(2026, 5, 30, 12, 0));

        var created = service.createAutomation(userId, request().toBuilder().name("  Export high  ").build());

        verify(automationRepository).write(eq(userId), automationsCaptor.capture());
        assertThat(created.getAutomationId()).isNotBlank();
        assertThat(created.getName()).isEqualTo("Export high");
        assertThat(created.isActive()).isTrue();
        assertThat(created.getPriority()).isEqualTo(2);
        assertThat(created.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 30, 12, 0));
        assertThat(automationsCaptor.getValue()).hasSize(2);
    }

    @Test
    void setActiveOnlyChangesActiveFlagAndPreservesState() {
        when(automationRepository.read(userId)).thenReturn(List.of(existing("automation-1", 1)));

        service.setActive(userId, "automation-1", false);

        verify(automationRepository).write(eq(userId), automationsCaptor.capture());
        assertThat(automationsCaptor.getValue().get(0).isActive()).isFalse();
        verify(automationStateRepository, never()).write(any(), any());
        verify(automationStateRepository, never()).delete(any());
    }

    @Test
    void reorderNormalizesPriorities() {
        when(automationRepository.read(userId)).thenReturn(List.of(existing("a", 1), existing("b", 2)));

        service.reorder(userId, List.of("b", "a"));

        verify(automationRepository).write(eq(userId), automationsCaptor.capture());
        assertThat(automationsCaptor.getValue()).extracting(Automation::getAutomationId).containsExactly("b", "a");
        assertThat(automationsCaptor.getValue()).extracting(Automation::getPriority).containsExactly(1, 2);
    }

    @Test
    void deleteCompactsPrioritiesAndLeavesStateWhenDefinitionsRemain() {
        when(automationRepository.read(userId)).thenReturn(List.of(existing("a", 1), existing("b", 2), existing("c", 3)));

        service.deleteAutomation(userId, "b");

        verify(automationRepository).write(eq(userId), automationsCaptor.capture());
        assertThat(automationsCaptor.getValue()).extracting(Automation::getAutomationId).containsExactly("a", "c");
        assertThat(automationsCaptor.getValue()).extracting(Automation::getPriority).containsExactly(1, 2);
        verify(automationStateRepository, never()).delete(userId);
    }

    @Test
    void deleteFinalAutomationDeletesDefinitionAndWholeStateRow() {
        when(automationRepository.read(userId)).thenReturn(List.of(existing("a", 1)));

        service.deleteAutomation(userId, "a");

        verify(automationRepository).delete(userId);
        verify(automationStateRepository).delete(userId);
    }

    private Automation request() {
        return Automation.builder()
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

    private Automation existing(String id, int priority) {
        return request().toBuilder()
                .automationId(id)
                .active(true)
                .priority(priority)
                .createdAt(LocalDateTime.of(2026, 5, 30, 11, 0))
                .build();
    }
}

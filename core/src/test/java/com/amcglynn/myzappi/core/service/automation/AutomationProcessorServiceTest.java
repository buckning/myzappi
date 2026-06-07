package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.core.dal.AutomationStateRepository;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.AutomationStateEntry;
import com.amcglynn.myzappi.core.model.EnergyStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.UserIdResolver;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationProcessorServiceTest {

    @Mock
    private AutomationStateRepository stateRepository;
    @Mock
    private MyEnergiService.Builder myEnergiServiceBuilder;
    @Mock
    private MyEnergiService myEnergiService;
    @Mock
    private AutomationActionExecutor actionExecutor;
    @Mock
    private Clock clock;
    @Captor
    private ArgumentCaptor<Map<String, AutomationStateEntry>> stateCaptor;

    private AutomationProcessorService service;
    private final UserId userId = UserId.from("user-1");
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 30, 12, 0);

    @BeforeEach
    void setUp() {
        service = new AutomationProcessorService(stateRepository, myEnergiServiceBuilder, new PredicateEvaluator(),
                actionExecutor, clock);
        when(clock.localDateTime(ZoneId.of("UTC"))).thenReturn(now);
    }

    @Test
    void missingStateAndTruePredicateTriggersAction() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of());
        var automation = automation("a", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));

        service.processUser(userId, List.of(automation));

        verify(actionExecutor).execute(userId, myEnergiService, automation.getAction());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("a").getLastPredicateMatched()).isTrue();
        assertThat(stateCaptor.getValue().get("a").getLastTriggeredAt()).isEqualTo(now);
    }

    @Test
    void previousFalseAndCurrentTruePredicateTriggersAction() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of("a", state(false)));
        var automation = automation("a", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));

        service.processUser(userId, List.of(automation));

        verify(actionExecutor).execute(userId, myEnergiService, automation.getAction());
    }

    @Test
    void previousTrueAndCurrentTruePredicateExecutesWhenTargetStateHasDrifted() {
        arrangeSnapshot(ZappiChargeMode.FAST, 100);
        when(stateRepository.read(userId)).thenReturn(Map.of("a", state(true)));
        var automation = automation("a", 1, "ENERGY_EXPORTING_KW",
                AutomationOperator.GREATER_THAN, "2.0", action("setChargeMode", "ECO_PLUS"));

        service.processUser(userId, List.of(automation));

        verify(actionExecutor).execute(userId, myEnergiService, automation.getAction());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("a").getLastPredicateMatched()).isTrue();
        assertThat(stateCaptor.getValue().get("a").getLastTriggeredAt()).isEqualTo(now);
    }

    @Test
    void previousTrueAndCurrentTruePredicateDoesNotExecuteWhenTargetStateIsAlreadySatisfied() {
        arrangeSnapshot(ZappiChargeMode.ECO_PLUS, 100);
        when(stateRepository.read(userId)).thenReturn(Map.of("a", state(true)));

        service.processUser(userId, List.of(automation("a", 1, "ENERGY_EXPORTING_KW",
                AutomationOperator.GREATER_THAN, "2.0", action("setChargeMode", "ECO_PLUS"))));

        verify(actionExecutor, never()).execute(any(), any(), any());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("a").getLastPredicateMatched()).isTrue();
    }

    @Test
    void previousTrueAndCurrentTruePredicateExecutesWhenMinimumGreenLevelHasDrifted() {
        arrangeSnapshot(ZappiChargeMode.ECO_PLUS, 30);
        when(stateRepository.read(userId)).thenReturn(Map.of("a", state(true)));
        var automation = automation("a", 1, "ENERGY_EXPORTING_KW",
                AutomationOperator.GREATER_THAN, "2.0", action("setZappiMgl", "75"));

        service.processUser(userId, List.of(automation));

        verify(actionExecutor).execute(userId, myEnergiService, automation.getAction());
    }

    @Test
    void previousTrueAndCurrentFalseUpdatesStateToFalse() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of("a", state(true)));

        service.processUser(userId, List.of(automation("a", 1, "ENERGY_EXPORTING_KW",
                AutomationOperator.GREATER_THAN, "9.0", action("setChargeMode", "ECO_PLUS"))));

        verify(actionExecutor, never()).execute(any(), any(), any());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("a").getLastPredicateMatched()).isFalse();
    }

    @Test
    void reEnabledAutomationWithCleanedStateCanTriggerWhenPredicateIsTrue() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of());
        var automation = automation("a", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));

        service.processUser(userId, List.of(automation));

        verify(actionExecutor).execute(userId, myEnergiService, automation.getAction());
    }

    @Test
    void disabledAutomationStateIsRemovedWithoutExecuting() {
        when(stateRepository.read(userId)).thenReturn(Map.of("a", state(true)));
        var disabled = automation("a", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS")).toBuilder().active(false).build();

        service.processUser(userId, List.of(disabled));

        verify(actionExecutor, never()).execute(any(), any(), any());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue()).isEmpty();
    }

    @Test
    void orphanStateIsRemovedWhenDefinitionIsMissing() {
        when(stateRepository.read(userId)).thenReturn(Map.of("orphan", state(true)));

        service.processUser(userId, List.of());

        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue()).isEmpty();
    }

    @Test
    void highestPriorityConflictExecutesAndLowerPriorityConflictIsSkipped() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of());
        var high = automation("high", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));
        var low = automation("low", 2, "ENERGY_SOLAR_GENERATION_KW", AutomationOperator.GREATER_THAN, "4.0",
                action("setChargeMode", "FAST"));

        service.processUser(userId, List.of(low, high));

        verify(actionExecutor).execute(userId, myEnergiService, high.getAction());
        verify(actionExecutor, never()).execute(userId, myEnergiService, low.getAction());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("low").getLastSkippedReason()).contains("higher priority");
    }

    @Test
    void failedHighestPriorityConflictDoesNotFallBack() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of());
        var high = automation("high", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));
        var low = automation("low", 2, "ENERGY_SOLAR_GENERATION_KW", AutomationOperator.GREATER_THAN, "4.0",
                action("setChargeMode", "FAST"));
        doThrow(new RuntimeException("command failed")).when(actionExecutor).execute(userId, myEnergiService, high.getAction());

        service.processUser(userId, List.of(high, low));

        verify(actionExecutor).execute(userId, myEnergiService, high.getAction());
        verify(actionExecutor, never()).execute(userId, myEnergiService, low.getAction());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("high").getLastError()).isEqualTo("command failed");
        assertThat(stateCaptor.getValue().get("high").getLastPredicateMatched()).isFalse();
    }

    @Test
    void lowerPrioritySkippedConflictRecordsActualPredicateMatchState() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of());
        var high = automation("high", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));
        var low = automation("low", 2, "ENERGY_SOLAR_GENERATION_KW", AutomationOperator.GREATER_THAN, "4.0",
                action("setChargeMode", "FAST"));

        service.processUser(userId, List.of(high, low));

        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("low").getLastPredicateMatched()).isTrue();
    }

    @Test
    void satisfiedHigherPriorityMatchBlocksLowerPriorityConflict() {
        arrangeSnapshot(ZappiChargeMode.ECO_PLUS, 100);
        when(stateRepository.read(userId)).thenReturn(Map.of("high", state(true), "low", state(true)));
        var high = automation("high", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));
        var low = automation("low", 2, "ENERGY_SOLAR_GENERATION_KW", AutomationOperator.GREATER_THAN, "4.0",
                action("setChargeMode", "FAST"));

        service.processUser(userId, List.of(high, low));

        verify(actionExecutor, never()).execute(any(), any(), any());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("high").getLastPredicateMatched()).isTrue();
        assertThat(stateCaptor.getValue().get("low").getLastSkippedReason()).contains("higher priority");
    }

    @Test
    void actionFailureRecordsFailureAndContinuesNonConflictingAutomation() {
        arrangeSnapshot();
        when(stateRepository.read(userId)).thenReturn(Map.of());
        var failing = automation("failing", 1, "ENERGY_EXPORTING_KW", AutomationOperator.GREATER_THAN, "2.0",
                action("setChargeMode", "ECO_PLUS"));
        var other = automation("other", 2, "ENERGY_SOLAR_GENERATION_KW", AutomationOperator.GREATER_THAN, "4.0",
                action("setZappiMgl", "50"));
        doThrow(new RuntimeException("failed")).when(actionExecutor).execute(userId, myEnergiService, failing.getAction());

        service.processUser(userId, List.of(failing, other));

        verify(actionExecutor).execute(userId, myEnergiService, failing.getAction());
        verify(actionExecutor).execute(userId, myEnergiService, other.getAction());
        verify(stateRepository).write(eq(userId), stateCaptor.capture());
        assertThat(stateCaptor.getValue().get("failing").getLastFailedAt()).isEqualTo(now);
        assertThat(stateCaptor.getValue().get("other").getLastTriggeredAt()).isEqualTo(now);
    }

    @Test
    void snapshotFailureRecordsUserFailureWithoutWritingPartialActionState() {
        when(stateRepository.read(userId)).thenReturn(Map.of());
        when(myEnergiServiceBuilder.build(any(UserIdResolver.class))).thenReturn(myEnergiService);
        when(myEnergiService.getAutomationSnapshot()).thenThrow(new RuntimeException("myenergi down"));

        var result = service.processUser(userId, List.of(automation("a", 1, "ENERGY_EXPORTING_KW",
                AutomationOperator.GREATER_THAN, "2.0", action("setChargeMode", "ECO_PLUS"))));

        assertThat(result.getFailed()).isEqualTo(1);
        verify(actionExecutor, never()).execute(any(), any(), any());
        verify(stateRepository, never()).write(any(), any());
    }

    private void arrangeSnapshot() {
        arrangeSnapshot(ZappiChargeMode.FAST, 30);
    }

    private void arrangeSnapshot(ZappiChargeMode zappiChargeMode, int mgl) {
        when(myEnergiServiceBuilder.build(any(UserIdResolver.class))).thenReturn(myEnergiService);
        when(myEnergiService.getAutomationSnapshot()).thenReturn(AutomationSnapshot.builder()
                .energyStatus(EnergyStatus.builder()
                        .solarGenerationKW(new KiloWatt(new Watt(5000L)))
                        .exportingKW(new KiloWatt(new Watt(3000L)))
                        .importingKW(new KiloWatt(new Watt(0L)))
                        .consumingKW(new KiloWatt(new Watt(2000L)))
                        .build())
                .zappiEvChargeRateKWBySerialNumber(Map.of(SerialNumber.from("10000001"), new KiloWatt(new Watt(3000L))))
                .zappiChargeModeBySerialNumber(Map.of(SerialNumber.from("10000001"), zappiChargeMode))
                .zappiMinimumGreenLevelBySerialNumber(Map.of(SerialNumber.from("10000001"), mgl))
                .libbiStateOfChargePercentBySerialNumber(Map.of(SerialNumber.from("30000001"), 80))
                .build());
    }

    private AutomationStateEntry state(boolean matched) {
        return AutomationStateEntry.builder()
                .lastPredicateMatched(matched)
                .lastEvaluatedAt(LocalDateTime.of(2026, 5, 30, 11, 55))
                .lastTriggeredAt(matched ? LocalDateTime.of(2026, 5, 30, 11, 55) : null)
                .build();
    }

    private Automation automation(String id, int priority, String predicateType, AutomationOperator operator,
                                  String predicateValue, AutomationAction action) {
        return Automation.builder()
                .automationId(id)
                .active(true)
                .priority(priority)
                .predicate(AutomationPredicate.builder()
                        .type(predicateType)
                        .operator(operator)
                        .value(predicateValue)
                        .build())
                .action(action)
                .build();
    }

    private AutomationAction action(String type, String value) {
        return AutomationAction.builder()
                .type(type)
                .target("10000001")
                .value(value)
                .build();
    }
}

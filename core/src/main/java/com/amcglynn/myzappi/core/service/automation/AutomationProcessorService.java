package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.dal.AutomationStateRepository;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationStateEntry;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AutomationProcessorService {

    private static final String CONFLICT_SKIP_REASON =
            "Skipped because a higher priority automation for the same action target triggered in this run";

    private final AutomationStateRepository automationStateRepository;
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final PredicateEvaluator predicateEvaluator;
    private final AutomationActionExecutor actionExecutor;
    private final Clock clock;

    public AutomationProcessorService(AutomationStateRepository automationStateRepository,
                                      MyEnergiService.Builder myEnergiServiceBuilder,
                                      PredicateEvaluator predicateEvaluator,
                                      AutomationActionExecutor actionExecutor,
                                      Clock clock) {
        this.automationStateRepository = automationStateRepository;
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
        this.predicateEvaluator = predicateEvaluator;
        this.actionExecutor = actionExecutor;
        this.clock = clock;
    }

    public AutomationProcessingResult processUser(UserId userId, List<Automation> definitions) {
        var now = clock.localDateTime(ZoneId.of("UTC"));
        var states = new HashMap<>(automationStateRepository.read(userId));
        var definitionsById = definitions.stream()
                .collect(Collectors.toMap(Automation::getAutomationId, automation -> automation));
        var activeAutomations = definitions.stream()
                .filter(Automation::isActive)
                .sorted(Comparator.comparingInt(Automation::getPriority))
                .toList();

        cleanupDisabledAndOrphanedStates(states, definitionsById.keySet(), activeAutomations);
        if (activeAutomations.isEmpty()) {
            automationStateRepository.write(userId, states);
            return AutomationProcessingResult.builder().build();
        }

        MyEnergiService myEnergiService;
        try {
            myEnergiService = myEnergiServiceBuilder.build(userId::toString);
            var snapshot = myEnergiService.getAutomationSnapshot();
            return evaluateAndExecute(userId, myEnergiService, snapshot, activeAutomations, states, now);
        } catch (Exception e) {
            log.error("Failed to fetch automation snapshot for user {}", userId, e);
            return AutomationProcessingResult.builder().failed(1).build();
        }
    }

    private AutomationProcessingResult evaluateAndExecute(UserId userId,
                                                          MyEnergiService myEnergiService,
                                                          com.amcglynn.myzappi.core.model.AutomationSnapshot snapshot,
                                                          List<Automation> activeAutomations,
                                                          Map<String, AutomationStateEntry> states,
                                                          LocalDateTime now) {
        var triggered = new ArrayList<Automation>();
        var evaluated = 0;
        var failed = 0;

        for (Automation automation : activeAutomations) {
            evaluated++;
            try {
                var matched = predicateEvaluator.evaluate(automation.getPredicate(), snapshot);
                var previous = states.get(automation.getAutomationId());
                var previousMatched = previous != null && Boolean.TRUE.equals(previous.getLastPredicateMatched());
                if (matched && !previousMatched) {
                    triggered.add(automation);
                } else {
                    states.put(automation.getAutomationId(), evaluatedState(matched, now, previous));
                }
            } catch (Exception e) {
                failed++;
                states.put(automation.getAutomationId(), failedState(e, now));
            }
        }

        var executionResult = executeTriggered(userId, myEnergiService, triggered, states, now);
        automationStateRepository.write(userId, states);
        return AutomationProcessingResult.builder()
                .evaluated(evaluated)
                .triggered(triggered.size())
                .executed(executionResult.executed())
                .skipped(executionResult.skipped())
                .failed(failed + executionResult.failed())
                .build();
    }

    private ExecutionResult executeTriggered(UserId userId,
                                             MyEnergiService myEnergiService,
                                             List<Automation> triggered,
                                             Map<String, AutomationStateEntry> states,
                                             LocalDateTime now) {
        var grouped = new LinkedHashMap<AutomationConflictKey, List<Automation>>();
        triggered.stream()
                .sorted(Comparator.comparingInt(Automation::getPriority))
                .forEach(automation -> grouped.computeIfAbsent(AutomationConflictKey.from(automation), ignored -> new ArrayList<>())
                        .add(automation));

        var executed = 0;
        var skipped = 0;
        var failed = 0;
        for (List<Automation> conflictGroup : grouped.values()) {
            var winner = conflictGroup.get(0);
            try {
                actionExecutor.execute(userId, myEnergiService, winner.getAction());
                states.put(winner.getAutomationId(), triggeredState(now));
                executed++;
            } catch (Exception e) {
                states.put(winner.getAutomationId(), failedState(e, now));
                failed++;
            }
            for (int i = 1; i < conflictGroup.size(); i++) {
                var skippedAutomation = conflictGroup.get(i);
                states.put(skippedAutomation.getAutomationId(),
                        skippedState(states.get(skippedAutomation.getAutomationId()), now));
                skipped++;
            }
        }
        return new ExecutionResult(executed, skipped, failed);
    }

    private void cleanupDisabledAndOrphanedStates(Map<String, AutomationStateEntry> states,
                                                  Set<String> definitionIds,
                                                  List<Automation> activeAutomations) {
        var activeIds = activeAutomations.stream()
                .map(Automation::getAutomationId)
                .collect(Collectors.toSet());
        states.keySet().removeIf(automationId -> !definitionIds.contains(automationId) || !activeIds.contains(automationId));
    }

    private AutomationStateEntry evaluatedState(boolean matched, LocalDateTime now, AutomationStateEntry previous) {
        return AutomationStateEntry.builder()
                .lastPredicateMatched(matched)
                .lastEvaluatedAt(now)
                .lastTriggeredAt(previous == null ? null : previous.getLastTriggeredAt())
                .build();
    }

    private AutomationStateEntry triggeredState(LocalDateTime now) {
        return AutomationStateEntry.builder()
                .lastPredicateMatched(true)
                .lastEvaluatedAt(now)
                .lastTriggeredAt(now)
                .build();
    }

    private AutomationStateEntry failedState(Exception exception, LocalDateTime now) {
        return AutomationStateEntry.builder()
                .lastPredicateMatched(false)
                .lastEvaluatedAt(now)
                .lastError(exception.getMessage())
                .lastFailedAt(now)
                .build();
    }

    private AutomationStateEntry skippedState(AutomationStateEntry previous, LocalDateTime now) {
        return AutomationStateEntry.builder()
                .lastPredicateMatched(previous != null && Boolean.TRUE.equals(previous.getLastPredicateMatched()))
                .lastEvaluatedAt(now)
                .lastTriggeredAt(previous == null ? null : previous.getLastTriggeredAt())
                .lastSkippedReason(CONFLICT_SKIP_REASON)
                .build();
    }

    private record ExecutionResult(int executed, int skipped, int failed) {
    }
}

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
            "Skipped because a higher priority automation for the same action target matched in this run";

    private final AutomationStateRepository automationStateRepository;
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final PredicateEvaluator predicateEvaluator;
    private final AutomationActionExecutor actionExecutor;
    private final AutomationActionStateEvaluator actionStateEvaluator;
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
        this.actionStateEvaluator = new AutomationActionStateEvaluator();
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
        var matchedAutomations = new ArrayList<MatchedAutomation>();
        var evaluated = 0;
        var failed = 0;

        for (Automation automation : activeAutomations) {
            log.info("Processing automation, {} for user {}", automation, userId);
            evaluated++;
            try {
                var matched = predicateEvaluator.evaluate(automation.getPredicate(), snapshot);
                var previous = states.get(automation.getAutomationId());
                if (matched) {
                    matchedAutomations.add(new MatchedAutomation(automation, shouldExecute(automation, snapshot, previous)));
                } else {
                    states.put(automation.getAutomationId(), evaluatedState(false, now, previous));
                }
            } catch (Exception e) {
                failed++;
                states.put(automation.getAutomationId(), failedState(e, now));
            }
        }

        var executionResult = executeMatched(userId, myEnergiService, matchedAutomations, states, now);
        automationStateRepository.write(userId, states);
        return AutomationProcessingResult.builder()
                .evaluated(evaluated)
                .triggered(executionResult.triggered())
                .executed(executionResult.executed())
                .skipped(executionResult.skipped())
                .failed(failed + executionResult.failed())
                .build();
    }

    private boolean shouldExecute(Automation automation,
                                  com.amcglynn.myzappi.core.model.AutomationSnapshot snapshot,
                                  AutomationStateEntry previous) {
        return actionStateEvaluator.isSatisfied(automation.getAction(), snapshot)
                .map(satisfied -> !satisfied)
                .orElseGet(() -> previous == null || !Boolean.TRUE.equals(previous.getLastPredicateMatched()));
    }

    private ExecutionResult executeMatched(UserId userId,
                                           MyEnergiService myEnergiService,
                                           List<MatchedAutomation> matchedAutomations,
                                           Map<String, AutomationStateEntry> states,
                                           LocalDateTime now) {
        var grouped = new LinkedHashMap<AutomationConflictKey, List<MatchedAutomation>>();
        matchedAutomations.stream()
                .sorted(Comparator.comparingInt(matched -> matched.automation().getPriority()))
                .forEach(matched -> grouped.computeIfAbsent(AutomationConflictKey.from(matched.automation()), ignored -> new ArrayList<>())
                        .add(matched));

        var triggered = 0;
        var executed = 0;
        var skipped = 0;
        var failed = 0;
        for (List<MatchedAutomation> conflictGroup : grouped.values()) {
            var winner = conflictGroup.get(0);
            var winnerAutomation = winner.automation();
            if (winner.shouldExecute()) {
                triggered++;
                try {
                    actionExecutor.execute(userId, myEnergiService, winnerAutomation.getAction());
                    states.put(winnerAutomation.getAutomationId(), triggeredState(now));
                    executed++;
                } catch (Exception e) {
                    states.put(winnerAutomation.getAutomationId(), failedState(e, now));
                    failed++;
                }
            } else {
                states.put(winnerAutomation.getAutomationId(),
                        evaluatedState(true, now, states.get(winnerAutomation.getAutomationId())));
            }
            for (int i = 1; i < conflictGroup.size(); i++) {
                var skippedAutomation = conflictGroup.get(i).automation();
                states.put(skippedAutomation.getAutomationId(),
                        skippedState(states.get(skippedAutomation.getAutomationId()), now));
                log.info("Skipped automation for user {}, {}", userId, skippedAutomation);
                skipped++;
            }
        }
        return new ExecutionResult(triggered, executed, skipped, failed);
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
                .lastPredicateMatched(true)
                .lastEvaluatedAt(now)
                .lastTriggeredAt(previous == null ? null : previous.getLastTriggeredAt())
                .lastSkippedReason(CONFLICT_SKIP_REASON)
                .build();
    }

    private record MatchedAutomation(Automation automation, boolean shouldExecute) {
    }

    private record ExecutionResult(int triggered, int executed, int skipped, int failed) {
    }
}

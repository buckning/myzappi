package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.dal.AutomationRepository;
import com.amcglynn.myzappi.core.dal.AutomationStateRepository;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationOptions;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AutomationService {

    private final AutomationRepository automationRepository;
    private final AutomationStateRepository automationStateRepository;
    private final AutomationValidator automationValidator;
    private final AutomationOptionsService automationOptionsService;
    private final Clock clock;

    public AutomationService(AutomationRepository automationRepository,
                             AutomationStateRepository automationStateRepository,
                             AutomationValidator automationValidator,
                             AutomationOptionsService automationOptionsService,
                             Clock clock) {
        this.automationRepository = automationRepository;
        this.automationStateRepository = automationStateRepository;
        this.automationValidator = automationValidator;
        this.automationOptionsService = automationOptionsService;
        this.clock = clock;
    }

    public List<Automation> listAutomations(UserId userId) {
        return automationRepository.read(userId).stream()
                .sorted(Comparator.comparingInt(Automation::getPriority))
                .toList();
    }

    public Automation createAutomation(UserId userId, Automation request) {
        var automations = new ArrayList<>(automationRepository.read(userId));
        automationValidator.validateForCreate(userId, request, automations.size());
        var created = request.toBuilder()
                .automationId(UUID.randomUUID().toString())
                .name(normalizeName(request.getName()))
                .active(request.getActive() == null || request.getActive())
                .priority(automations.size() + 1)
                .createdAt(clock.localDateTime(ZoneId.of("UTC")))
                .build();
        automations.add(created);
        automationRepository.write(userId, automations);
        return created;
    }

    public void setActive(UserId userId, String automationId, boolean active) {
        var automations = new ArrayList<>(automationRepository.read(userId));
        automations.stream()
                .filter(automation -> automation.getAutomationId().equals(automationId))
                .findFirst()
                .ifPresent(automation -> automation.setActive(active));
        automationRepository.write(userId, automations);
    }

    public void reorder(UserId userId, List<String> orderedAutomationIds) {
        var automations = automationRepository.read(userId);
        automationValidator.validatePriorityReorder(automations, orderedAutomationIds);
        var reordered = new ArrayList<Automation>();
        for (int i = 0; i < orderedAutomationIds.size(); i++) {
            var id = orderedAutomationIds.get(i);
            var priority = i + 1;
            automations.stream()
                    .filter(automation -> automation.getAutomationId().equals(id))
                    .findFirst()
                    .ifPresent(automation -> reordered.add(automation.toBuilder().priority(priority).build()));
        }
        automationRepository.write(userId, reordered);
    }

    public void deleteAutomation(UserId userId, String automationId) {
        var remaining = new ArrayList<>(automationRepository.read(userId));
        remaining.removeIf(automation -> automation.getAutomationId().equals(automationId));
        if (remaining.isEmpty()) {
            automationRepository.delete(userId);
            automationStateRepository.delete(userId);
            return;
        }
        var normalized = new ArrayList<Automation>();
        for (int i = 0; i < remaining.size(); i++) {
            normalized.add(remaining.get(i).toBuilder().priority(i + 1).build());
        }
        automationRepository.write(userId, normalized);
    }

    public AutomationOptions getOptions() {
        return automationOptionsService.getOptions();
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return name.trim();
    }
}

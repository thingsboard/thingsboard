// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class StepLinkedListUtils {

    public AgentAppStep findByStepId(List<AgentAppStep> steps, UUID stepId) {
        if (CollectionUtils.isEmpty(steps) || stepId == null) {
            return null;
        }
        return steps.stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find the first step (the step not referenced by any other step's nextId).
     *
     * @param steps list of steps
     * @return the first step, or null if list is empty
     * @throws IllegalStateException if no first step found or multiple first steps exist
     */
    public AgentAppStep findFirstStep(List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return null;
        }

        Map<UUID, AgentAppStep> stepsById = steps.stream()
                .collect(Collectors.toMap(AgentAppStep::getId, Function.identity()));

        Set<UUID> referencedIds = steps.stream()
                .map(AgentAppStep::getNextId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> firstStepCandidates = new HashSet<>(stepsById.keySet());
        firstStepCandidates.removeAll(referencedIds);

        if (firstStepCandidates.isEmpty()) {
            throw new IllegalStateException("No first step found - possible circular reference");
        }
        if (firstStepCandidates.size() > 1) {
            throw new IllegalStateException("Multiple first steps found: " + firstStepCandidates);
        }

        return stepsById.get(firstStepCandidates.iterator().next());
    }

    /**
     * Convert an unordered list of steps to an ordered list following the nextId chain.
     *
     * @param steps list of steps with nextId references
     * @return ordered list of steps
     * @throws IllegalStateException if chain is broken or has cycles
     */
    public List<AgentAppStep> toOrderedList(List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return List.of();
        }

        Map<UUID, AgentAppStep> stepsById = steps.stream()
                .collect(Collectors.toMap(AgentAppStep::getId, Function.identity()));

        AgentAppStep firstStep = findFirstStep(steps);
        UUID firstId = firstStep.getId();
        List<AgentAppStep> ordered = new ArrayList<>(steps.size());
        Set<UUID> visited = new HashSet<>();

        UUID currentId = firstId;
        while (currentId != null) {
            if (visited.contains(currentId)) {
                throw new IllegalStateException("Circular reference detected at step: " + currentId);
            }

            AgentAppStep step = stepsById.get(currentId);
            if (step == null) {
                throw new IllegalStateException("Broken chain - step not found: " + currentId);
            }

            visited.add(currentId);
            ordered.add(step);
            currentId = step.getNextId();
        }

        if (ordered.size() != steps.size()) {
            throw new IllegalStateException("Orphaned steps detected. Expected " + steps.size() +
                    " steps but chain contains " + ordered.size());
        }

        return ordered;
    }

    /**
     * Validate the step list for cycles and orphans.
     *
     * @param steps list of steps to validate
     * @throws IllegalStateException if validation fails
     */
    public void validate(List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return;
        }

        // Validate all steps have IDs
        for (AgentAppStep step : steps) {
            if (step.getId() == null) {
                throw new IllegalStateException("Step has null id: " + step.getTitle());
            }
        }

        // Validate nextId references exist
        Set<UUID> allIds = steps.stream()
                .map(AgentAppStep::getId)
                .collect(Collectors.toSet());

        for (AgentAppStep step : steps) {
            if (step.getNextId() != null && !allIds.contains(step.getNextId())) {
                throw new IllegalStateException("Step '" + step.getTitle() +
                        "' references non-existent nextId: " + step.getNextId());
            }
        }

        // This will throw if there are cycles or orphans
        toOrderedList(steps);
    }

    /**
     * Find the step that follows the step with the given ID in the linked list.
     *
     * @param currentStepId the ID of the current step
     * @param steps         list of steps
     * @return the next step, or empty if currentStepId is null, not found, or is the last step
     */
    public Optional<AgentAppStep> getNextStep(UUID currentStepId, List<AgentAppStep> steps) {
        if (currentStepId == null || CollectionUtils.isEmpty(steps)) {
            return Optional.empty();
        }
        Map<UUID, AgentAppStep> stepsById = steps.stream()
                .collect(Collectors.toMap(AgentAppStep::getId, Function.identity()));
        AgentAppStep current = stepsById.get(currentStepId);
        if (current == null || current.getNextId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stepsById.get(current.getNextId()));
    }

    public static <T extends AgentAppStep> Optional<T> getByType(AgentAppStepType agentAppStepType,
                                                              Class<T> stepCls,
                                                              List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return Optional.empty();
        }
        return steps.stream()
                .filter(stepCls::isInstance)
                .map(stepCls::cast)
                .filter(s -> s.getType() == agentAppStepType)
                .findFirst();
    }

    public static List<AgentAppStep> filter(List<AgentAppStep> steps, Predicate<AgentAppStep> predicate) {
        return steps.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Remove every step matching {@code remove} and re-stitch the {@code id -> nextId} chain so the surviving steps
     * remain linked: each removed step's predecessor is pointed at the removed step's {@code nextId}. Handles head
     * removal (no predecessor) and consecutive removals. The predecessors' {@code nextId} is mutated in place, so the
     * caller should pass steps it owns (e.g. a deep copy during template materialization).
     *
     * @param steps  list of linked steps
     * @param remove predicate selecting steps to drop
     * @return a new list containing only the surviving steps, with re-stitched {@code nextId} references
     */
    public static List<AgentAppStep> removeAndRestitch(List<AgentAppStep> steps, Predicate<AgentAppStep> remove) {
        if (CollectionUtils.isEmpty(steps)) {
            return steps == null ? new ArrayList<>() : new ArrayList<>(steps);
        }
        List<AgentAppStep> result = new ArrayList<>(steps);
        List<AgentAppStep> victims = result.stream().filter(remove).toList();
        for (AgentAppStep victim : victims) {
            UUID victimId = victim.getId();
            for (AgentAppStep candidate : result) {
                if (victimId != null && victimId.equals(candidate.getNextId())) {
                    candidate.setNextId(victim.getNextId());
                }
            }
            result.remove(victim);
        }
        return result;
    }
}

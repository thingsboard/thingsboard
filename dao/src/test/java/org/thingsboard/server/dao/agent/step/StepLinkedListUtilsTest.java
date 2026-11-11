// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.step;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StepLinkedListUtilsTest {

    // ==================== findFirstStep() tests ====================

    @Test
    void findFirstStep_shouldReturnNull_whenListIsEmpty() {
        assertNull(StepLinkedListUtils.findFirstStep(Collections.emptyList()));
        assertNull(StepLinkedListUtils.findFirstStep(null));
    }

    @Test
    void findFirstStep_shouldReturnOnlyStep_whenSingleStep() {
        UUID stepId = UUID.randomUUID();
        ComposeStartStep step = createStep(stepId, null, "Only Step");

        AgentAppStep firstStep = StepLinkedListUtils.findFirstStep(List.of(step));

        assertNotNull(firstStep);
        assertEquals(stepId, firstStep.getId());
    }

    @Test
    void findFirstStep_shouldFindFirstStep_inChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2 -> 3
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        // Provide in random order
        AgentAppStep firstStep = StepLinkedListUtils.findFirstStep(List.of(step3, step1, step2));

        assertNotNull(firstStep);
        assertEquals(id1, firstStep.getId());
    }

    @Test
    void findFirstStep_shouldThrow_whenCircularReference() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Circular: 1 -> 2 -> 1
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id1, "Second");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.findFirstStep(List.of(step1, step2)));
    }

    @Test
    void findFirstStep_shouldThrow_whenMultipleFirstSteps() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Two separate chains: 1 -> 3, 2 -> (nothing)
        ComposeStartStep step1 = createStep(id1, id3, "First Chain Start");
        ComposeStartStep step2 = createStep(id2, null, "Second Chain Start");
        ComposeStartStep step3 = createStep(id3, null, "First Chain End");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.findFirstStep(List.of(step1, step2, step3)));
    }

    // ==================== toOrderedList() tests ====================

    @Test
    void toOrderedList_shouldReturnEmptyList_whenInputEmpty() {
        assertEquals(List.of(), StepLinkedListUtils.toOrderedList(Collections.emptyList()));
        assertEquals(List.of(), StepLinkedListUtils.toOrderedList(null));
    }

    @Test
    void toOrderedList_shouldReturnSingleStep() {
        UUID stepId = UUID.randomUUID();
        ComposeStartStep step = createStep(stepId, null, "Only Step");

        List<AgentAppStep> result = StepLinkedListUtils.toOrderedList(List.of(step));

        assertEquals(1, result.size());
        assertEquals(stepId, result.get(0).getId());
    }

    @Test
    void toOrderedList_shouldOrderStepsCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2 -> 3
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        // Provide in reverse order
        List<AgentAppStep> result = StepLinkedListUtils.toOrderedList(List.of(step3, step2, step1));

        assertEquals(3, result.size());
        assertEquals(id1, result.get(0).getId());
        assertEquals(id2, result.get(1).getId());
        assertEquals(id3, result.get(2).getId());
    }

    @Test
    void toOrderedList_shouldThrow_whenBrokenChain() {
        UUID id1 = UUID.randomUUID();
        UUID nonExistentId = UUID.randomUUID();

        // Chain: 1 -> nonExistent (broken). Only one head candidate, so the chain walk is what fails.
        ComposeStartStep step1 = createStep(id1, nonExistentId, "First");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1)));
        assertEquals("Broken chain - step not found: " + nonExistentId, exception.getMessage());
    }

    @Test
    void toOrderedList_shouldThrow_whenMultipleHeadCandidates() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, null, "First");
        ComposeStartStep step2 = createStep(id2, null, "Also first");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2)));
        assertTrue(exception.getMessage().startsWith("Multiple first steps found"));
    }

    @Test
    void toOrderedList_shouldThrow_whenOrphanedSteps() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        // Chain: 1 -> 2, plus a detached 3 <-> 4 cycle, so 1 is still the only head candidate
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");
        ComposeStartStep step3 = createStep(id3, id4, "Orphan");
        ComposeStartStep step4 = createStep(id4, id3, "Orphan next");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2, step3, step4)));
        assertEquals("Orphaned steps detected. Expected 4 steps but chain contains 2", exception.getMessage());
    }

    @Test
    void toOrderedList_shouldThrow_whenChainLoopsBackInsideTheWalk() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // 1 -> 2 -> 3 -> 2: 1 is still an unreferenced head, so the walk reaches the loop
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, id2, "Third");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2, step3)));
        assertEquals("Circular reference detected at step: " + id2, exception.getMessage());
    }

    @Test
    void toOrderedList_shouldThrow_whenEveryStepIsReferenced() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Fully circular: 1 -> 2 -> 3 -> 1, so there is no head candidate at all
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, id1, "Third");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2, step3)));
        assertEquals("No first step found - possible circular reference", exception.getMessage());
    }

    // ==================== validate() tests ====================

    @Test
    void validate_shouldPassForEmptyList() {
        assertDoesNotThrow(() -> StepLinkedListUtils.validate(Collections.emptyList()));
        assertDoesNotThrow(() -> StepLinkedListUtils.validate(null));
    }

    @Test
    void validate_shouldPassForValidChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");

        assertDoesNotThrow(() -> StepLinkedListUtils.validate(List.of(step1, step2)));
    }

    @Test
    void validate_shouldThrow_whenStepHasNullId() {
        ComposeStartStep step = new ComposeStartStep();
        step.setTitle("No ID");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.validate(List.of(step)));

        assertTrue(ex.getMessage().contains("null id"));
    }

    @Test
    void validate_shouldThrow_whenNextIdReferencesNonExistent() {
        UUID id1 = UUID.randomUUID();
        UUID nonExistent = UUID.randomUUID();

        ComposeStartStep step = createStep(id1, nonExistent, "Broken");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.validate(List.of(step)));

        assertTrue(ex.getMessage().contains("non-existent nextId"));
    }

    // ==================== getNextStep() tests ====================

    @Test
    void getNextStep_shouldReturnEmpty_whenCurrentStepIdIsNull() {
        UUID id1 = UUID.randomUUID();
        ComposeStartStep step = createStep(id1, null, "Step");
        assertTrue(StepLinkedListUtils.getNextStep(null, List.of(step)).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnEmpty_whenStepsIsEmpty() {
        assertTrue(StepLinkedListUtils.getNextStep(UUID.randomUUID(), Collections.emptyList()).isEmpty());
        assertTrue(StepLinkedListUtils.getNextStep(UUID.randomUUID(), null).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnEmpty_whenCurrentStepIsLast() {
        UUID id1 = UUID.randomUUID();
        ComposeStartStep step = createStep(id1, null, "Last");
        assertTrue(StepLinkedListUtils.getNextStep(id1, List.of(step)).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnNextStep_inChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        List<AgentAppStep> steps = List.of(step1, step2, step3);

        AgentAppStep next = StepLinkedListUtils.getNextStep(id1, steps).orElse(null);
        assertNotNull(next);
        assertEquals(id2, next.getId());

        next = StepLinkedListUtils.getNextStep(id2, steps).orElse(null);
        assertNotNull(next);
        assertEquals(id3, next.getId());

        assertTrue(StepLinkedListUtils.getNextStep(id3, steps).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnEmpty_whenCurrentStepIdNotFound() {
        UUID id1 = UUID.randomUUID();
        ComposeStartStep step = createStep(id1, null, "Step");
        assertTrue(StepLinkedListUtils.getNextStep(UUID.randomUUID(), List.of(step)).isEmpty());
    }

    // ==================== findByStepId() tests ====================

    @Test
    void findByStepId_shouldReturnNull_whenInputEmptyOrIdNull() {
        ComposeStartStep step = createStep(UUID.randomUUID(), null, "Step");
        assertNull(StepLinkedListUtils.findByStepId(null, step.getId()));
        assertNull(StepLinkedListUtils.findByStepId(Collections.emptyList(), step.getId()));
        assertNull(StepLinkedListUtils.findByStepId(List.of(step), null));
    }

    @Test
    void findByStepId_shouldReturnMatchingStep() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");

        assertEquals(id2, StepLinkedListUtils.findByStepId(List.of(step1, step2), id2).getId());
        assertNull(StepLinkedListUtils.findByStepId(List.of(step1, step2), UUID.randomUUID()));
    }

    // ==================== getByType() tests ====================

    @Test
    void getByType_shouldReturnEmpty_whenInputEmptyOrTypeNotPresent() {
        assertTrue(StepLinkedListUtils.getByType(AgentAppStepType.COMPOSE_START, ComposeStartStep.class, null).isEmpty());
        assertTrue(StepLinkedListUtils.getByType(AgentAppStepType.COMPOSE_START, ComposeStartStep.class, Collections.emptyList()).isEmpty());
        assertTrue(StepLinkedListUtils.getByType(AgentAppStepType.COMPOSE_DOWN, ComposeStartStep.class,
                List.of(createStep(UUID.randomUUID(), null, "Step"))).isEmpty());
    }

    @Test
    void getByType_shouldReturnFirstMatchOfRequestedClass() {
        ComposeStartStep first = createStep(UUID.randomUUID(), null, "First");
        ComposeStartStep second = createStep(UUID.randomUUID(), null, "Second");

        assertEquals(first.getId(), StepLinkedListUtils
                .getByType(AgentAppStepType.COMPOSE_START, ComposeStartStep.class, List.of(first, second))
                .orElseThrow().getId());
    }

    // ==================== filter() tests ====================

    @Test
    void filter_shouldKeepOnlyMatchingSteps() {
        ComposeStartStep kept = createStep(UUID.randomUUID(), null, "Keep");
        ComposeStartStep dropped = createStep(UUID.randomUUID(), null, "Drop");

        List<AgentAppStep> result = StepLinkedListUtils.filter(List.of(kept, dropped), s -> "Keep".equals(s.getTitle()));

        assertEquals(1, result.size());
        assertEquals(kept.getId(), result.get(0).getId());
    }

    // ==================== removeAndRestitch() tests ====================

    @Test
    void removeAndRestitch_shouldReturnEmptyList_whenInputNullOrEmpty() {
        assertEquals(List.of(), StepLinkedListUtils.removeAndRestitch(null, s -> true));
        assertEquals(List.of(), StepLinkedListUtils.removeAndRestitch(Collections.emptyList(), s -> true));
    }

    @Test
    void removeAndRestitch_shouldReturnEmptyList_whenSingleElementRemoved() {
        ComposeStartStep only = createStep(UUID.randomUUID(), null, "Only");

        assertEquals(List.of(), StepLinkedListUtils.removeAndRestitch(newList(only), s -> true));
    }

    @Test
    void removeAndRestitch_shouldKeepChain_whenHeadRemoved() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        List<AgentAppStep> result = StepLinkedListUtils.removeAndRestitch(
                newList(step1, step2, step3), s -> id1.equals(s.getId()));

        assertEquals(List.of(id2, id3), orderedIds(result));
    }

    @Test
    void removeAndRestitch_shouldKeepChain_whenTailRemoved() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        List<AgentAppStep> result = StepLinkedListUtils.removeAndRestitch(
                newList(step1, step2, step3), s -> id3.equals(s.getId()));

        assertEquals(List.of(id1, id2), orderedIds(result));
        assertNull(StepLinkedListUtils.findByStepId(result, id2).getNextId());
    }

    @Test
    void removeAndRestitch_shouldKeepChain_whenMidChainRemoved() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        List<AgentAppStep> result = StepLinkedListUtils.removeAndRestitch(
                newList(step1, step2, step3), s -> id2.equals(s.getId()));

        assertEquals(List.of(id1, id3), orderedIds(result));
        assertEquals(id3, StepLinkedListUtils.findByStepId(result, id1).getNextId());
    }

    @Test
    void removeAndRestitch_shouldKeepChain_whenTwoConsecutiveStepsRemoved() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, id4, "Third");
        ComposeStartStep step4 = createStep(id4, null, "Fourth");

        List<AgentAppStep> result = StepLinkedListUtils.removeAndRestitch(
                newList(step1, step2, step3, step4), s -> id2.equals(s.getId()) || id3.equals(s.getId()));

        assertEquals(List.of(id1, id4), orderedIds(result));
        assertEquals(id4, StepLinkedListUtils.findByStepId(result, id1).getNextId());
    }

    @Test
    void removeAndRestitch_shouldReturnEmptyList_whenEveryStepRemoved() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");

        assertEquals(List.of(), StepLinkedListUtils.removeAndRestitch(newList(step1, step2), s -> true));
    }

    @Test
    void removeAndRestitch_shouldDropStepWithNullId_withoutBreakingChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");
        ComposeStartStep orphan = createStep(null, null, "Orphan");

        List<AgentAppStep> result = StepLinkedListUtils.removeAndRestitch(
                newList(step1, step2, orphan), s -> s.getId() == null);

        assertEquals(List.of(id1, id2), orderedIds(result));
    }

    // ==================== Helper methods ====================

    private static List<AgentAppStep> newList(AgentAppStep... steps) {
        return new ArrayList<>(List.of(steps));
    }

    private static List<UUID> orderedIds(List<AgentAppStep> steps) {
        return StepLinkedListUtils.toOrderedList(steps).stream().map(AgentAppStep::getId).toList();
    }

    private ComposeStartStep createStep(UUID id, UUID nextId, String title) {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(id);
        step.setNextId(nextId);
        step.setTitle(title);
        return step;
    }
}

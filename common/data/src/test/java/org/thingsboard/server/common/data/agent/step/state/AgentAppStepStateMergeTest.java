// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step.state;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.AgentAppEventId;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAppStepStateMergeTest {

    @Test
    void overlayValueWinsWhenPresent() {
        ComposeStepState template = compose(true, false);
        ComposeStepState overlay = compose(false, true);
        assertThat(template.getCommandMetadata(overlay)).containsEntry("pullImages", "false");
    }

    @Test
    void fallsBackToTemplateWhenNoOverlay() {
        ComposeStepState template = compose(true, false);
        assertThat(template.getCommandMetadata(null)).containsEntry("pullImages", "true");
    }

    @Test
    void fallsBackToTemplateWhenOverlayOmitsFieldValue() {
        ComposeStepState template = compose(true, true);
        ComposeStepState overlay = new ComposeStepState();
        overlay.setPullImages(new StepField<>(null, true)); // present but no value
        assertThat(template.getCommandMetadata(overlay)).containsEntry("pullImages", "true");
    }

    @Test
    void rollbackTakesServerInjectedValueOverNullTemplate() {
        RollBackStepState template = new RollBackStepState();
        template.setFailedEventId(new StepField<>(null, false)); // template placeholder, userChoice:false
        AgentAppEventId injected = new AgentAppEventId(UUID.randomUUID());
        RollBackStepState overlay = new RollBackStepState(injected);

        assertThat(template.getCommandMetadata(overlay))
                .containsEntry("failedCommandId", injected.getId().toString());
        assertThat(template.getCommandMetadata(null)).isEmpty();
    }

    @Test
    void backupVolumesMergePerField() {
        BackupVolumesStepState template = new BackupVolumesStepState();
        template.setBackupVolumes(new StepField<>(List.of(), true));
        BackupVolumesStepState overlay = new BackupVolumesStepState();
        overlay.setBackupVolumes(new StepField<>(List.of("a", "b"), true));

        assertThat(template.getCommandMetadata(overlay)).containsEntry("backupVolumes", "a,b");
        assertThat(template.getCommandMetadata(null)).containsEntry("backupVolumes", "");
    }

    private static ComposeStepState compose(boolean value, boolean userChoice) {
        ComposeStepState state = new ComposeStepState();
        state.setPullImages(new StepField<>(value, userChoice));
        return state;
    }
}

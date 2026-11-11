// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step.state;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.exception.DataValidationException;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BackupVolumesStepState extends AgentAppStepState {

    public static final String BACKUP_VOLUMES = "backupVolumes";

    private StepField<List<String>> backupVolumes;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.BACKUP_VOLUME;
    }

    @Override
    public void validate() throws DataValidationException {
        if (backupVolumes == null || backupVolumes.getValue() == null) {
            throw new DataValidationException("Validation error: " + BACKUP_VOLUMES + " must not be null");
        }
    }

    @Override
    protected @NonNull Map<String, StepField<?>> fields() {
        return Collections.singletonMap(BACKUP_VOLUMES, backupVolumes);
    }

    @Override
    public Map<String, String> getCommandMetadata(@Nullable AgentAppStepState overlay) {
        List<String> volumes = effectiveValue(BACKUP_VOLUMES, overlay);
        return Map.of(BACKUP_VOLUMES, volumes != null ? String.join(",", volumes) : "");
    }
}

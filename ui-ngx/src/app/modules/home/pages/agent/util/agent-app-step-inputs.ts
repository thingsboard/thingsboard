// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AgentApplication, AgentAppStep } from '@shared/models/agent.models';
import {
  buildBackupVolumeInput,
  buildComposeDownInput,
  buildPullImagesInput,
  ClassifiedStep,
  extractComposeVolumeKeys,
  readInitialPullImages,
  StepInputKind
} from '@home/pages/agent/util/agent-app-steps';

export interface VolumeChoice {
  key: string;
  selected: boolean;
}

export interface StepBinding {
  kind: StepInputKind;
  step: AgentAppStep;
  backupVolumes?: VolumeChoice[];
  pullImages?: boolean;
  removeVolumes?: boolean;
}

export function seedBackupVolumes(backupSource: AgentApplication | null): VolumeChoice[] {
  if (!backupSource) {
    return [];
  }
  return extractComposeVolumeKeys(backupSource).map(key => ({ key, selected: true }));
}

export function createStepBinding({ kind, step }: ClassifiedStep, backupSource: AgentApplication | null): StepBinding {
  switch (kind) {
    case 'backupVolume':
      return { kind, step, backupVolumes: seedBackupVolumes(backupSource) };
    case 'pullImages':
      return { kind, step, pullImages: readInitialPullImages(step) };
    case 'composeDown':
      return { kind, step, removeVolumes: false };
  }
}

export function buildStepPayload(b: StepBinding): any {
  switch (b.kind) {
    case 'backupVolume':
      return buildBackupVolumeInput(
        b.step,
        (b.backupVolumes || []).filter(v => v.selected).map(v => v.key)
      );
    case 'pullImages':
      return buildPullImagesInput(b.step, !!b.pullImages);
    case 'composeDown':
      return buildComposeDownInput(b.step, !!b.removeVolumes);
  }
}

export function buildStepInputs(bindings: StepBinding[]): { [stepId: string]: any } {
  const out: { [stepId: string]: any } = {};
  for (const b of bindings) {
    out[b.step.id] = buildStepPayload(b);
  }
  return out;
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AgentAppConfig,
  AgentAppEventActionType,
  AgentAppStep,
  AgentAppStepState,
  AgentAppStepType,
  AgentAppTemplate,
  dockerComposeConfig
} from '@shared/models/agent.models';


// A step field is user-facing only when it is present AND declares userChoice === true.
// userChoice === false fields (e.g. ROLLBACK failedEventId) are applied silently and never rendered.
function isUserChoice(step: AgentAppStep, key: string): boolean {
  const field = step?.state?.[key];
  return !!field && field.userChoice === true;
}

function visibleSteps(steps: AgentAppStep[] | undefined): AgentAppStep[] {
  return (steps || []).filter(s => !s.templateOnly);
}

export function findComposeDownStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.deleteSteps).find(s =>
    s.type === AgentAppStepType.COMPOSE_DOWN && isUserChoice(s, 'removeVolumes')
  ) || null;
}

export function readInitialPullImages(step: AgentAppStep | null | undefined): boolean {
  return !!step?.state?.pullImages?.value;
}

export function extractComposeVolumeKeys(source: { config?: AgentAppConfig } | null | undefined): string[] {
  const compose = dockerComposeConfig(source)?.compose;
  if (!compose || !compose.volumes || typeof compose.volumes !== 'object') {
    return [];
  }
  return Object.keys(compose.volumes);
}

export function buildBackupVolumeInput(step: AgentAppStep, selectedKeys: string[]): AgentAppStepState {
  return {
    backupVolumes: { value: selectedKeys, userChoice: true },
    type: AgentAppStepType.BACKUP_VOLUME
  } as AgentAppStepState;
}

export function buildPullImagesInput(step: AgentAppStep, pullImages: boolean): AgentAppStepState {
  return {
    pullImages: { value: pullImages, userChoice: true },
    type: step.type
  } as AgentAppStepState;
}

export function buildComposeDownInput(step: AgentAppStep, removeVolumes: boolean): AgentAppStepState {
  return {
    removeVolumes: { value: removeVolumes, userChoice: true },
    type: AgentAppStepType.COMPOSE_DOWN
  } as AgentAppStepState;
}

/**
 * Kinds of user-facing step inputs the FE knows how to render + collect.
 * The classifier maps an AgentAppStep → kind (or null if no user input).
 * Rendering and payload-building stay per-kind; what varies per template
 * is which steps of which kinds show up in which list — classification
 * is structural (step type + state shape), not positional.
 */
export type StepInputKind = 'backupVolume' | 'pullImages' | 'composeDown';

export interface ClassifiedStep {
  kind: StepInputKind;
  step: AgentAppStep;
}

// A step is classifiable as a user-facing input only when its state declares
// the field with userChoice === true. A COMPOSE_DOWN without such a field,
// for example, means the server runs compose-down with the template value — no prompt needed.
export function classifyStep(step: AgentAppStep): StepInputKind | null {
  switch (step.type) {
    case AgentAppStepType.BACKUP_VOLUME:
      return isUserChoice(step, 'backupVolumes') ? 'backupVolume' : null;
    case AgentAppStepType.COMPOSE_DOWN:
      return isUserChoice(step, 'removeVolumes') ? 'composeDown' : null;
    case AgentAppStepType.COMPOSE:
      return isUserChoice(step, 'pullImages') ? 'pullImages' : null;
    default:
      return null;
  }
}

/**
 * The template step-list the BE consults for a given action. UPDATE reuses
 * startSteps (mirroring the single-app wizard's update mode).
 */
export function stepsForAction(
  template: AgentAppTemplate | null | undefined,
  action: AgentAppEventActionType
): AgentAppStep[] {
  if (!template) { return []; }
  switch (action) {
    case AgentAppEventActionType.INSTALL:
    case AgentAppEventActionType.UPDATE:
      return template.startSteps || [];
    case AgentAppEventActionType.UPGRADE:
      return template.upgradeSteps || [];
    case AgentAppEventActionType.DELETE:
      return template.deleteSteps || [];
    case AgentAppEventActionType.RESTART:
      return template.restartSteps || [];
    case AgentAppEventActionType.ROLLBACK:
      return template.rollbackSteps || [];
    default:
      return [];
  }
}

/**
 * The action's steps in execution order, without template-only steps. Steps are
 * linked via `nextId`: the head is the one no other step points at, and the chain
 * is walked from there to match the BE order. Falls back to the declared order
 * when the chain is broken.
 */
export function orderedStepsForAction(
  template: AgentAppTemplate | null | undefined,
  action: AgentAppEventActionType
): AgentAppStep[] {
  return orderByNextId(visibleSteps(stepsForAction(template, action)));
}

function orderByNextId(steps: AgentAppStep[]): AgentAppStep[] {
  if (!steps.length) { return []; }
  const byId = new Map(steps.map(s => [s.id, s]));
  const referenced = new Set(steps.map(s => s.nextId).filter(Boolean) as string[]);
  const head = steps.find(s => !referenced.has(s.id));
  if (!head) {
    return steps;
  }
  const ordered: AgentAppStep[] = [];
  let cursor: AgentAppStep | undefined = head;
  const seen = new Set<string>();
  while (cursor && !seen.has(cursor.id)) {
    ordered.push(cursor);
    seen.add(cursor.id);
    cursor = cursor.nextId ? byId.get(cursor.nextId) : undefined;
  }
  return ordered;
}

export function classifyStepsForAction(
  template: AgentAppTemplate | null | undefined,
  action: AgentAppEventActionType
): ClassifiedStep[] {
  const out: ClassifiedStep[] = [];
  for (const step of visibleSteps(stepsForAction(template, action))) {
    const kind = classifyStep(step);
    if (kind) {
      out.push({ kind, step });
    }
  }
  return out;
}

export function actionUsesTemplate(action: AgentAppEventActionType): boolean {
  switch (action) {
    case AgentAppEventActionType.INSTALL:
    case AgentAppEventActionType.UPDATE:
    case AgentAppEventActionType.UPGRADE:
    case AgentAppEventActionType.DELETE:
      return true;
    default:
      return false;
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import * as YAML from 'yaml';
import {
  AgentAppConfig,
  AgentApplicationType,
  AgentAppStepType,
  AgentAppTemplate,
  DockerComposeConfig,
  dockerComposeConfig
} from '@shared/models/agent.models';

// Emitter options tuned to match the previous hand-rolled output: two-space
// indent and no line folding (compose command/env strings stay on one line).
// The `yaml` lib handles the quoting/escaping rules the old heuristic missed
// (leading indicators, leading/trailing whitespace, etc.), keeping the
// parse -> stringify round-trip valid.
const DUMP_YAML_OPTIONS: YAML.ToStringOptions = {
  indent: 2,
  lineWidth: 0
};

export function dumpYaml(value: any, indent = 0): string {
  return YAML.stringify(value, DUMP_YAML_OPTIONS);
}

export function dumpCompose(source: { config?: AgentAppConfig } | null | undefined): string {
  const compose: any = (source?.config as DockerComposeConfig)?.compose;
  if (!compose) {
    return '';
  }
  return dumpYaml(compose, 0).trimEnd() + '\n';
}

const COMPOSE_TYPE_LABEL_KEYS: Record<string, string> = {
  kafka: 'agent.compose-type-kafka',
  hybrid: 'agent.compose-type-hybrid',
  in_memory: 'agent.compose-type-in-memory'
};

// Returns the locale key for a known compose type, or null when the raw key
// should be shown as-is.
export function composeTypeLabelKey(composeType: string): string | null {
  return COMPOSE_TYPE_LABEL_KEYS[(composeType || '').toLowerCase()] || null;
}

function findComposeTemplates(template: AgentAppTemplate): Record<string, any> | null {
  for (const step of (template.startSteps || [])) {
    if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && step.composeTemplates) {
      return step.composeTemplates;
    }
  }
  return null;
}

export function composeTemplateKeys(template: AgentAppTemplate | null | undefined): string[] {
  if (!template) {
    return [];
  }
  const templates = findComposeTemplates(template);
  return templates ? Object.keys(templates) : [];
}

export function dumpRawTemplateCompose(template: AgentAppTemplate, composeType?: string): string {
  const templates = findComposeTemplates(template);
  if (templates) {
    const keys = Object.keys(templates);
    if (keys.length) {
      const key = (composeType && keys.includes(composeType)) ? composeType : keys[0];
      return dumpYaml(templates[key], 0).trimEnd() + '\n';
    }
  }
  return '';
}

type ComposeTypePicker = (keys: string[]) => string;

const firstKeyPicker: ComposeTypePicker = keys => keys[0];

const preferredKeyPicker = (preferred: string): ComposeTypePicker =>
  keys => keys.includes(preferred) ? preferred : firstKeyPicker(keys);

const COMPOSE_TYPE_PICKERS = new Map<AgentApplicationType, ComposeTypePicker>([
  [AgentApplicationType.EDGE, preferredKeyPicker('kafka')]
]);

export function pickComposeType(template: AgentAppTemplate): string {
  const keys = composeTemplateKeys(template);
  if (!keys.length) {
    return 'default';
  }
  const picker = COMPOSE_TYPE_PICKERS.get(template.appType) ?? firstKeyPicker;
  return picker(keys);
}

export function parseComposeYaml(yaml: string, fallbackCompose?: any): any {
  if (yaml && yaml.trim().length > 0) {
    try {
      return YAML.parse(yaml);
    } catch (e) { /* fall through */ }
  }
  if (fallbackCompose) {
    return fallbackCompose;
  }
  return { services: {} };
}

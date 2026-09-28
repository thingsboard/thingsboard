// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AgentApplication,
  AgentApplicationOrigin,
  AgentApplicationType,
  AgentAppTemplate,
  AgentAppConfigType,
  DockerComposeConfig
} from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';
import { parseComposeYaml } from '@home/pages/agent/util/agent-compose-yaml';

// Minimal INSTALLED draft posted to the merge-preview endpoint for a fresh
// install before the user has typed any compose.
export function buildInstallMergeDraft(agentId: string,
                                       selectedType: AgentApplicationType,
                                       appName: string,
                                       template: AgentAppTemplate): AgentApplication {
  return {
    name: appName,
    appType: selectedType,
    agentId: new AgentId(agentId),
    templateVersion: template.currentVersion,
    origin: AgentApplicationOrigin.INSTALLED
  };
}

// Update mode re-uses the existing application, only re-pointing the template.
export function buildUpdateMergeDraft(existingApplication: AgentApplication,
                                      template: AgentAppTemplate): AgentApplication {
  return { ...existingApplication, templateVersion: template.currentVersion };
}

// Draft posted when re-merging after a related entity is picked (install mode):
// carries the user's current compose so host/credential overrides survive.
export function buildRelatedMergeDraft(agentId: string,
                                       selectedType: AgentApplicationType,
                                       appName: string,
                                       template: AgentAppTemplate,
                                       composeYaml: string,
                                       fallbackCompose: any): AgentApplication {
  return {
    name: appName,
    appType: selectedType,
    agentId: new AgentId(agentId),
    templateVersion: template.currentVersion,
    origin: AgentApplicationOrigin.INSTALLED,
    config: { type: AgentAppConfigType.DOCKER_COMPOSE, compose: parseComposeYaml(composeYaml, fallbackCompose) } as DockerComposeConfig
  };
}

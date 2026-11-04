// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';
import {
  AgentAppProfile,
  agentApplicationTypeTranslationMap,
  AgentApplicationType,
  AgentAppTemplate,
  VirtualAgentAppProfile
} from '@shared/models/agent.models';
import { orderTemplatesNewestFirst } from '@home/pages/agent/util/template-version-order';
import { pickComposeType } from '@home/pages/agent/util/agent-compose-yaml';

// App types that get virtual "Predefined" picker entries; GENERIC profiles are always user-created.
export const VIRTUAL_APP_TYPES = [AgentApplicationType.EDGE, AgentApplicationType.GATEWAY];

// Display-only label for a virtual entry; the materialized profile's actual name comes from
// the materialize response (the server generates it independently).
export function virtualProfileName(template: AgentAppTemplate): string {
  const type = template.appType.charAt(0) + template.appType.slice(1).toLowerCase();
  return `${type} ${template.currentVersion}`;
}

// "Edge / 4.3.1.2EDGEPE" suffix shown next to a real profile in the pickers.
export function appProfileTemplateLabel(profile: AgentAppProfile, translate: TranslateService): string {
  const typeKey = agentApplicationTypeTranslationMap.get(profile.appType);
  const type = typeKey ? translate.instant(typeKey) : profile.appType;
  return `${type} / ${profile.templateVersion}`;
}

export function appProfileSearchMatches(profile: AgentAppProfile, searchText: string): boolean {
  const lc = searchText.toLowerCase();
  return profile.name.toLowerCase().includes(lc)
    || (profile.templateVersion || '').toLowerCase().includes(lc);
}

// One placeholder entry per template version the tenant has no profile for yet
// (a profile with matching templateVersion occupies that version's slot), newest first.
export function buildVirtualAppProfiles(templates: AgentAppTemplate[],
                                        existingProfiles: AgentAppProfile[]): VirtualAgentAppProfile[] {
  const takenVersions = new Set(existingProfiles.map(p => p.templateVersion).filter(v => !!v));
  return orderTemplatesNewestFirst(templates || [])
    .filter(t => !!t.currentVersion && !takenVersions.has(t.currentVersion))
    .map(t => ({
      id: null,
      virtual: true,
      name: virtualProfileName(t),
      appType: t.appType,
      templateVersion: t.currentVersion,
      defaultComposeType: pickComposeType(t)
    } as VirtualAgentAppProfile));
}

// Stable @for key for picker lists that mix real and virtual entries: virtual entries have no id.
export function appProfileTrackKey(profile: AgentAppProfile): string {
  return profile.id?.id ?? `virtual:${profile.appType}:${profile.templateVersion}`;
}

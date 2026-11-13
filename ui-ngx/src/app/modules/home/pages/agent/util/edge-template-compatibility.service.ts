// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AgentAppProfile, AgentApplicationType, AgentAppTemplate } from '@shared/models/agent.models';

// Edge releases older than 4.3 don't know the license version sent by the server: they always require an Edge
// license key and a cloud endpoint, which an add-on Edge never carries, and terminate right after connecting.
export const MIN_ADD_ON_EDGE_VERSION = [4, 3];

export function isEdgeVersionSupported(appType: AgentApplicationType, version: string | undefined | null,
                                       addOnEdge: boolean): boolean {
  if (appType !== AgentApplicationType.EDGE || !addOnEdge) {
    return true;
  }
  const match = (version || '').match(/^\d+(?:\.\d+)*/);
  const segments = match ? match[0].split('.').map(seg => parseInt(seg, 10)) : [];
  for (let i = 0; i < MIN_ADD_ON_EDGE_VERSION.length; i++) {
    const segment = segments[i] ?? 0;
    if (segment !== MIN_ADD_ON_EDGE_VERSION[i]) {
      return segment > MIN_ADD_ON_EDGE_VERSION[i];
    }
  }
  return true;
}

// Hides EDGE template versions an add-on Edge cannot run from every template/profile picker.
@Injectable({ providedIn: 'root' })
export class EdgeTemplateCompatibilityService {

  constructor(private store: Store<AppState>) {}

  // The license version is instance-wide: with a v2 license every Edge is an add-on Edge.
  isAddOnEdgeByDefault(): boolean {
    return getCurrentAuthState(this.store).licenseVersion > 1;
  }

  filterTemplates(templates: AgentAppTemplate[], addOnEdge = this.isAddOnEdgeByDefault()): AgentAppTemplate[] {
    return (templates || []).filter(t => isEdgeVersionSupported(t.appType, t.currentVersion, addOnEdge));
  }

  filterProfiles<T extends AgentAppProfile>(profiles: T[], addOnEdge = this.isAddOnEdgeByDefault()): T[] {
    return (profiles || []).filter(p => isEdgeVersionSupported(p.appType, p.templateVersion, addOnEdge));
  }

}

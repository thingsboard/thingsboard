// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppConfigType,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  VirtualAgentAppProfile
} from '@shared/models/agent.models';

export interface UpgradeTemplateResult {
  template: AgentAppTemplate;
  // Template of the app's current version — its upgradeSteps describe the
  // current -> next hop and are what the backend validates step inputs against.
  sourceTemplate: AgentAppTemplate;
  // Resolved from the linked template's currentVersion; null on the
  // desiredTemplateVersion path where ngOnInit's seeded value must be kept.
  fromVersion: string | null;
}

// Thrown by resolveUpgradeTemplate for known, non-HTTP failures so the caller
// can surface the matching translated message instead of a generic error.
export interface UpgradeResolveError {
  messageKey: string;
}

// Per-wizard-instance loader: owns the template/profile caches and wraps the
// AgentService IO the install/update/upgrade flows perform. Provided at the
// dispatcher level so caches are shared across the active flow but don't leak
// across separate wizard openings.
@Injectable()
export class AgentAppWizardLoaderService {

  private templateCache = new Map<AgentApplicationType, AgentAppTemplate>();
  private templateByVersionCache = new Map<string, AgentAppTemplate>();
  private templatesByTypeCache = new Map<AgentApplicationType, AgentAppTemplate[]>();
  private profilesCache = new Map<AgentApplicationType, AgentAppProfile[]>();

  constructor(private agentService: AgentService) {}

  // Warm the per-type template cache so the first type-card click in install
  // mode doesn't flash a "Loading template…" state.
  prewarmTemplates(types: AgentApplicationType[]): void {
    types.forEach(type => {
      if (this.templateCache.has(type)) {
        return;
      }
      this.agentService.getLatestAgentAppTemplate(type, AgentAppConfigType.DOCKER_COMPOSE).subscribe({
        next: tpl => this.templateCache.set(type, tpl),
        error: () => { /* swallow — loadTemplate retries on demand */ }
      });
    });
  }

  loadTemplate(type: AgentApplicationType): Observable<AgentAppTemplate> {
    const cached = this.templateCache.get(type);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getLatestAgentAppTemplate(type, AgentAppConfigType.DOCKER_COMPOSE).pipe(
      tap(tpl => this.templateCache.set(type, tpl))
    );
  }

  loadTemplateByVersion(appType: AgentApplicationType, version: string,
                        configType: string = AgentAppConfigType.DOCKER_COMPOSE): Observable<AgentAppTemplate> {
    const cacheKey = `${appType}:${configType}:${version}`;
    const cached = this.templateByVersionCache.get(cacheKey);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getAgentAppTemplateByVersion(appType, configType, version).pipe(
      tap(tpl => this.templateByVersionCache.set(cacheKey, tpl))
    );
  }

  loadTemplatesByType(type: AgentApplicationType): Observable<AgentAppTemplate[]> {
    const cached = this.templatesByTypeCache.get(type);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getAgentAppTemplatesByAppType(type).pipe(
      tap(templates => this.templatesByTypeCache.set(type, templates))
    );
  }

  materializeProfile(profile: VirtualAgentAppProfile): Observable<AgentAppProfile> {
    return this.agentService.materializeAgentAppProfile(profile.appType, profile.templateVersion, profile.defaultComposeType);
  }

  loadProfiles(type: AgentApplicationType): Observable<AgentAppProfile[]> {
    const cached = this.profilesCache.get(type);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getAgentAppProfilesByAppType(type).pipe(
      tap(profiles => this.profilesCache.set(type, profiles))
    ) as Observable<AgentAppProfile[]>;
  }

  cacheProfiles(type: AgentApplicationType, profiles: AgentAppProfile[]): void {
    this.profilesCache.set(type, profiles);
  }

  // Reads the linked profile via the info-by-id endpoint, which is gated by
  // tenant ownership only (no per-entity AGENT_APP_PROFILE permission check),
  // so roles with AGENT but not AGENT_APP_PROFILE access can still load it.
  loadProfileById(id: string): Observable<AgentAppProfile> {
    return this.agentService.getAgentAppProfileInfoById(id);
  }

  merge(templateVersion: string, appType: AgentApplicationType, draft: AgentApplication, composeType?: string,
        relatedEntityId?: EntityId, actionType?: AgentAppEventActionType,
        setHostValues?: boolean): Observable<AgentApplication> {
    return this.agentService.mergeForPreview(templateVersion, appType, draft, composeType, relatedEntityId, actionType, setHostValues);
  }

  loadManagedApp(entityType: string, entityId: string): Observable<AgentApplication | null> {
    return this.agentService.getAgentApplicationByRelatedEntity(
      entityType, entityId, { ignoreErrors: true, ignoreLoading: true }
    ).pipe(map(app => app || null));
  }

  // Resolves the single-hop upgrade target. Follows the template's nextVersion
  // pointer rather than jumping to "latest" so intermediate versions (and their
  // upgradeSteps / migrations) aren't skipped on multi-hop chains.
  resolveUpgradeTemplate(app: AgentApplication): Observable<UpgradeTemplateResult> {
    if (!app.templateVersion) {
      return throwError(() => ({ messageKey: 'agent.app-upgrade-no-template' } as UpgradeResolveError));
    }
    return this.agentService.getAgentAppTemplateByVersion(app.appType, AgentAppConfigType.DOCKER_COMPOSE, app.templateVersion).pipe(
      mergeMap(current => {
        const desiredVersion = app.desiredTemplateVersion;
        if (desiredVersion) {
          return this.loadTemplateByVersion(app.appType, desiredVersion).pipe(
            map(template => ({ template, sourceTemplate: current, fromVersion: null }))
          );
        }
        if (!current.nextVersion) {
          return throwError(() => ({ messageKey: 'agent.app-upgrade-no-next-version' } as UpgradeResolveError));
        }
        const fromVersion = current.currentVersion || null;
        const configType = current.configType || AgentAppConfigType.DOCKER_COMPOSE;
        return this.agentService.getAgentAppTemplateByVersion(
          current.appType, configType, current.nextVersion
        ).pipe(map(template => ({ template, sourceTemplate: current, fromVersion })));
      })
    );
  }
}

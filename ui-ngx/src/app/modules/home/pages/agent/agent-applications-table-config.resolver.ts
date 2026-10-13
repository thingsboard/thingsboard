// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  agentEntityUrl,
  currentAgentRouteSnapshot,
  resolveAgentIdParam
} from '@home/pages/agent/util/agent-route-params';
import {
  CellActionDescriptor,
  EntityColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Observable, of } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { catchError, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  AgentApplication,
  AgentApplicationInfo,
  AgentApplicationOrigin,
  agentApplicationOriginTranslationMap,
  AgentApplicationSaveRequest,
  AgentAppConfigType,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppTemplate,
  AgentInfo
} from '@shared/models/agent.models';
import { findComposeDownStep } from '@home/pages/agent/util/agent-app-steps';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';
import { confirmUpdateDrift } from '@home/pages/agent/util/agent-version-warnings';
import { versionTag } from '@home/pages/agent/util/version-tag';
import { AgentService } from '@core/http/agent.service';
import { AgentApplicationComponent } from '@home/pages/agent/agent-application.component';
import { AgentApplicationTabsComponent } from '@home/pages/agent/agent-application-tabs.component';
import {
  AgentAppDeleteDialogComponent,
  AgentAppDeleteDialogData
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult,
  isAgentAppUpgradeResult
} from '@home/components/agent/wizard/agent-app-install-wizard.component';

@Injectable()
export class AgentApplicationsTableConfigResolver {

  private readonly config: EntityTableConfig<AgentApplicationInfo> = new EntityTableConfig<AgentApplicationInfo>();
  private agentId: string;
  private agent: AgentInfo;
  private templateCache = new Map<string, AgentAppTemplate>();

  constructor(private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private dialogService: DialogService) {

    this.config.entityType = EntityType.AGENT_APPLICATION;
    this.config.entityComponent = AgentApplicationComponent;
    this.config.entityTabsComponent = AgentApplicationTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_APPLICATION);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_APPLICATION);
    this.config.addEnabled = true;
    this.config.entitiesDeleteEnabled = false;
    this.config.selectionEnabled = false;
    this.config.rowPointer = true;
    this.config.loadEntity = id => this.agentService.getAgentApplicationInfoById(id.id);
    this.config.saveEntity = (request: AgentApplicationSaveRequest) => {
      const { relatedEntityIdNext, relatedEntityIdPrev, ...app } = request;
      return this.agentService.updateAgentApplication(app).pipe(
        mergeMap(saved => this.applyRelatedEntityChange(saved, relatedEntityIdPrev, relatedEntityIdNext)),
        mergeMap(saved => this.agentService.getAgentApplicationInfoById(saved.id.id)),
        tap(saved => this.showUpdateHintDialog(saved))
      );
    };
    this.config.addEntity = () => this.openInstallWizard();
    this.config.handleRowClick = ($event: Event, app) => {
      if ($event) { $event.stopPropagation(); }
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), this.agentId, 'applications', app.id.id));
      return true;
    };
    this.config.onEntityAction = (action) => {
      if (action.action === 'open') {
        this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), this.agentId, 'applications', action.entity.id.id));
        return true;
      }
      return false;
    };
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentApplicationInfo>> {
    this.agentId = resolveAgentIdParam(route);
    return this.agentService.getAgentInfoById(this.agentId).pipe(
      map(agent => {
        this.agent = agent;
        this.config.tableTitle = agent.name + ': ' + this.translate.instant('agent.applications');
        this.config.backNavigationCommands = [agentEntityUrl(route, this.agentId)];
        this.config.componentsData = { agentId: this.agentId, agent };
        this.config.columns = this.configureColumns();
        this.config.cellActionDescriptors = this.configureCellActions();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getAgentApplicationsByAgentId(this.agentId, pageLink).pipe(
            switchMap(page => this.enrichWithTemplates(page))
          );
        return this.config;
      })
    );
  }

  private applyRelatedEntityChange(saved: AgentApplication, prev: EntityId | null | undefined,
                                   next: EntityId | null | undefined): Observable<AgentApplication> {
    const prevId = prev?.id ?? null;
    const nextId = next?.id ?? null;
    if (prevId === nextId) {
      return of(saved);
    }
    if (nextId) {
      return this.agentService.assignRelatedEntity(saved.id.id, next);
    }
    return this.agentService.unassignRelatedEntity(saved.id.id);
  }

  private configureColumns(): Array<EntityColumn<AgentApplicationInfo>> {
    return [
      new EntityTableColumn<AgentApplicationInfo>('name', 'agent.app-name', '25%'),
      new EntityTableColumn<AgentApplicationInfo>('appType', 'agent.app-type', '120px',
        e => this.appTypeBadge(e.appType), () => ({}), false),
      new EntityLinkTableColumn<AgentApplicationInfo>('profileName', 'agent.app-profile', '20%',
        e => e.profileName || '—',
        e => e.applicationProfileId?.id
          ? `/edgeManagement/profiles/application/${e.applicationProfileId.id}`
          : '',
        false),
      new EntityTableColumn<AgentApplicationInfo>('currentVersion', 'agent.app-template', '20%',
        e => this.templateCell(e), () => ({}), false),
      new EntityTableColumn<AgentApplicationInfo>('origin', 'agent.app-origin', '140px',
        e => this.originBadge(e.origin), () => ({}), false),
      new EntityTableColumn<AgentApplicationInfo>('profileConfigOutdated', 'agent.app-profile-in-sync', '104px',
        e => this.profileSyncBadge(e), () => this.statusCellStyle, false, () => this.statusHeaderStyle,
        e => this.profileSyncTooltip(e)),
    ];
  }

  private readonly statusCellStyle = { justifyContent: 'center', textAlign: 'center' };
  private readonly statusHeaderStyle = {
    whiteSpace: 'normal', justifyContent: 'center', textAlign: 'center', lineHeight: '1.3'
  };

  private statusIcon(icon: string, color: string): string {
    return `<span style="display:inline-flex;align-items:center;justify-content:center;"><span class="material-icons" style="font-size:20px;color:${color};">${icon}</span></span>`;
  }

  private profileTemplateDrifted(e: AgentApplicationInfo): boolean {
    return !!e.profileTemplateVersion && !!e.templateVersion
      && e.profileTemplateVersion !== e.templateVersion;
  }

  private profileSyncBadge(e: AgentApplicationInfo): string {
    if (!e.applicationProfileId?.id) {
      return this.statusIcon('do_not_disturb_on', 'rgba(0,0,0,0.38)');
    }
    if (this.profileTemplateDrifted(e)) {
      return this.statusIcon('sync_disabled', '#e65100');
    }
    return e.profileConfigOutdated
      ? this.statusIcon('sync_problem', '#e65100')
      : this.statusIcon('check_circle', '#2e7d32');
  }

  private profileSyncTooltip(e: AgentApplicationInfo): string {
    if (!e.applicationProfileId?.id) {
      return this.translate.instant('agent.app-profile-in-sync-na');
    }
    if (this.profileTemplateDrifted(e)) {
      return this.translate.instant('agent.app-profile-drifted-hint', { version: e.profileTemplateVersion });
    }
    return e.profileConfigOutdated
      ? this.translate.instant('agent.app-profile-in-sync-no')
      : this.translate.instant('agent.app-profile-in-sync-yes');
  }

  private originBadge(origin: AgentApplicationOrigin | undefined): string {
    if (!origin) {
      return `<span style="color:rgba(0,0,0,0.38);font-size:12px;">—</span>`;
    }
    const config: Record<string, { icon: string; color: string }> = {
      INSTALLED: { icon: 'file_download', color: '#1565c0' },
      DISCOVERED: { icon: 'podcasts', color: '#326c57' },
      AUTO_PROVISIONED: { icon: 'settings_suggest', color: '#6a1b9a' }
    };
    const c = config[origin] || { icon: 'help_outline', color: '#616161' };
    const labelKey = agentApplicationOriginTranslationMap.get(origin);
    const label = labelKey ? this.translate.instant(labelKey) : origin;
    return `<span style="display:inline-flex;align-items:center;gap:4px;font-size:13px;font-weight:500;color:${c.color};"><span class="material-icons" style="font-size:18px;">${c.icon}</span>${label}</span>`;
  }

  private appTypeBadge(appType: string): string {
    const styles: Record<string, string> = {
      EDGE: 'background:#e8eaf6;color:#283593;',
      GATEWAY: 'background:#e0f2f1;color:#00695c;',
      GENERIC: 'background:#f3e5f5;color:#6a1b9a;'
    };
    const style = styles[appType] || 'background:#eeeeee;color:#616161;';
    return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${appType}</span>`;
  }

  private templateCell(e: AgentApplicationInfo): string {
    // currentVersion may come from BE (AgentApplicationInfo) or from our enrichment
    if (e.currentVersion) {
      return versionTag(e.currentVersion);
    }
    // Fallback: look up from template cache
    const tpl = e.templateVersion ? this.templateCache.get(e.templateVersion) : null;
    return versionTag(tpl?.currentVersion ?? e.templateVersion);
  }

  private enrichWithTemplates(page: any): Observable<any> {
    const hasMissing = page.data.some((app: AgentApplicationInfo) =>
      !app.currentVersion && app.templateVersion
    );
    if (!hasMissing) {
      return of(page);
    }
    return this.agentService.getAgentAppTemplates().pipe(
      map(templates => {
        templates.forEach(t => this.templateCache.set(t.currentVersion, t));
        page.data.forEach((app: AgentApplicationInfo) => {
          if (!app.currentVersion && app.templateVersion) {
            const tid = app.templateVersion;
            const tpl = this.templateCache.get(tid);
            if (tpl) {
              app.currentVersion = tpl.currentVersion;
            }
          }
        });
        return page;
      }),
      catchError(() => of(page))
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<AgentApplicationInfo>> {
    return [
      {
        name: this.translate.instant('agent.app-restart'),
        icon: 'restart_alt',
        isEnabled: () => true,
        onAction: ($event, e) => this.restart($event, e)
      },
      {
        name: this.translate.instant('agent.app-update'),
        icon: 'sync_alt',
        isEnabled: () => true,
        onAction: ($event, e) => this.update($event, e)
      },
      {
        name: this.translate.instant('agent.app-upgrade'),
        nameFunction: e => e.nextVersion
          ? this.translate.instant('agent.app-upgrade-to', { version: e.nextVersion })
          : this.translate.instant('agent.app-upgrade'),
        icon: 'arrow_upward',
        isEnabled: e => !!e.nextVersion,
        onAction: ($event, e) => this.openUpgradeWizard($event, e)
      },
      {
        name: this.translate.instant('agent.app-delete'),
        icon: 'delete',
        isEnabled: () => true,
        onAction: ($event, e) => this.openDeleteDialog($event, e)
      }
    ];
  }

  private update($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationInfoById(app.id.id).pipe(
      mergeMap(full => confirmUpdateDrift(this.dialog, full).pipe(
        map(confirmed => ({ full, confirmed }))
      ))
    ).subscribe(({ full, confirmed }) => {
      if (!confirmed) { return; }
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
          data: {
            agentId: this.agentId,
            agent: this.agent,
            mode: 'update',
            application: full
          }
        }
      ).afterClosed().subscribe(event => {
        if (event) {
          this.config.updateData();
        }
      });
    });
  }

  private restart($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.app-restart-title', { name: app.name }),
      this.translate.instant('agent.app-restart-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (!res) { return; }
      // Need the full application for the progress dialog (it reads
      // config.compose.volumes and templateId); the list row is trimmed.
      this.agentService.getAgentApplicationInfoById(app.id.id).pipe(
        mergeMap(full =>
          this.agentService.createAgentAppEvent(app.id.id, { actionType: AgentAppEventActionType.RESTART })
            .pipe(map(event => ({ full, event })))
        )
      ).subscribe(({ full, event }) => {
        this.config.updateData();
        if (event) {
          openAgentAppEventProgress(this.dialog, full, event).subscribe();
        }
      });
    });
  }

  private openDeleteDialog($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    // Fetch the full application so the dialog can render volume keys from
    // config.compose. Falls back to the list-row entity if the fetch fails.
    this.agentService.getAgentApplicationInfoById(app.id.id).subscribe({
      next: full => this.showDeleteDialog(full),
      error: () => this.showDeleteDialog(app)
    });
  }

  private showDeleteDialog(application: AgentApplication | AgentApplicationInfo) {
    const app = application as AgentApplication;
    const composeDownStep$ = app?.templateVersion
      ? this.agentService.getAgentAppTemplateByVersion(app.appType, AgentAppConfigType.DOCKER_COMPOSE, app.templateVersion).pipe(
          map(findComposeDownStep),
          catchError(() => of(null))
        )
      : of(null);
    composeDownStep$.subscribe(composeDownStep => {
      this.dialog.open<AgentAppDeleteDialogComponent, AgentAppDeleteDialogData, AgentAppEvent | null>(
        AgentAppDeleteDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            application: app,
            agentName: this.agent?.name,
            composeDownStep
          }
        }
      ).afterClosed().subscribe(event => {
        if (!event) { return; }
        this.config.updateData();
        openAgentAppEventProgress(this.dialog, app, event)
          .subscribe(() => this.config.updateData());
      });
    });
  }

  private openUpgradeWizard($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationInfoById(app.id.id).subscribe(full => this.showUpgradeWizard(full));
  }

  private showUpgradeWizard(application: AgentApplicationInfo) {
    this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
      AgentAppInstallWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: {
          agentId: this.agentId,
          agent: this.agent,
          mode: 'upgrade',
          application
        }
      }
    ).afterClosed().subscribe(event => {
      if (event) {
        this.config.updateData();
      }
    });
  }

  private openInstallWizard(): Observable<AgentApplicationInfo | null> {
    return this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
      AgentAppInstallWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { agentId: this.agentId, agent: this.agent }
      }
    ).afterClosed().pipe(
      switchMap(result => !isAgentAppUpgradeResult(result) && result?.applicationId?.id
        ? this.agentService.getAgentApplicationInfoById(result.applicationId.id)
        : of(null))
    );
  }

  private showUpdateHintDialog(app: AgentApplicationInfo) {
    this.dialogService.confirm(
      this.translate.instant('agent.app-save-update-hint-title'),
      this.translate.instant('agent.app-save-update-hint'),
      this.translate.instant('action.close'),
      this.translate.instant('agent.app-save-update-hint-action')
    ).subscribe(res => {
      if (res) {
        this.update(null, app);
      }
    });
  }
}

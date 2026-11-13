// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityLinkTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AddEntityDialogData, EntityAction } from '@home/models/entity/entity-component.models';
import { Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { catchError, map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@shared/models/customer.model';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { AgentAppEventInfo, AgentProcessingStatus, AgentInfo } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import {
  buildAgentErrorEventsTooltip,
  recentAgentErrorsPageLink
} from '@home/pages/agent/util/agent-error-events';
import {
  AgentUpgradeDialogComponent,
  AgentUpgradeDialogData
} from '@home/pages/agent/dialog/agent-upgrade-dialog.component';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';

import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';
import {
  AgentInstallInstructionsDialogComponent,
  AgentInstallInstructionsDialogData
} from '@home/pages/agent/agent-install-instructions-dialog.component';
import {
  AddEntityDialogComponent
} from '@home/components/entity/add-entity-dialog.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { AgentAutoProvisionDialogService } from '@home/components/agent/dialog/agent-auto-provision-dialog.service';

@Injectable()
export class AgentsTableConfigResolver {

  private agentErrorEvents = new Map<string, AgentAppEventInfo[]>();

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<AgentInfo>,
              private store: Store<AppState>,
              private agentService: AgentService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private autoProvisionDialogService: AgentAutoProvisionDialogService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<AgentInfo>(groupParams);
    this.configDefaults(config, route);

    const customerId = config.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          config.componentsData.agentScope = 'customer_user';
          config.componentsData.customerId = authUser.customerId;
        } else if (customerId) {
          config.componentsData.agentScope = 'customer';
          config.componentsData.customerId = customerId;
        }
      }),
      mergeMap(() =>
        config.componentsData.customerId
          ? this.customerService.getCustomer(config.componentsData.customerId)
          : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('agent.agents');
        } else {
          config.tableTitle = this.translate.instant('agent.agents');
        }
        config.columns = this.configureColumns(config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config);
        config.groupActionDescriptors = this.configureGroupActions();
        config.addActionDescriptors = this.configureAddActions(config);
        config.addEnabled = config.componentsData.agentScope !== 'customer_user';
        config.entitiesDeleteEnabled = config.componentsData.agentScope === 'tenant';
        config.deleteEnabled = () => config.componentsData.agentScope === 'tenant';
        config.onDestroy = () => this.agentErrorEvents.clear();
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<AgentInfo>, route: ActivatedRouteSnapshot) {
    config.entityType = EntityType.AGENT;
    config.entityComponent = AgentComponent;
    config.entityTabsComponent = AgentTabsComponent;
    // When inside a group-scoped leaf, the group tabs component is used by the
    // group container; for the flat /all list we keep the agent-specific tabs.
    if (route.data?.groupType && route.data?.hideTabs) {
      config.entityTabsComponent = GroupEntityTabsComponent<AgentInfo>;
    }
    config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT);
    config.entityResources = entityTypeResources.get(EntityType.AGENT);

    config.entityTitle = (agent) => agent ? agent.name : '';
    config.rowPointer = true;

    config.deleteEntityTitle = agent => this.translate.instant('agent.delete-agent-title', {agentName: agent.name});
    config.deleteEntityContent = () => this.translate.instant('agent.delete-agent-text');
    config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-agents-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('agent.delete-agents-text');

    config.loadEntity = id => this.agentService.getAgentInfoById(id.id);
    config.saveEntity = agent => this.agentService.saveAgent(agent).pipe(
      mergeMap((savedAgent) => this.agentService.getAgentInfoById(savedAgent.id.id))
    );
    config.onEntityAction = action => this.onAgentAction(action, config);
    config.handleRowClick = ($event, agent) => {
      this.manageApplications($event, agent);
      return true;
    };
    config.detailsReadonly = () => config.componentsData?.agentScope === 'customer_user';
    config.addEntity = () => this.addAgent(config);

    // Default scope comes from route.data (e.g. 'tenant' under /all) and is
    // overridden below if the current user is a customer user, or if the
    // route belongs to a customer-scoped hierarchy view (config.customerId).
    config.componentsData = {
      agentScope: route.data?.agentsType || 'tenant',
      customerId: null as string | null
    };
  }

  configureColumns(config: EntityTableConfig<AgentInfo>): Array<EntityColumn<AgentInfo>> {
    const scope = config.componentsData.agentScope;
    const columns: Array<EntityColumn<AgentInfo>> = [
      new DateEntityTableColumn<AgentInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentInfo>('name', 'agent.name', '20%', config.entityTitle),
      new EntityLinkTableColumn<AgentInfo>('agentProfileName', 'agent.agent-profile', '20%',
        entity => entity.agentProfileName || '—',
        entity => entity.agentProfileId?.id
          ? `/edgeManagement/profiles/agent/${entity.agentProfileId.id}`
          : '',
        false)
    ];
    if (scope === 'tenant' || scope === 'customer') {
      columns.push(
        new EntityTableColumn<AgentInfo>('customerTitle', 'customer.customer', '15%')
      );
    }
    columns.push(
      new EntityChipsEntityTableColumn<AgentInfo>('groups', 'entity.groups', '25%')
    );
    columns.push(
      new EntityTableColumn<AgentInfo>('active', 'agent.status', '140px',
        entity => this.agentStatus(entity), entity => this.agentStatusStyle(entity), false)
    );
    return columns;
  }


  private agentStatus(agent: AgentInfo): string {
    const isOnline = !!agent.active;
    const color = isOnline ? '#4caf50' : 'rgba(0,0,0,0.38)';
    const label = this.translate.instant(isOnline ? 'agent.online' : 'agent.offline');
    return `<span style="display:inline-flex; align-items:center; white-space:nowrap; color:${color};">
      <span style="display:inline-block; width:8px; height:8px; border-radius:50%; margin-right:6px; background:${color};"></span>
      ${label}
    </span>`;
  }

  private agentStatusStyle(_agent: AgentInfo): object {
    return {
      fontSize: '13px',
      fontWeight: '500'
    };
  }

  private hasAgentErrors(agent: AgentInfo): boolean {
    return (this.agentErrorEvents.get(agent.id.id)?.length ?? 0) > 0;
  }

  private agentErrorTooltip(agent: AgentInfo): string {
    const events = this.agentErrorEvents.get(agent.id.id) || [];
    return events.length ? buildAgentErrorEventsTooltip(events, this.translate, this.datePipe) : '';
  }

  private loadAgentErrorEvents(agents: AgentInfo[], config: EntityTableConfig<AgentInfo>) {
    const pageLink = recentAgentErrorsPageLink();
    agents.forEach(agent => {
      const id = agent.id.id;
      this.agentService.getAgentAppEventInfosByAgentId(
        id, pageLink, undefined, AgentProcessingStatus.ERROR, { ignoreErrors: true, ignoreLoading: true }
      ).subscribe({
        next: page => {
          this.agentErrorEvents.set(id, page?.data || []);
          config.getTable()?.detectChanges();
        },
        error: () => {
          this.agentErrorEvents.delete(id);
        }
      });
    });
  }

  private goToAgentErrorEvents($event: Event, agent: AgentInfo) {
    if ($event) { $event.stopPropagation(); }
    this.router.navigateByUrl(
      agentEntityUrl(currentAgentRouteSnapshot(this.router), agent.id.id, 'events'));
  }

  private reconcileAgentErrorEvents(agents: AgentInfo[], config: EntityTableConfig<AgentInfo>) {
    const visibleIds = new Set(agents.map(a => a.id.id));
    this.agentErrorEvents.forEach((_events, id) => {
      if (!visibleIds.has(id)) {
        this.agentErrorEvents.delete(id);
      }
    });
    this.loadAgentErrorEvents(agents, config);
  }

  configureEntityFunctions(config: EntityTableConfig<AgentInfo>): void {
    const scope = config.componentsData.agentScope;
    if (scope === 'tenant') {
      config.entitiesFetchFunction = pageLink =>
        this.agentService.getTenantAgentInfos(pageLink).pipe(tap(page =>
          this.reconcileAgentErrorEvents(page.data, config)
        ));
      config.deleteEntity = id => this.agentService.deleteAgent(id.id);
    }
    if (scope === 'customer' || scope === 'customer_user') {
      config.entitiesFetchFunction = pageLink =>
        this.agentService.getCustomerAgentInfos(config.componentsData.customerId, pageLink).pipe(tap(page =>
          this.reconcileAgentErrorEvents(page.data, config)
        ));
    }
  }

  configureCellActions(config: EntityTableConfig<AgentInfo>): Array<CellActionDescriptor<AgentInfo>> {
    return [
      {
        name: this.translate.instant('agent.app-errors'),
        nameFunction: (agent) => this.agentErrorTooltip(agent),
        icon: 'warning',
        iconFunction: (agent) => this.hasAgentErrors(agent) ? 'warning' : '',
        style: { color: '#f57c00' },
        isEnabled: (agent) => this.hasAgentErrors(agent),
        onAction: ($event, agent) => this.goToAgentErrorEvents($event, agent)
      },
      {
        name: this.translate.instant('agent.upgrade-agent-action'),
        icon: 'arrow_upward',
        isEnabled: (agent) => config.componentsData.agentScope !== 'customer_user' && this.canUpgrade(agent),
        onAction: ($event, agent) => this.openUpgrade($event, agent)
      },
      {
        name: this.translate.instant('agent.agent-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, agent) => config.toggleEntityDetails($event, agent)
      }
    ];
  }

  private canUpgrade(agent: AgentInfo): boolean {
    return !!agent?.upgradeTargetImageRef;
  }

  private openUpgrade($event: Event, agent: AgentInfo): void {
    if ($event) {
      $event.stopPropagation();
    }
    const agentId = agent?.id?.id;
    if (!agentId) {
      return;
    }
    const data: AgentUpgradeDialogData = {
      agentId,
      agentName: agent.name,
      currentImageRef: agent.agentVersion,
      suggestedImageRef: agent.upgradeTargetImageRef
    };
    this.dialog.open<AgentUpgradeDialogComponent, AgentUpgradeDialogData, any>(
      AgentUpgradeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data
      }
    ).afterClosed().subscribe(event => {
      if (event) {
        openAgentAppEventProgress(this.dialog, null, event).subscribe();
      }
    });
  }

  private manageApplications($event: Event, agent: AgentInfo) {
    if ($event) { $event.stopPropagation(); }
    this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agent.id.id, 'applications'));
  }

  private openAgent($event: Event, agent: AgentInfo, config: EntityTableConfig<AgentInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([agent.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  configureGroupActions(): Array<GroupActionDescriptor<AgentInfo>> {
    return [];
  }

  configureAddActions(config: EntityTableConfig<AgentInfo>): Array<HeaderActionDescriptor> {
    return [
      {
        name: this.translate.instant('agent.add-agent-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('agent.auto-provision'),
        icon: 'auto_fix_high',
        isEnabled: () => true,
        onAction: ($event) => this.autoProvisionAgent($event, config)
      }
    ];
  }

  private autoProvisionAgent($event: Event, config: EntityTableConfig<AgentInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.autoProvisionDialogService.open().subscribe(result => {
      if (result) {
        config.updateData();
      }
    });
  }

  private addAgent(config: EntityTableConfig<AgentInfo>): Observable<AgentInfo> {
    return this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<AgentInfo>, AgentInfo>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entitiesTableConfig: config
        }
      }).afterClosed().pipe(
        tap(entity => {
          if (entity) {
            this.openInstallInstructions(null, entity, true, config);
          }
        })
      );
  }

  openInstallInstructions($event: Event, agent: AgentInfo, afterAdd = false, config?: EntityTableConfig<AgentInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.agentService.getAgentInstallInstructions(agent.id.id).pipe(
      catchError(() => of({ instructions: '' }))
    ).subscribe(res => {
      this.dialog.open<AgentInstallInstructionsDialogComponent, AgentInstallInstructionsDialogData>(
        AgentInstallInstructionsDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: {
            agent,
            afterAdd,
            instructions: res?.instructions || ''
          }
        }).afterClosed().subscribe(() => {
          if (afterAdd && config) {
            config.updateData();
            config.entityAdded(agent);
          }
        });
    });
  }

  manageOwnerAndGroups($event: Event, agent: AgentInfo, config: EntityTableConfig<AgentInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, agent).subscribe(res => {
      if (res) {
        config.updateData();
      }
    });
  }

  onAgentAction(action: EntityAction<AgentInfo>, config: EntityTableConfig<AgentInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openAgent(action.event, action.entity, config);
        return true;
      case 'openInstallInstructions':
        this.openInstallInstructions(action.event, action.entity, false, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}

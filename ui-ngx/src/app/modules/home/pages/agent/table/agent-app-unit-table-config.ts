// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injector, NgZone, StaticProvider, ViewContainerRef } from '@angular/core';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  AgentApplicationInfo,
  AgentAppUnit,
  AgentAppUnitType,
  agentAppUnitTypeTranslationMap
} from '@shared/models/agent.models';
import {
  AttributeScope,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { EntityType, EntityTypeResource } from '@shared/models/entity-type.models';
import { versionTag } from '@home/pages/agent/util/version-tag';
import {
  AGENT_APP_UNIT_FILTER_PANEL_DATA,
  AgentAppUnitFilterPanelComponent,
  AgentAppUnitFilterPanelData,
  AgentAppUnitFilterValue
} from './agent-app-unit-filter-panel.component';

const typeBadgeStyles: Record<string, string> = {
  CONTAINER: 'background:#e3f2fd;color:#1565c0;',
  VOLUME:    'background:#e8f5e9;color:#2e7d32;',
  NETWORK:   'background:#f3e5f5;color:#6a1b9a;'
};

const stateConfig: Record<string, { icon: string; color: string }> = {
  running:    { icon: 'check_circle', color: '#2e7d32' },
  exited:     { icon: 'cancel', color: '#c62828' },
  dead:       { icon: 'cancel', color: '#c62828' },
  paused:     { icon: 'pause_circle', color: '#f57c00' },
  restarting: { icon: 'autorenew', color: '#f57c00' },
  created:    { icon: 'hourglass_empty', color: '#f57c00' }
};

// Entities-table renders cellContentFunction results via
// bypassSecurityTrustHtml, so any agent-reported string (identifier, image,
// container state, …) must be HTML-escaped before it lands in a cell.
function escapeHtml(value: string): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function badge(value: string, style: string): string {
  return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;`
    + `font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${escapeHtml(value)}</span>`;
}

function stateBadge(state: string): string {
  const c = stateConfig[state] || { icon: 'help_outline', color: '#616161' };
  return `<span style="display:inline-flex;align-items:center;gap:4px;font-size:13px;font-weight:500;color:${c.color};"><span class="material-icons" style="font-size:18px;">${c.icon}</span>${escapeHtml(state)}</span>`;
}

export class AgentAppUnitTableConfig extends EntityTableConfig<AgentAppUnit> {

  private filter: AgentAppUnitFilterValue = { type: null };
  private activeSubs = new Map<string, TelemetrySubscriber>();
  private visibleUnits = new Map<string, AgentAppUnit>();

  constructor(private readonly application: AgentApplicationInfo,
              private readonly agentService: AgentService,
              private readonly attributeService: AttributeService,
              private readonly translate: TranslateService,
              private readonly overlay: Overlay,
              private readonly viewContainerRef: ViewContainerRef,
              private readonly telemetryWsService: TelemetryWebsocketService,
              private readonly zone: NgZone,
              private readonly router: Router) {
    super();

    this.tableTitle = this.translate.instant('agent.app-units');
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.rowPointer = true;
    // Per-tab table, not a page-level table — don't let router query params
    // drive our paginator/sort (the parent Applications list shares the URL).
    this.pageMode = false;
    this.defaultSortOrder = { property: 'type', direction: Direction.ASC };

    this.entityTranslations = { noEntities: 'agent.app-no-units' };
    this.entityResources = {} as EntityTypeResource<AgentAppUnit>;

    this.columns.push(
      new EntityTableColumn<AgentAppUnit>('identifier',
        'agent.app-unit-identifier', '40%',
        (u) => escapeHtml(u.identifier), () => ({}), true),
      new EntityTableColumn<AgentAppUnit>('type',
        'agent.app-type', '140px',
        (u) => badge(
          this.translate.instant(agentAppUnitTypeTranslationMap.get(u.type) || u.type),
          typeBadgeStyles[u.type] || 'background:#eeeeee;color:#616161;'
        ),
        () => ({}),
        true
      ),
      new EntityTableColumn<AgentAppUnit>('image',
        'agent.app-unit-image', '30%',
        (u) => u.image ? escapeHtml(u.image) : versionTag(null), () => ({}), false),
      new EntityTableColumn<AgentAppUnit>('state',
        'agent.app-unit-state', '140px',
        (u) => u.state
          ? stateBadge(u.state)
          : versionTag(null),
        () => ({}), false
      )
    );

    this.headerActionDescriptors.push(
      {
        name: this.translate.instant('agent.app-unit-filter'),
        icon: 'filter_list',
        isEnabled: () => true,
        onAction: ($event) => this.openFilterPanel($event)
      },
      {
        name: this.translate.instant('action.clear'),
        icon: 'mdi:filter-variant-remove',
        isEnabled: () => this.hasActiveFilter(),
        onAction: () => this.clearFilter()
      }
    );

    this.cellActionDescriptors.push({
      name: this.translate.instant('agent.app-unit-view-logs'),
      icon: 'description',
      isEnabled: (unit) => !!unit && unit.type === AgentAppUnitType.CONTAINER,
      onAction: ($event, entity) => this.openLogViewer($event, entity)
    });

    this.handleRowClick = ($event, entity) => {
      if (!entity || entity.type !== AgentAppUnitType.CONTAINER) {
        return false;
      }
      this.openLogViewer($event as MouseEvent, entity);
      return true;
    };

    this.entitiesFetchFunction = (pageLink) => this.fetch(pageLink);
  }

  private openLogViewer($event: Event, unit: AgentAppUnit): void {
    if ($event) { $event.stopPropagation(); }
    const agentId = this.application.agentId?.id;
    if (!agentId) { return; }
    this.router.navigate(
      [agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, 'applications', this.application.id.id, 'units', unit.id.id, 'logs')],
      { queryParams: { identifier: unit.identifier } }
    );
  }

  private fetch(pageLink: PageLink): Observable<PageData<AgentAppUnit>> {
    return this.agentService
      .getAgentAppUnits(this.application.id.id, pageLink, this.filter.type || undefined)
      .pipe(switchMap(page => this.enrich(page)));
  }

  /**
   * Only CONTAINER units carry live `image`/`state` attributes, populated on
   * the unit entity in SERVER_SCOPE by ComposeUnitsSynchronizer. Fetch them
   * as part of the same observable chain so the data source receives rows
   * with the attributes already merged in — no mutation-after-render dance.
   */
  private enrich(page: PageData<AgentAppUnit>): Observable<PageData<AgentAppUnit>> {
    const containers = page.data.filter(u => u.type === AgentAppUnitType.CONTAINER);
    if (!containers.length) {
      return of(page);
    }
    const requests = containers.map(u =>
      this.attributeService.getEntityAttributes(
        { entityType: EntityType.AGENT_APP_UNIT, id: u.id.id },
        AttributeScope.SERVER_SCOPE,
        ['image', 'state'],
        { ignoreLoading: true, ignoreErrors: true }
      ).pipe(catchError(() => of([])))
    );
    return forkJoin(requests).pipe(map(results => {
      results.forEach((attrs, i) => {
        const unit = containers[i];
        for (const attr of attrs) {
          if (attr.key === 'image') { unit.image = attr.value as string; }
          if (attr.key === 'state') { unit.state = attr.value as string; }
        }
      });
      this.reconcileSubscriptions(containers);
      return page;
    }));
  }

  private reconcileSubscriptions(containers: AgentAppUnit[]) {
    const visibleIds = new Set(containers.map(u => u.id.id));
    this.activeSubs.forEach((sub, id) => {
      if (!visibleIds.has(id)) {
        sub.unsubscribe();
        sub.complete();
        this.activeSubs.delete(id);
      }
    });
    this.visibleUnits = new Map(containers.map(u => [u.id.id, u]));
    containers.forEach(u => this.subscribeUnitAttributes(u));
  }

  private subscribeUnitAttributes(unit: AgentAppUnit) {
    const id = unit.id.id;
    if (this.activeSubs.has(id)) { return; }
    const subscriber = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      unit.id,
      AttributeScope.SERVER_SCOPE,
      this.zone,
      ['image', 'state']
    );
    subscriber.data$.subscribe(update => {
      if (!update?.data) { return; }
      const row = this.visibleUnits.get(id) ?? unit;
      let changed = false;
      const imageEntries = update.data['image'];
      if (imageEntries?.length) {
        row.image = imageEntries[0][1] as string;
        changed = true;
      }
      const stateEntries = update.data['state'];
      if (stateEntries?.length) {
        row.state = stateEntries[0][1] as string;
        changed = true;
      }
      if (changed) {
        this.zone.run(() => this.getTable()?.detectChanges());
      }
    });
    subscriber.subscribe();
    this.activeSubs.set(id, subscriber);
  }

  destroySubscriptions() {
    this.activeSubs.forEach(sub => {
      sub.unsubscribe();
      sub.complete();
    });
    this.activeSubs.clear();
    this.visibleUnits.clear();
  }

  private hasActiveFilter(): boolean {
    return !!this.filter.type;
  }

  private clearFilter(): void {
    if (!this.hasActiveFilter()) { return; }
    this.filter = { type: null };
    this.getTable().paginator.pageIndex = 0;
    this.updateData();
  }

  private openFilterPanel($event: MouseEvent): void {
    if ($event) { $event.stopPropagation(); }
    const target = ($event.target || $event.currentTarget) as HTMLElement;
    const config = new OverlayConfig({
      panelClass: 'tb-panel-container',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      height: 'fit-content',
      maxHeight: '65vh'
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(target)
      .withPositions([
        { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top' },
        { originX: 'end',   originY: 'bottom', overlayX: 'end',   overlayY: 'top' }
      ]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => overlayRef.dispose());

    const providers: StaticProvider[] = [
      {
        provide: AGENT_APP_UNIT_FILTER_PANEL_DATA,
        useValue: { value: { ...this.filter } } as AgentAppUnitFilterPanelData
      },
      { provide: OverlayRef, useValue: overlayRef }
    ];
    const injector = Injector.create({ parent: this.viewContainerRef.injector, providers });
    const ref = overlayRef.attach(new ComponentPortal(
      AgentAppUnitFilterPanelComponent, this.viewContainerRef, injector));
    ref.onDestroy(() => {
      const result = ref.instance.result;
      if (result && result.type !== this.filter.type) {
        this.filter = result;
        this.getTable().paginator.pageIndex = 0;
        this.updateData();
      }
    });
  }
}

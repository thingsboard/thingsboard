// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { MatDrawer } from '@angular/material/sidenav';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AgentService } from '@core/http/agent.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AgentId } from '@shared/models/id/agent-id';
import { AgentApplicationId } from '@shared/models/id/agent-application-id';
import { AgentMetricsSubscription } from '@home/pages/agent/util/agent-metrics-subscription';
import { MetricsSnapshot } from '@home/pages/agent/util/agent-metrics';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { MetricsEntityRef } from './agent-multi-entity-metrics-panel.component';
import { MatDialog } from '@angular/material/dialog';
import { AgentAppEventInfo, AgentApplicationInfo, AgentInfo, AgentProcessingStatus } from '@shared/models/agent.models';
import {
  AgentUpgradeDialogComponent,
  AgentUpgradeDialogData
} from '@home/pages/agent/dialog/agent-upgrade-dialog.component';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';
import { agentEntityUrl } from '@home/pages/agent/util/agent-route-params';
import {
  buildAgentErrorEventsTooltip,
  recentAgentErrorsPageLink
} from '@home/pages/agent/util/agent-error-events';

@Component({
  selector: 'tb-agent-applications-page',
  templateUrl: './agent-applications-page.component.html',
  styleUrls: ['./agent-applications-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentApplicationsPageComponent implements OnInit, OnDestroy {

  @ViewChild('chartsDrawer') chartsDrawer!: MatDrawer;

  entitiesTableConfig: EntityTableConfig<AgentApplicationInfo> | null = null;
  agentEntityId: AgentId | null = null;
  snapshot: MetricsSnapshot = AgentMetricsSubscription.empty();
  agentVersion: string | null = null;
  agentName = '';
  upgradeAvailable = false;
  upgradeTargetImageRef = '';

  /** Just the tag, so the chip reads "Upgrade to 4.4.1" rather than repeating the whole reference. */
  get upgradeTargetTag(): string {
    const ref = this.upgradeTargetImageRef;
    const lastSlash = ref.lastIndexOf('/');
    const lastColon = ref.lastIndexOf(':');
    return lastColon > lastSlash ? ref.substring(lastColon + 1) : ref;
  }
  chartsOpen = false;
  entities: MetricsEntityRef[] = [];
  errorCount = 0;
  errorTooltip = '';

  private destroy$ = new Subject<void>();
  private wrappedConfig: EntityTableConfig<AgentApplicationInfo> | null = null;
  private originalFetchFunction: EntityTableConfig<AgentApplicationInfo>['entitiesFetchFunction'] | null = null;
  private subscription: AgentMetricsSubscription;
  private loadedAppsForAgentId: string | null = null;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cdr: ChangeDetectorRef,
              private agentService: AgentService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private datePipe: DatePipe) {
    this.subscription = new AgentMetricsSubscription(telemetryWsService, zone);
  }

  ngOnInit(): void {
    this.subscription.snapshot$.pipe(takeUntil(this.destroy$)).subscribe(snap => {
      this.snapshot = snap;
      this.cdr.markForCheck();
    });
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe(data => {
      const config = data.entitiesTableConfig as EntityTableConfig<AgentApplicationInfo> | undefined;
      if (!config) {
        return;
      }
      this.unwrapFetchFunction();
      this.wrappedConfig = config;
      this.originalFetchFunction = config.entitiesFetchFunction;
      config.entitiesFetchFunction = pageLink => this.originalFetchFunction(pageLink).pipe(
        tap(() => this.fetchErrorEvents())
      );
      this.entitiesTableConfig = config;
      const componentsData = (config.componentsData as { agentId?: string }) || {};
      const newAgentId = componentsData.agentId ? new AgentId(componentsData.agentId) : null;
      const changed = newAgentId?.id !== this.agentEntityId?.id;
      this.agentEntityId = newAgentId;
      this.cdr.markForCheck();
      if (changed) {
        this.subscription.tearDown();
        this.entities = [];
        this.loadedAppsForAgentId = null;
        this.agentVersion = null;
        this.upgradeAvailable = false;
        this.upgradeTargetImageRef = '';
        this.errorCount = 0;
        this.errorTooltip = '';
        if (this.agentEntityId) {
          this.subscription.subscribe(this.agentEntityId, this.agentEntityId);
          this.fetchApplications(this.agentEntityId.id);
          this.fetchUpgradeState(this.agentEntityId.id);
        }
      }
    });
  }

  /**
   * The reported image reference doubles as the agent's version, and the upgrade target is resolved
   * server-side onto the same info object, so the strip can show what is running and whether anything
   * newer applies from a single read.
   */
  private fetchUpgradeState(agentId: string): void {
    this.agentService.getAgentInfoById(agentId, {ignoreErrors: true, ignoreLoading: true})
      .subscribe({
        next: (info: AgentInfo) => {
          this.agentVersion = info?.agentVersion || null;
          this.agentName = info?.name || '';
          this.upgradeTargetImageRef = info?.upgradeTargetImageRef || '';
          this.upgradeAvailable = !!this.upgradeTargetImageRef;
          this.cdr.markForCheck();
        },
        error: () => {
          this.agentVersion = null;
          this.upgradeAvailable = false;
        }
      });
  }

  onUpgradeAgent($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.agentEntityId) {
      return;
    }
    const data: AgentUpgradeDialogData = {
      agentId: this.agentEntityId.id,
      agentName: this.agentName,
      currentImageRef: this.agentVersion,
      suggestedImageRef: this.upgradeTargetImageRef
    };
    this.dialog.open<AgentUpgradeDialogComponent, AgentUpgradeDialogData, any>(
      AgentUpgradeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data
      }
    ).afterClosed().subscribe(event => {
      if (event) {
        openAgentAppEventProgress(this.dialog, null, event)
          .subscribe(() => {
            this.fetchUpgradeState(this.agentEntityId.id);
            this.fetchErrorEvents();
          });
      }
    });
  }

  private fetchErrorEvents(): void {
    const agentId = this.agentEntityId?.id;
    if (!agentId) {
      return;
    }
    this.agentService.getAgentAppEventInfosByAgentId(agentId, recentAgentErrorsPageLink(), undefined,
      AgentProcessingStatus.ERROR, { ignoreErrors: true, ignoreLoading: true })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: page => {
          if (agentId !== this.agentEntityId?.id) {
            return;
          }
          const events: AgentAppEventInfo[] = page?.data || [];
          this.errorCount = page?.totalElements ?? events.length;
          this.errorTooltip = events.length
            ? buildAgentErrorEventsTooltip(events, this.translate, this.datePipe)
              + '\n' + this.translate.instant('agent.app-errors-open-events')
            : '';
          this.cdr.markForCheck();
        },
        error: () => {
          this.errorCount = 0;
          this.errorTooltip = '';
          this.cdr.markForCheck();
        }
      });
  }

  goToErrorEvents(): void {
    if (!this.agentEntityId) {
      return;
    }
    this.router.navigateByUrl(agentEntityUrl(this.route.snapshot, this.agentEntityId.id, 'events'));
  }

  private fetchApplications(agentId: string): void {
    if (this.loadedAppsForAgentId === agentId) { return; }
    this.loadedAppsForAgentId = agentId;
    const pageLink = new PageLink(200, 0, null,
      { property: 'name', direction: Direction.ASC });
    this.agentService.getAgentApplicationsByAgentId(agentId, pageLink,
      { ignoreLoading: true, ignoreErrors: true })
      .pipe(takeUntil(this.destroy$))
      .subscribe(page => {
        this.entities = page.data.map(app => ({
          entityId: new AgentApplicationId(app.id.id),
          label: app.name
        }));
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.unwrapFetchFunction();
    this.destroy$.next();
    this.destroy$.complete();
    this.subscription.tearDown();
  }

  private unwrapFetchFunction(): void {
    if (this.wrappedConfig) {
      this.wrappedConfig.entitiesFetchFunction = this.originalFetchFunction;
      this.wrappedConfig = null;
      this.originalFetchFunction = null;
    }
  }

  toggleCharts(): void {
    this.chartsOpen = !this.chartsOpen;
    this.cdr.markForCheck();
  }

  closeCharts(): void {
    if (this.chartsOpen) {
      this.chartsOpen = false;
      this.cdr.markForCheck();
    }
  }
}

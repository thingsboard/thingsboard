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
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AgentService } from '@core/http/agent.service';
import { AgentApplicationId } from '@shared/models/id/agent-application-id';
import { AgentAppUnitId } from '@shared/models/id/agent-app-unit-id';
import { AgentMetricsSubscription } from '@home/pages/agent/util/agent-metrics-subscription';
import { MetricsSnapshot } from '@home/pages/agent/util/agent-metrics';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { MetricsEntityRef } from './agent-multi-entity-metrics-panel.component';

@Component({
  selector: 'tb-agent-application-details-page',
  templateUrl: './agent-application-details-page.component.html',
  styleUrls: ['./agent-application-details-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentApplicationDetailsPageComponent implements OnInit, OnDestroy {

  appEntityId: AgentApplicationId | null = null;
  snapshot: MetricsSnapshot = AgentMetricsSubscription.empty();
  chartsOpen = false;
  entities: MetricsEntityRef[] = [];

  @ViewChild(EntityDetailsPageComponent) detailsPage?: EntityDetailsPageComponent;

  private destroy$ = new Subject<void>();
  private subscription: AgentMetricsSubscription;
  private loadedUnitsForAppId: string | null = null;

  constructor(private route: ActivatedRoute,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cdr: ChangeDetectorRef,
              private agentService: AgentService) {
    this.subscription = new AgentMetricsSubscription(telemetryWsService, zone);
  }

  get entity(): any {
    return this.detailsPage?.entity;
  }

  get entitiesTableConfig(): any {
    return this.route.snapshot.data.entitiesTableConfig;
  }

  ngOnInit(): void {
    this.subscription.snapshot$.pipe(takeUntil(this.destroy$)).subscribe(snap => {
      this.snapshot = snap;
      this.cdr.markForCheck();
    });
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params.get('entityId');
      const newId = id ? new AgentApplicationId(id) : null;
      const changed = newId?.id !== this.appEntityId?.id;
      this.appEntityId = newId;
      this.cdr.markForCheck();
      if (changed) {
        this.subscription.tearDown();
        this.entities = [];
        this.loadedUnitsForAppId = null;
        if (this.appEntityId) {
          this.subscription.subscribe(this.appEntityId, null);
          this.fetchUnits(this.appEntityId.id);
        }
      }
    });
  }

  private fetchUnits(appId: string): void {
    if (this.loadedUnitsForAppId === appId) {
      return;
    }
    this.loadedUnitsForAppId = appId;
    const pageLink = new PageLink(200, 0, null,
      { property: 'identifier', direction: Direction.ASC });
    // Fetch all unit types. The panel uses each entity's `type` to scope its
    // WS subscription: CONTAINER → cpu/mem only, VOLUME → sizeBytes only,
    // NETWORK → dropped entirely.
    this.agentService.getAgentAppUnits(appId, pageLink, undefined,
      { ignoreLoading: true, ignoreErrors: true })
      .pipe(takeUntil(this.destroy$))
      .subscribe(page => {
        this.entities = page.data.map(u => ({
          entityId: new AgentAppUnitId(u.id.id),
          label: u.identifier,
          type: u.type
        }));
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscription.tearDown();
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

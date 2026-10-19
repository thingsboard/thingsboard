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
import { Location } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  AttributeScope,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { AgentId } from '@shared/models/id/agent-id';
import { AgentAppUnitId } from '@shared/models/id/agent-app-unit-id';
import { LogViewerComponent } from './log-viewer.component';
import { AgentMetricsSubscription } from '@home/pages/agent/util/agent-metrics-subscription';
import { MetricsSnapshot } from '@home/pages/agent/util/agent-metrics';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';

@Component({
  selector: 'tb-agent-app-unit-log-viewer-page',
  templateUrl: './agent-app-unit-log-viewer-page.component.html',
  styleUrls: ['./agent-app-unit-log-viewer-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentAppUnitLogViewerPageComponent implements OnInit, OnDestroy {

  @ViewChild(LogViewerComponent, { static: true })
  logViewer: LogViewerComponent;

  unitIdentifier = '';
  appType = '';
  image = '';
  state = '';
  agentOnline = false;
  metrics: MetricsSnapshot = AgentMetricsSubscription.empty();

  private destroy$ = new Subject<void>();
  private logSub: TelemetrySubscriber | null = null;
  private activeSub: TelemetrySubscriber | null = null;
  private unitAttrSub: TelemetrySubscriber | null = null;
  private metricsSub: AgentMetricsSubscription | null = null;
  private lastSeenLogSeq = 0;

  private unitId: AgentAppUnitId;
  private applicationId: string;
  private agentId: AgentId;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private location: Location,
              private agentService: AgentService,
              private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cdr: ChangeDetectorRef) {}

  back(): void {
    if (this.agentId?.id && this.applicationId) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), this.agentId.id, 'applications', this.applicationId));
      return;
    }
    this.location.back();
  }

  ngOnInit(): void {
    const params = this.route.snapshot.paramMap;
    this.applicationId = params.get('applicationId');
    this.unitId = new AgentAppUnitId(params.get('unitId'));
    if (params.get('agentId')) {
      this.agentId = new AgentId(params.get('agentId'));
    }

    this.subscribeToUnitAttributes();
    this.subscribeToMetrics();
    this.loadHeader();
    this.subscribeToLogs();
    if (this.agentId) {
      this.subscribeToAgentActive();
    }
  }

  ngOnDestroy(): void {
    this.tearDownLogs();
    if (this.activeSub) {
      this.activeSub.unsubscribe();
      this.activeSub.complete();
      this.activeSub = null;
    }
    if (this.unitAttrSub) {
      this.unitAttrSub.unsubscribe();
      this.unitAttrSub.complete();
      this.unitAttrSub = null;
    }
    if (this.metricsSub) {
      this.metricsSub.tearDown();
      this.metricsSub = null;
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private subscribeToMetrics(): void {
    if (!this.unitId) {
      return;
    }
    if (!this.metricsSub) {
      this.metricsSub = new AgentMetricsSubscription(this.telemetryWsService, this.zone);
      this.metricsSub.snapshot$.pipe(takeUntil(this.destroy$)).subscribe(snap => {
        this.metrics = snap;
        this.zone.run(() => this.cdr.markForCheck());
      });
    }
    this.metricsSub.subscribe(this.unitId, null);
  }

  private loadHeader(): void {
    if (!this.applicationId) {
      return;
    }
    this.agentService.getAgentApplicationInfoById(this.applicationId, { ignoreLoading: true, ignoreErrors: true })
      .pipe(takeUntil(this.destroy$))
      .subscribe(app => {
        if (!app) {
          return;
        }
        this.appType = app.appType;
        const projectName = app.projectName ?? '';
        if (!this.agentId && app.agentId?.id) {
          this.agentId = new AgentId(app.agentId.id);
          this.subscribeToAgentActive();
        }
        const queryIdentifier = this.route.snapshot.queryParamMap.get('identifier') || undefined;
        const stateIdentifier = window.history.state?.unitIdentifier as string | undefined;
        const unitName = queryIdentifier ?? stateIdentifier;
        this.unitIdentifier = unitName
          ? (projectName ? `${projectName}/${unitName}` : unitName)
          : projectName;
        this.cdr.markForCheck();
      });

  }

  private subscribeToUnitAttributes(): void {
    if (!this.unitId || this.unitAttrSub) {
      return;
    }
    this.unitAttrSub = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      this.unitId,
      AttributeScope.SERVER_SCOPE,
      this.zone,
      ['image', 'state']
    );
    this.unitAttrSub.data$.pipe(takeUntil(this.destroy$)).subscribe(update => {
      if (!update?.data) {
        return;
      }
      let changed = false;
      const imageEntries = update.data['image'];
      if (imageEntries?.length) {
        this.image = imageEntries[0][1] as string;
        changed = true;
      }
      const stateEntries = update.data['state'];
      if (stateEntries?.length) {
        this.state = stateEntries[0][1] as string;
        changed = true;
      }
      if (changed) {
        this.zone.run(() => this.cdr.markForCheck());
      }
    });
    this.unitAttrSub.subscribe();
  }

  private subscribeToLogs(): void {
    this.logSub = TelemetrySubscriber.createLogsSubscription(
      this.telemetryWsService,
      this.unitId,
      this.zone,
      this.lastSeenLogSeq
    );
    this.logSub.logs$.pipe(takeUntil(this.destroy$)).subscribe(update => {
      if (!update) {
        return;
      }
      this.zone.run(() => {
        if (update.droppedLines > 0) {
          this.logViewer.recordDropped(update.droppedLines);
        }
        if (update.evictedChunks > 0) {
          this.logViewer.appendGap(update.evictedChunks);
        }
        if (update.lines && update.lines.length) {
          this.logViewer.appendLines(update.lines);
        }
        if (update.latestSeq > this.lastSeenLogSeq) {
          this.lastSeenLogSeq = update.latestSeq;
        }
      });
    });
    this.logSub.subscribe();
  }

  private tearDownLogs(): void {
    if (this.logSub) {
      this.logSub.unsubscribe();
      this.logSub.complete();
      this.logSub = null;
    }
  }

  private subscribeToAgentActive(): void {
    if (!this.agentId || this.activeSub) {
      return;
    }
    this.activeSub = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      this.agentId,
      AttributeScope.SERVER_SCOPE,
      this.zone,
      ['active']
    );
    this.activeSub.data$.pipe(takeUntil(this.destroy$)).subscribe(update => {
      const entries = update?.data?.['active'];
      if (!entries?.length) {
        return;
      }
      const value = entries[0][1];
      const isActive = value === true || value === 'true';
      this.zone.run(() => this.onAgentActiveChanged(isActive));
    });
    this.activeSub.subscribe();
  }

  private onAgentActiveChanged(active: boolean): void {
    this.agentOnline = active;
    this.cdr.markForCheck();
    if (!active) {
      this.tearDownLogs();
    } else if (!this.logSub) {
      this.subscribeToLogs();
    }
  }
}

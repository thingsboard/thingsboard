// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subject, Subscription, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import {
  AgentAppEvent,
  AgentAppEventInfo,
  AgentBulkActionEventStats,
  AgentProcessingStatus
} from '@shared/models/agent.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

/**
 * Any table config that wants to render the events stats header implements this
 * by exposing a fetch returning the per-status event counts.
 */
export interface EventsStatsFetcher {
  fetchEventStats(): Observable<AgentBulkActionEventStats>;
}

/**
 * The header is handed an `EntityTableConfig`, and only some configs implement the fetch, so the
 * narrowing happens here once rather than at each use site.
 */
function asEventsStatsFetcher(cfg: unknown): EventsStatsFetcher | null {
  const candidate = cfg as EventsStatsFetcher | null;
  return typeof candidate?.fetchEventStats === 'function' ? candidate : null;
}

interface StatusCounts {
  total: number;
  done: number;
  error: number;
  running: number;
  pending: number;
}

type AnyEvent = AgentAppEvent | AgentAppEventInfo;

@Component({
  selector: 'tb-agent-events-stats-header',
  templateUrl: './agent-events-stats-header.component.html',
  styleUrls: ['./agent-events-stats-header.component.scss'],
  standalone: false
})
export class AgentEventsStatsHeaderComponent
  extends EntityTableHeaderComponent<AnyEvent>
  implements OnInit, OnDestroy {

  counts: StatusCounts = { total: 0, done: 0, error: 0, running: 0, pending: 0 };

  private fetcher: EventsStatsFetcher | null = null;
  private pollSub: Subscription | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private cd: ChangeDetectorRef) {
    super();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  get successPct(): number {
    return this.counts.total === 0
      ? 0
      : Math.round((this.counts.done / this.counts.total) * 100);
  }

  get donePct(): number { return this.pctOf(this.counts.done); }
  get errorPct(): number { return this.pctOf(this.counts.error); }
  get runningPct(): number { return this.pctOf(this.counts.running); }

  private pctOf(n: number): number {
    return this.counts.total === 0 ? 0 : (n / this.counts.total) * 100;
  }

  protected override setEntitiesTableConfig(cfg: EntityTableConfig<AnyEvent>) {
    super.setEntitiesTableConfig(cfg);
    this.stopPolling();
    this.fetcher = asEventsStatsFetcher(cfg);
    if (this.fetcher) {
      this.loadCounts();
      this.pollSub = timer(3000, 3000).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadCounts());
    }
  }

  private loadCounts(): void {
    if (!this.fetcher) { return; }
    this.fetcher.fetchEventStats().pipe(takeUntil(this.destroy$)).subscribe({
      next: stats => {
        this.counts = this.computeCounts(stats);
        this.cd.markForCheck();
        // Once every event has reached a terminal state there is nothing left
        // to refresh — stop the 3s poll.
        if (this.allEventsTerminal()) {
          this.stopPolling();
        }
      },
      error: () => {}
    });
  }

  private allEventsTerminal(): boolean {
    if (this.counts.total === 0 || this.counts.pending > 0 || this.counts.running > 0) {
      return false;
    }
    return this.counts.done + this.counts.error === this.counts.total;
  }

  private computeCounts(stats: AgentBulkActionEventStats): StatusCounts {
    const count = (...statuses: AgentProcessingStatus[]) =>
      statuses.reduce((sum, status) => sum + (stats.countsByStatus?.[status] || 0), 0);
    const done = count(AgentProcessingStatus.FINISHED);
    const error = count(AgentProcessingStatus.ERROR, AgentProcessingStatus.START_FAILED);
    const running = count(AgentProcessingStatus.PROCESSING);
    const pending = count(AgentProcessingStatus.QUEUED, AgentProcessingStatus.PENDING);
    // The endpoint's own total, not the sum of the buckets: if a status is ever added that none of
    // them cover, the run stays correctly unfinished instead of reading as 100% done.
    return { total: stats.total, done, error, running, pending };
  }
}

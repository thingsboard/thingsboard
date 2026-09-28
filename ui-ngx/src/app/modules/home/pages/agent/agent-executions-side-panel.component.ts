// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { Subject, Subscription, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  AgentAppEventInfo,
  AgentApplication,
  AgentProcessingStatus,
  agentProcessingStatusTranslationMap,
  AgentAppProfile,
  AgentBulkAction,
  AgentBulkActionStatus,
  AgentProfileInfo
} from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';

const NON_TERMINAL_BULK: ReadonlyArray<AgentBulkActionStatus> = [
  AgentBulkActionStatus.QUEUED,
  AgentBulkActionStatus.IN_PROGRESS
];

type StatusFilter = 'ALL' | 'ERROR' | 'RUNNING';

interface StatusCounts {
  total: number;
  done: number;
  error: number;
  running: number;
  pending: number;
}

@Component({
  selector: 'tb-agent-executions-side-panel',
  templateUrl: './agent-executions-side-panel.component.html',
  styleUrls: ['./agent-executions-side-panel.component.scss'],
  standalone: false
})
export class AgentExecutionsSidePanelComponent implements OnChanges, OnDestroy {

  @Input() agentProfile: AgentProfileInfo;
  @Input() appProfile: AgentAppProfile;
  @Input() bulkAction: AgentBulkAction;

  @Output() closePanel = new EventEmitter<void>();
  @Output() openFullDetails = new EventEmitter<void>();

  events: AgentAppEventInfo[] = [];
  counts: StatusCounts = { total: 0, done: 0, error: 0, running: 0, pending: 0 };
  filter: StatusFilter = 'ALL';
  loading = false;

  pageIndex = 0;
  pageSize = 10;
  readonly pageSizeOptions = [10, 25, 50, 100];

  private readonly destroy$ = new Subject<void>();
  private pollSub: Subscription | null = null;

  constructor(private agentService: AgentService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private translate: TranslateService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.bulkAction) {
      this.stopPolling();
      this.pageIndex = 0;
      if (this.bulkAction) {
        this.refresh();
        if (NON_TERMINAL_BULK.includes(this.bulkAction.status)) {
          this.startPolling();
        }
      }
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredEvents(): AgentAppEventInfo[] {
    switch (this.filter) {
      case 'ERROR':
        return this.events.filter(e =>
          e.processingStatus === AgentProcessingStatus.ERROR ||
          e.processingStatus === AgentProcessingStatus.START_FAILED);
      case 'RUNNING':
        return this.events.filter(e => e.processingStatus === AgentProcessingStatus.PROCESSING);
      default:
        return this.events;
    }
  }

  get pagedEvents(): AgentAppEventInfo[] {
    const start = this.pageIndex * this.pageSize;
    return this.filteredEvents.slice(start, start + this.pageSize);
  }

  onPageChange(e: { pageIndex: number; pageSize: number }): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
  }

  get successPct(): number {
    return this.counts.total === 0
      ? 0
      : Math.round((this.counts.done / this.counts.total) * 100);
  }

  get donePct(): number {
    return this.pctOf(this.counts.done);
  }
  get errorPct(): number {
    return this.pctOf(this.counts.error);
  }
  get runningPct(): number {
    return this.pctOf(this.counts.running);
  }

  setFilter(f: StatusFilter): void {
    this.filter = f;
    this.pageIndex = 0;
  }

  close(): void {
    this.closePanel.emit();
  }

  openDetails(): void {
    this.openFullDetails.emit();
  }

  refreshNow(): void {
    this.refresh();
  }

  isEventClickable(event: AgentAppEventInfo): boolean {
    return !!event.applicationId
      && (event.processingStatus === AgentProcessingStatus.PENDING
        || event.processingStatus === AgentProcessingStatus.QUEUED
        || event.processingStatus === AgentProcessingStatus.PROCESSING);
  }

  openEventProgress(event: AgentAppEventInfo): void {
    if (!this.isEventClickable(event)) {
      return;
    }
    this.agentService.getAgentApplicationInfoById(event.applicationId.id, { ignoreErrors: true }).subscribe({
      next: application => this.openProgressDialog(application, event),
      error: () => this.openProgressDialog(null, event)
    });
  }

  private openProgressDialog(application: AgentApplication | null, event: AgentAppEventInfo): void {
    this.dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
      AgentAppEventProgressDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { application, event }
      }
    ).afterClosed().subscribe(() => this.refresh());
  }

  formatTime(ts: number): string {
    if (!ts) { return ''; }
    return this.datePipe.transform(ts, 'HH:mm:ss') || '';
  }

  statusLabel(status: AgentProcessingStatus): string {
    const key = agentProcessingStatusTranslationMap.get(status);
    return key ? this.translate.instant(key) : String(status);
  }

  statusColor(status: AgentProcessingStatus): string {
    switch (status) {
      case AgentProcessingStatus.FINISHED: return '#2e7d32';
      case AgentProcessingStatus.ERROR:
      case AgentProcessingStatus.START_FAILED: return '#c62828';
      case AgentProcessingStatus.PROCESSING: return '#1565c0';
      case AgentProcessingStatus.QUEUED:
      case AgentProcessingStatus.PENDING:
      default: return '#616161';
    }
  }

  private pctOf(n: number): number {
    return this.counts.total === 0 ? 0 : (n / this.counts.total) * 100;
  }

  private clampPageIndex(): void {
    const total = this.filteredEvents.length;
    const maxIndex = Math.max(0, Math.ceil(total / this.pageSize) - 1);
    if (this.pageIndex > maxIndex) {
      this.pageIndex = maxIndex;
    }
  }

  private refresh(): void {
    if (!this.bulkAction) { return; }
    this.loading = true;
    const pageLink = new PageLink(100, 0, null, { property: 'updatedTime', direction: Direction.DESC });
    this.agentService.getAgentBulkActionEvents(this.bulkAction.id.id, pageLink).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: page => {
        this.events = page.data;
        this.counts = this.computeCounts(this.events, this.bulkAction.total);
        this.clampPageIndex();
        this.loading = false;
        if (this.allEventsTerminal()) {
          this.stopPolling();
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private allEventsTerminal(): boolean {
    // Nothing dispatched yet — keep polling until events start arriving.
    if (!this.events.length || this.counts.pending > 0 || this.counts.running > 0) {
      return false;
    }
    return this.events.every(e =>
      e.processingStatus === AgentProcessingStatus.FINISHED ||
      e.processingStatus === AgentProcessingStatus.ERROR ||
      e.processingStatus === AgentProcessingStatus.START_FAILED);
  }

  private computeCounts(events: AgentAppEventInfo[], total: number): StatusCounts {
    const done = events.filter(e => e.processingStatus === AgentProcessingStatus.FINISHED).length;
    const error = events.filter(e =>
      e.processingStatus === AgentProcessingStatus.ERROR ||
      e.processingStatus === AgentProcessingStatus.START_FAILED).length;
    const running = events.filter(e => e.processingStatus === AgentProcessingStatus.PROCESSING).length;
    const submitted = done + error + running +
      events.filter(e =>
        e.processingStatus === AgentProcessingStatus.QUEUED ||
        e.processingStatus === AgentProcessingStatus.PENDING).length;
    const pending = Math.max(0, (total || submitted) - submitted);
    return {
      total: total || submitted,
      done,
      error,
      running,
      pending
    };
  }

  private startPolling(): void {
    this.pollSub = timer(3000, 3000).pipe(takeUntil(this.destroy$)).subscribe(() => this.refresh());
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }
}

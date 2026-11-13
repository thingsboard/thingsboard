// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { orderedStepsForAction } from '@home/pages/agent/util/agent-app-steps';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentApplication,
  AgentAppEvent,
  AgentAppEventActionType,
  agentAppEventActionTypeTranslationMap,
  AgentProcessingStatus,
  agentProcessingStatusTranslationMap,
  AgentAppStep,
  AgentAppStepType,
  AgentAppTemplate,
  AgentAppConfigType,
  isAgentScopedAppEventActionType
} from '@shared/models/agent.models';
import { DialogService } from '@core/services/dialog.service';
import { Subscription, timer } from 'rxjs';

export interface AgentAppEventProgressDialogData {
  application: AgentApplication | null;
  event: AgentAppEvent;
}

interface ProgressStepView {
  step: AgentAppStep;
  index: number;
  state: 'completed' | 'processing' | 'pending' | 'error';
}

/**
 * An agent-scoped upgrade has no application and no template: its two steps are synthesized by the
 * server rather than declared anywhere, so the dialog declares the same pair to render against.
 */
const AGENT_UPGRADE_STEPS: ReadonlyArray<{ type: AgentAppStepType; titleKey: string }> = [
  { type: AgentAppStepType.AGENT_PREPARE, titleKey: 'agent.agent-upgrade-step-prepare' },
  { type: AgentAppStepType.AGENT_FINALIZE, titleKey: 'agent.agent-upgrade-step-finalize' }
];

const TERMINAL_STATUSES: ReadonlyArray<AgentProcessingStatus> = [
  AgentProcessingStatus.FINISHED,
  AgentProcessingStatus.ERROR,
  AgentProcessingStatus.START_FAILED
];

@Component({
  selector: 'tb-agent-app-event-progress-dialog',
  templateUrl: './agent-app-event-progress-dialog.component.html',
  styleUrls: ['./agent-app-event-progress-dialog.component.scss'],
  standalone: false
})
export class AgentAppEventProgressDialogComponent
  extends DialogComponent<AgentAppEventProgressDialogComponent, boolean>
  implements OnInit, OnDestroy {

  application: AgentApplication | null;
  event: AgentAppEvent;
  template: AgentAppTemplate | null = null;
  steps: ProgressStepView[] = [];
  loading = true;
  loadError = '';

  agentAppEventActionTypeTranslationMap = agentAppEventActionTypeTranslationMap;
  agentProcessingStatusTranslationMap = agentProcessingStatusTranslationMap;

  private pollSub: Subscription | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private dialogService: DialogService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppEventProgressDialogData,
              public dialogRef: MatDialogRef<AgentAppEventProgressDialogComponent, boolean>) {
    super(store, router, dialogRef);
    this.application = data.application;
    this.event = data.event;
  }

  ngOnInit(): void {
    if (this.isAgentScoped) {
      // The agent upgrades itself: there is no application to look a template up by, but the event
      // is live and must be followed, so the steps are synthesized and polling still runs.
      this.rebuildSteps();
      this.loading = false;
      this.startPollingIfNeeded();
      return;
    }
    const templateVersion = this.application?.templateVersion;
    if (!templateVersion) {
      // Orphan event (application removed) or missing template — render read-only
      // with whatever the event already carries. No polling.
      this.loading = false;
      return;
    }
    this.agentService.getAgentAppTemplateByVersion(this.application.appType, AgentAppConfigType.DOCKER_COMPOSE, templateVersion).subscribe({
      next: tpl => {
        this.template = tpl;
        this.rebuildSteps();
        this.loading = false;
        this.startPollingIfNeeded();
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-event-progress-load-failed');
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  get isAgentScoped(): boolean {
    return !!this.event?.actionType && isAgentScopedAppEventActionType(this.event.actionType);
  }

  /** Agent-scoped events have no application, so the header names the action instead. */
  get subjectName(): string {
    if (this.isAgentScoped) {
      return this.translate.instant('agent.agent-upgrade-subject');
    }
    return this.application?.name || this.event?.applicationName || this.translate.instant('agent.app-deleted');
  }

  get statusKey(): string {
    return this.agentProcessingStatusTranslationMap.get(this.event?.processingStatus) || this.event?.processingStatus || '';
  }

  get actionKey(): string {
    return this.agentAppEventActionTypeTranslationMap.get(this.event?.actionType) || this.event?.actionType || '';
  }

  get statusBadgeClass(): string {
    if (!this.event?.processingStatus) {
      return '';
    }
    return this.event.processingStatus.toLowerCase();
  }

  get isTerminal(): boolean {
    return TERMINAL_STATUSES.includes(this.event?.processingStatus);
  }

  get hasError(): boolean {
    return (this.event?.processingStatus === AgentProcessingStatus.ERROR
        || this.event?.processingStatus === AgentProcessingStatus.START_FAILED)
      && !!this.event?.errorMessage;
  }

  get canCancel(): boolean {
    if (!this.application?.id?.id) { return false; }
    const s = this.event?.processingStatus;
    return s === AgentProcessingStatus.PENDING
        || s === AgentProcessingStatus.QUEUED
        || s === AgentProcessingStatus.PROCESSING;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  viewInEvents($event: Event): void {
    if ($event) { $event.preventDefault(); $event.stopPropagation(); }
    const agentId = this.application?.agentId?.id || this.event?.agentId?.id;
    if (!agentId) { return; }
    this.dialogRef.close(false);
    this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, 'events'));
  }

  cancelEvent($event: Event): void {
    if ($event) { $event.stopPropagation(); }
    if (!this.event?.id?.id) { return; }
    this.dialogService.confirm(
      this.translate.instant('agent.app-event-cancel-title'),
      this.translate.instant('agent.app-event-cancel-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.cancelAgentAppEvent(this.application.id.id, this.event.id.id).subscribe(() => {
          this.refreshEventOnce();
        });
      }
    });
  }

  private startPollingIfNeeded(): void {
    if (this.isTerminal || this.pollSub) {
      return;
    }
    this.pollSub = timer(3000, 3000).subscribe(() => this.refreshEventOnce());
  }

  private stopPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = null;
    }
  }

  private refreshEventOnce(): void {
    if (!this.event?.id?.id) { return; }
    this.agentService.getAgentAppEventById(this.event.id.id,
      { ignoreLoading: true, ignoreErrors: true }).subscribe(fresh => {
      if (fresh) {
        this.event = fresh;
        this.rebuildSteps();
        if (this.isTerminal) {
          this.stopPolling();
        }
      }
    });
  }

  private rebuildSteps(): void {
    if (this.isAgentScoped) {
      this.rebuildAgentUpgradeSteps();
      return;
    }
    if (!this.template) {
      this.steps = [];
      return;
    }
    const ordered = orderedStepsForAction(this.template, this.event.actionType);
    const currentId = this.event.currentStepId;
    const processingStatus = this.event.processingStatus;
    const currentIdx = currentId ? ordered.findIndex(s => s.id === currentId) : -1;
    this.steps = ordered.map((step, i) => {
      let state: ProgressStepView['state'];
      if (currentIdx < 0) {
        state = 'pending';
      } else if (i < currentIdx) {
        state = 'completed';
      } else if (i === currentIdx) {
        state = (processingStatus === AgentProcessingStatus.ERROR || processingStatus === AgentProcessingStatus.START_FAILED)
          ? 'error' : 'processing';
      } else {
        state = 'pending';
      }
      if (processingStatus === AgentProcessingStatus.FINISHED) {
        state = 'completed';
      }
      return { step, index: i + 1, state };
    });
  }

  /**
   * Which of the two synthetic steps is running is told by the event's own status rather than by
   * currentStepId: the server generates those ids per event, so there is nothing stable to match
   * them against. Prepare is done as soon as anything past it is happening.
   */
  private rebuildAgentUpgradeSteps(): void {
    const status = this.event?.processingStatus;
    const failed = status === AgentProcessingStatus.ERROR || status === AgentProcessingStatus.START_FAILED;
    const finished = status === AgentProcessingStatus.FINISHED;
    const activeIdx = this.activeAgentUpgradeStepIndex();
    this.steps = AGENT_UPGRADE_STEPS.map((declared, i) => {
      let state: ProgressStepView['state'];
      if (finished) {
        state = 'completed';
      } else if (i < activeIdx) {
        state = 'completed';
      } else if (i === activeIdx) {
        state = failed ? 'error' : 'processing';
      } else {
        state = 'pending';
      }
      return {
        step: {
          id: declared.type,
          title: this.translate.instant(declared.titleKey),
          type: declared.type
        } as AgentAppStep,
        index: i + 1,
        state
      };
    });
  }

  private activeAgentUpgradeStepIndex(): number {
    const activity = this.event?.currentActivity || '';
    return activity.includes(AgentAppStepType.AGENT_FINALIZE) ? 1 : 0;
  }
}

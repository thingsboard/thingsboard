// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppStep,
  AgentAppStepState,
  AgentAppTemplate,
  AgentProfileInfo,
  BulkOperationPreview,
  BulkOperationRequest,
  BulkOperationResult,
  SkippedApp,
  SkipReason
} from '@shared/models/agent.models';
import { StepBinding as SharedStepBinding, VolumeChoice } from '@home/pages/agent/util/agent-app-step-inputs';
import {
  buildBackupVolumeInput,
  buildComposeDownInput,
  buildPullImagesInput,
  classifyStepsForAction,
  ClassifiedStep,
  extractComposeVolumeKeys,
  readInitialPullImages,
  StepInputKind
} from '@home/pages/agent/util/agent-app-steps';

export interface AgentProfileBulkActionDialogData {
  agentProfile: AgentProfileInfo;
  profile: AgentAppProfile;
  actionType: AgentAppEventActionType;
  // For UPGRADE: every source-version template pointing at the profile's
  // version; eligible apps may sit on different source versions, so the
  // dialog surfaces the union of their user-input steps.
  templates?: AgentAppTemplate[] | null;
  preview: BulkOperationPreview;
}

// One rendered input block per step kind. The same logical choice may live
// under different step ids across source-version templates (e.g. the DB-hop
// vs patch-hop pull-images step), so the chosen value fans out to every id in
// `stepIds`. Per-kind local state lives on optional fields; only the field
// matching `kind` is populated and read.
export interface StepBinding extends SharedStepBinding {
  stepIds: string[];
}

@Component({
  selector: 'tb-agent-profile-bulk-action-dialog',
  templateUrl: './agent-profile-bulk-action-dialog.component.html',
  styleUrls: ['./agent-profile-bulk-action-dialog.component.scss'],
  standalone: false
})
export class AgentProfileBulkActionDialogComponent
  extends DialogComponent<AgentProfileBulkActionDialogComponent, BulkOperationResult>
  implements OnDestroy {

  private readonly destroy$ = new Subject<void>();

  agentProfile: AgentProfileInfo;
  profile: AgentAppProfile;
  actionType: AgentAppEventActionType;

  submitting = false;
  previewLoaded = false;
  preview: BulkOperationPreview | null = null;

  // Pre-computed from `preview.skippedSample` so the template doesn't re-filter
  // on every change-detection cycle. Keys are SkipReason values.
  skippedByReasonMap: Partial<Record<SkipReason, SkippedApp[]>> = {};
  skippedLinkMap = new Map<string, string[]>();
  totalSkipped = 0;

  // One binding per user-input step kind surfaced by the templates, in BE
  // order; same-kind steps from other source versions merge into one block.
  bindings: StepBinding[] = [];
  profileVolumeKeys: string[] = [];

  readonly ActionType = AgentAppEventActionType;
  readonly SkipReason = SkipReason;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileBulkActionDialogData,
              public dialogRef: MatDialogRef<AgentProfileBulkActionDialogComponent, BulkOperationResult>) {
    super(store, router, dialogRef);
    this.agentProfile = data.agentProfile;
    this.profile = data.profile;
    this.actionType = data.actionType;
    this.profileVolumeKeys = extractComposeVolumeKeys(this.profile);
    if (data.templates?.length) {
      this.initStepsFromTemplates(data.templates);
    }
    this.preview = data.preview;
    this.hydratePreview(data.preview);
    this.previewLoaded = true;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  get titleKey(): string {
    switch (this.actionType) {
      case AgentAppEventActionType.RESTART: return 'agent.bulk-restart-title';
      case AgentAppEventActionType.UPDATE:  return 'agent.bulk-update-title';
      case AgentAppEventActionType.UPGRADE: return 'agent.bulk-upgrade-title';
      case AgentAppEventActionType.DELETE:  return 'agent.bulk-delete-title';
      default: return 'agent.bulk-action-title';
    }
  }

  get confirmKey(): string {
    switch (this.actionType) {
      case AgentAppEventActionType.RESTART: return 'agent.bulk-restart-cta';
      case AgentAppEventActionType.UPDATE:  return 'agent.bulk-update-cta';
      case AgentAppEventActionType.UPGRADE: return 'agent.bulk-upgrade-cta';
      case AgentAppEventActionType.DELETE:  return 'agent.bulk-delete-cta';
      default: return 'action.confirm';
    }
  }

  get confirmColor(): 'primary' | 'warn' | 'accent' {
    return this.actionType === AgentAppEventActionType.DELETE ? 'warn' :
           this.actionType === AgentAppEventActionType.UPGRADE ? 'accent' : 'primary';
  }

  get confirmDisabled(): boolean {
    return this.submitting || !this.preview || this.preview.eligible === 0;
  }

  skippedByReason(reason: SkipReason): SkippedApp[] {
    return this.skippedByReasonMap[reason] || [];
  }

  skippedCount(reason: SkipReason): number {
    return this.preview?.skippedCountsByReason?.[reason] ?? 0;
  }

  skippedExtraCount(reason: SkipReason): number {
    return Math.max(0, this.skippedCount(reason) - this.skippedByReason(reason).length);
  }

  appLink(s: SkippedApp): string[] | null {
    const key = s.applicationId?.id;
    return key ? (this.skippedLinkMap.get(key) ?? null) : null;
  }

  // Close the dialog first, then navigate. Using [routerLink] with an extra
  // (click)="cancel()" races the close animation against the route change —
  // the dialog fades out over the new page, looking like a stuck overlay.
  navigateToApp(link: string[], $event: Event) {
    if ($event) { $event.preventDefault(); }
    this.dialogRef.close(null);
    this.router.navigate(link);
  }

  private hydratePreview(preview: BulkOperationPreview) {
    const byReason: Partial<Record<SkipReason, SkippedApp[]>> = {};
    const linkMap = new Map<string, string[]>();
    for (const s of preview.skippedSample || []) {
      (byReason[s.reason] ||= []).push(s);
      if (s.agentId?.id && s.applicationId?.id) {
        linkMap.set(s.applicationId.id,
          [agentEntityUrl(currentAgentRouteSnapshot(this.router), s.agentId.id, 'applications', s.applicationId.id)]);
      }
    }
    this.skippedByReasonMap = byReason;
    this.skippedLinkMap = linkMap;
    const counts = preview.skippedCountsByReason || {};
    this.totalSkipped = Object.values(counts).reduce((sum, n) => sum + (n || 0), 0);
  }

  toggleBackupVolume(v: VolumeChoice) {
    v.selected = !v.selected;
  }

  cancel() {
    this.dialogRef.close(null);
  }

  confirm() {
    if (this.confirmDisabled) {
      return;
    }
    this.submitting = true;
    const request: BulkOperationRequest = {
      actionType: this.actionType,
      stepInputs: this.buildStepInputs()
    };
    this.agentService.bulkOperation(this.agentProfile.id.id, this.profile.id.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.submitting = false
      });
  }

  private initStepsFromTemplates(templates: AgentAppTemplate[]) {
    const seenStepIds = new Set<string>();
    const bindings: StepBinding[] = [];
    const byKind = new Map<StepInputKind, StepBinding>();
    for (const template of templates) {
      for (const cs of classifyStepsForAction(template, this.actionType)) {
        if (seenStepIds.has(cs.step.id)) {
          continue;
        }
        seenStepIds.add(cs.step.id);
        const existing = byKind.get(cs.kind);
        if (existing) {
          existing.stepIds.push(cs.step.id);
          continue;
        }
        const binding = this.createBinding(cs);
        byKind.set(cs.kind, binding);
        bindings.push(binding);
      }
    }
    this.bindings = bindings;
  }

  private createBinding({ kind, step }: ClassifiedStep): StepBinding {
    switch (kind) {
      case 'backupVolume':
        return {
          kind, step, stepIds: [step.id],
          backupVolumes: this.profileVolumeKeys.map(k => ({ key: k, selected: false }))
        };
      case 'pullImages':
        return { kind, step, stepIds: [step.id], pullImages: readInitialPullImages(step) };
      case 'composeDown':
        return { kind, step, stepIds: [step.id], removeVolumes: false };
    }
  }

  private buildStepInputs(): { [stepId: string]: AgentAppStepState } {
    const stepInputs: { [stepId: string]: AgentAppStepState } = {};
    for (const b of this.bindings) {
      const input = this.buildBindingInput(b);
      for (const stepId of b.stepIds) {
        stepInputs[stepId] = input;
      }
    }
    return stepInputs;
  }

  private buildBindingInput(b: StepBinding): AgentAppStepState {
    switch (b.kind) {
      case 'backupVolume':
        return buildBackupVolumeInput(
          b.step,
          (b.backupVolumes || []).filter(v => v.selected).map(v => v.key)
        );
      case 'pullImages':
        return buildPullImagesInput(b.step, !!b.pullImages);
      case 'composeDown':
        return buildComposeDownInput(b.step, !!b.removeVolumes);
    }
  }
}

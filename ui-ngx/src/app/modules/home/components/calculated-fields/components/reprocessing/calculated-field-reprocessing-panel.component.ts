// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Input, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EntityId } from '@shared/models/id/entity-id';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import {
  calculateTsOffset,
  DAY,
  getTimePageLinkInterval,
  historyInterval,
  SECOND
} from '@shared/models/time/time.models';
import { interval, Subject, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { CancelTaskDialogComponent, CancelTaskDialogData } from '@home/components/task/cancel-task-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { JobService } from '@core/http/job.service';
import { Job, JobStatus, processTask, workingTask } from '@shared/models/job.models';
import { switchMap, takeUntil, takeWhile } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull, parseHttpErrorMessage } from '@core/utils';
import { ThemePalette } from '@angular/material/core';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';

enum ReprocessingState {
  LOADING = 'loading',
  CONFIG = 'config',
  VALIDATION_ERROR = 'validationError',
  SUBMIT = 'submit',
  PROCESS = 'process',
  RESULTS = 'results',
}

@Component({
    selector: 'tb-calculated-field-reprocessing-panel',
    templateUrl: './calculated-field-reprocessing-panel.component.html',
    styleUrls: ['./calculated-field-reprocessing-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CalculatedFieldReprocessingPanelComponent implements OnInit {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  originatorId: EntityId;

  state = signal<ReprocessingState>(ReprocessingState.LOADING);
  submitProgress = signal(0);
  processProgress = signal(0);
  processInfo = signal('');
  job = signal<Job | null>(null);

  timeWindow = historyInterval(7 * DAY);

  validationMgs: string;

  reprocessingLabel  = 'calculated-fields.reprocessing-in-progress';

  JobStatus = JobStatus;
  ReprocessingState = ReprocessingState;

  failedTasks = 0;
  totalTasks = 0;

  resultLabel: string;
  resultIcon: string;
  resultIconColor: ThemePalette;

  hasWritePermission = false;

  private showEntityProcessing = false;
  private destroy$ = new Subject<void>();

  private readonly submitDuration = 10 * SECOND;
  private readonly processDuration = 60 * SECOND;

  constructor(private popover: TbPopoverComponent<CalculatedFieldReprocessingPanelComponent>,
              private calculatedFieldsService: CalculatedFieldsService,
              private router: Router,
              private destroyRef: DestroyRef,
              private translate: TranslateService,
              private jobService: JobService,
              private dialog: MatDialog,
              private userPermissionsService: UserPermissionsService) {
    this.hasWritePermission = this.userPermissionsService.hasGenericPermission(Resource.JOB, Operation.WRITE);
  }

  ngOnInit() {
    this.calculatedFieldsService.validateCalculatedFieldReprocessing(this.entityId.id, {ignoreLoading: true}).subscribe({
      next: (value) => {
        if (value.isValid) {
          this.state.set(ReprocessingState.CONFIG);
        } else if (value.lastJobStatus) {
          this.findJob();
        } else {
          this.validationMgs = value.message;
          this.state.set(ReprocessingState.VALIDATION_ERROR);
        }
        if (this.originatorId.entityType === EntityType.ASSET_PROFILE || this.originatorId.entityType === EntityType.DEVICE_PROFILE) {
          this.reprocessingLabel = 'calculated-fields.reprocessing-entities-data';
          this.showEntityProcessing = true;
        }
      },
      error: (eroor) => {
        this.validationMgs = eroor.error;
        this.state.set(ReprocessingState.VALIDATION_ERROR);
      }
    });
  }

  cancel() {
    this.popover.hide();
  }

  reprocessing() {
    if (!this.job()) {
      this.startProgress('submitProgress', this.submitDuration);
      const interval = getTimePageLinkInterval(this.timeWindow);
      const tsOffset = calculateTsOffset(this.timeWindow.timezone);
      this.calculatedFieldsService.reprocessCalculatedField(this.entityId.id, interval.startTime + tsOffset, interval.endTime + tsOffset, {ignoreLoading: true, ignoreErrors: true})
        .subscribe({
          next: () => {
            this.completeProgress('submitProgress');
            this.findJob();
          },
          error: (error) => {
            this.validationMgs = parseHttpErrorMessage(error, this.translate).message;
            this.state.set(ReprocessingState.VALIDATION_ERROR);
          }
        });
    } else {
      this.startProgress('processProgress', this.processDuration);
      this.processInfo.set('');
      this.jobService.reprocessJob(this.job().id.id, {ignoreLoading: true}).subscribe(() => {
        this.waitJobResult(true);
      });
    }
  }

  openDetails() {
    this.router.navigate(['/features/taskManager'], {
      queryParams: {entityId: encodeURIComponent(JSON.stringify(this.originatorId))}
    }).then(() => {});
  }

  cancelReprocessing($event: Event) {
    $event?.stopPropagation();
    this.dialog.open<CancelTaskDialogComponent, CancelTaskDialogData, boolean>(CancelTaskDialogComponent, {
      disableClose: true,
      data: {
        title: this.translate.instant('task.cancel-task-calculated-field-reprocessing-title'),
        message: this.translate.instant('task.cancel-task-calculated-field-reprocessing-text')
      },
      panelClass: ['tb-fullscreen-dialog']
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.popover.hide();
        this.jobService.cancelJob(this.job().id.id).subscribe();
      }
    });
  }


  private findJob() {
    this.startProgress('processProgress', this.processDuration);
    this.calculatedFieldsService.getLastCalculatedFieldReprocessingJob(this.entityId.id, {ignoreLoading: true}).subscribe((job) => {
      this.job.set(job);
      if (!workingTask.includes(this.job().status)) {
        this.completeProgress('processProgress');
        this.calculateProgress();
      } else {
        this.waitJobResult();
      }
    });
  }

  private startProgress(progressProp: 'submitProgress' | 'processProgress', duration: number) {
    this.state.set(progressProp === 'submitProgress' ? ReprocessingState.SUBMIT : ReprocessingState.PROCESS);
    const intervalTime = 100;
    const steps = duration / intervalTime;
    const increment = 100 / steps;
    this[progressProp].set(0);

    interval(intervalTime).pipe(
      takeUntil(this.destroy$),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this[progressProp].update((value) => Math.min(value + increment, 99));
      if (this[progressProp]() >= 99) {
        this.destroy$.next();
      }
    });
  }

  private completeProgress(progressProp: 'submitProgress' | 'processProgress') {
    this.destroy$.next();
    this[progressProp].set(100);
    if (progressProp === 'processProgress') {
      this.openResult();
    }
  }

  private calculateProgress() {
    if (this.showEntityProcessing) {
      const total = this.job().result.totalCount;
      if (isDefinedAndNotNull(total)) {
        const process = processTask(this.job().result);
        const progress = process > 0 ? Math.round(process / total * 100) : (total > 0 ? 0 : 100);
        if (progress > this.processProgress()) {
          this.processProgress.set(progress);
        }
        this.processInfo.set(`${process}/${total} (${progress}%)`);
      }
    }
  }

  private waitJobResult(ignoreStartDelay = false) {
    timer((ignoreStartDelay ? 0 : 1000), 2000).pipe(
      switchMap(() => this.jobService.getJobById(this.job().id.id, {ignoreLoading: true})),
      takeWhile((res) => workingTask.includes(res.status), true),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((res) => {
      this.job.set(res);
      this.calculateProgress();
      if (!workingTask.includes(res.status)) {
        this.completeProgress('processProgress');
      }
    });
  }

  private openResult() {
    this.state.set(ReprocessingState.RESULTS);
    this.totalTasks = this.job().result.totalCount;
    this.failedTasks = this.job().result.failedCount;
    switch (this.job().status) {
      case JobStatus.COMPLETED:
        this.resultLabel = 'calculated-fields.reprocessing-completed';
        this.resultIcon = 'check_circle_outline';
        this.resultIconColor = 'primary';
        break;
      case JobStatus.CANCELLED:
        this.resultLabel = 'calculated-fields.reprocessing-cancelled';
        this.resultIcon = 'warning';
        this.resultIconColor = undefined;
        break;
      case JobStatus.FAILED:
        this.resultLabel = 'calculated-fields.reprocessing-failed';
        this.resultIcon = 'warning';
        this.resultIconColor = undefined;
        break;
    }
  }
}

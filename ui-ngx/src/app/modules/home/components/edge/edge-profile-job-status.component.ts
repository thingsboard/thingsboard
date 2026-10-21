// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription, switchMap, takeWhile, timer } from 'rxjs';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { JobService } from '@core/http/job.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import {
  EdgeProfileApplyOp,
  edgeProfileApplyOpTranslations,
  Job,
  JobStatus,
  processTask,
  workingTask
} from '@shared/models/job.models';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import {
  CancelTaskDialogComponent,
  CancelTaskDialogData
} from '@home/components/task/cancel-task-dialog.component';

@Component({
  selector: 'tb-edge-profile-job-status',
  templateUrl: './edge-profile-job-status.component.html',
  styleUrls: ['./edge-profile-job-status.component.scss'],
  standalone: false
})
export class EdgeProfileJobStatusComponent implements OnChanges {

  @Input()
  edgeProfileId: string;

  readonly JobStatus = JobStatus;

  job: Job;
  readonly hasWritePermission: boolean;

  private polling: Subscription;

  constructor(private edgeProfileService: EdgeProfileService,
              private jobService: JobService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private router: Router,
              private destroyRef: DestroyRef) {
    this.hasWritePermission = this.userPermissionsService.hasGenericPermission(Resource.JOB, Operation.WRITE);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.edgeProfileId) {
      this.refresh();
    }
  }

  refresh(): void {
    this.polling?.unsubscribe();
    if (!this.edgeProfileId) {
      this.job = null;
      return;
    }
    this.edgeProfileService.getLatestEdgeProfileJob(this.edgeProfileId, {ignoreLoading: true, ignoreErrors: true})
      .subscribe(job => this.onJob(job));
  }

  get working(): boolean {
    return !!this.job && workingTask.includes(this.job.status);
  }

  get progress(): number {
    const total = this.job?.result?.totalCount;
    if (!total) {
      return 0;
    }
    return Math.round(processTask(this.job.result) / total * 100);
  }

  get processed(): number {
    return this.job ? processTask(this.job.result) : 0;
  }

  get total(): number {
    return this.job?.result?.totalCount ?? 0;
  }

  get opLabel(): string {
    const op = this.job?.configuration?.op as EdgeProfileApplyOp;
    return op ? this.translate.instant(edgeProfileApplyOpTranslations.get(op)) : '';
  }

  retry(): void {
    this.jobService.reprocessJob(this.job.id.id, {ignoreLoading: true}).subscribe(() => this.poll(0));
  }

  cancel(): void {
    this.dialog.open<CancelTaskDialogComponent, CancelTaskDialogData, boolean>(CancelTaskDialogComponent, {
      disableClose: true,
      data: {
        title: this.translate.instant('task.cancel-task-edge-profile-apply-title'),
        message: this.translate.instant('task.cancel-task-edge-profile-apply-text')
      },
      panelClass: ['tb-fullscreen-dialog']
    }).afterClosed().subscribe(result => {
      if (result) {
        this.jobService.cancelJob(this.job.id.id, {ignoreLoading: true}).subscribe(() => this.poll(0));
      }
    });
  }

  openDetails(): void {
    this.router.navigate(['/features/taskManager'], {
      queryParams: {entityId: encodeURIComponent(JSON.stringify({entityType: EntityType.EDGE_PROFILE, id: this.edgeProfileId}))}
    }).then(() => {});
  }

  private onJob(job: Job): void {
    this.job = job;
    if (this.working) {
      this.poll(1000);
    }
  }

  private poll(startDelay: number): void {
    this.polling?.unsubscribe();
    this.polling = timer(startDelay, 2000).pipe(
      switchMap(() => this.jobService.getJobById(this.job.id.id, {ignoreLoading: true, ignoreErrors: true})),
      takeWhile(job => workingTask.includes(job.status), true),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(job => this.job = job);
  }

}

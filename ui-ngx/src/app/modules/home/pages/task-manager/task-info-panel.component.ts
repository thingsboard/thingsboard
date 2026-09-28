// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { Job, JobStatus, JobType } from '@app/shared/models/job.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { ReportService } from '@core/http/report.service';

interface TaskError {
  typeName: string;
  detailsUrl: string;
  entityName: string;
  error: string;
}

@Component({
    selector: 'tb-task-info',
    templateUrl: './task-info-panel.component.html',
    styleUrls: ['./task-info-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TaskInfoPanelComponent implements OnInit {

  @Input()
  job: Job;

  @Output()
  reprocessTask = new EventEmitter<void>();

  @Output()
  cancelTask = new EventEmitter<void>();

  JobStatus = JobStatus;
  JobType = JobType;

  hasWritePermission = false;
  errors: TaskError[] = [];
  timeTaken: number;

  constructor(private popover: TbPopoverComponent<TaskInfoPanelComponent>,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private reportService: ReportService,) {
    this.hasWritePermission = this.userPermissionsService.hasGenericPermission(Resource.JOB, Operation.WRITE);
  }

  ngOnInit() {
    if (this.job.result.results.length > 0) {
      this.job.result.results.forEach((result) => {
        if (result.failure?.entityInfo) {
          const entityType = result.failure.entityInfo.id.entityType as EntityType;
          const typeName = this.translate.instant(entityTypeTranslations.get(entityType).type);
          const detailsUrl = getEntityDetailsPageURL(result.failure.entityInfo.id.id, entityType);
          this.errors.push({
            entityName: result.failure.entityInfo.name,
            typeName,
            detailsUrl,
            error: result.failure.error
          });
        }
      })
    }
    if (this.job.status === JobStatus.COMPLETED || this.job.status === JobStatus.FAILED) {
      this.timeTaken = this.job.result.finishTs - this.job.result.startTs;
    }
  }

  cancel() {
    this.popover.hide();
  }

  reprocess() {
    this.reprocessTask.emit();
  }

  cancelJob() {
    this.cancelTask.emit();
  }

  downloadReport($event: Event) {
    $event?.stopPropagation();
    this.reportService.downloadReport(this.job.result.report.id.id).subscribe();
  }
}

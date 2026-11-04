// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { EntityType } from '@shared/models/entity-type.models';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { UserId } from '@shared/models/id/user-id';
import { ReportService } from '@core/http/report.service';
import { ReportRequest, reportOriginatorTypes } from '@shared/models/report.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface GenerateReportDialogData {
  reportTemplateId: ReportTemplateId;
}

@Component({
  selector: 'tb-generate-report-dialog',
  templateUrl: './generate-report-dialog.component.html',
  standalone: false
})
export class GenerateReportDialogComponent extends DialogComponent<GenerateReportDialogComponent, boolean> {

  generateReportForm: FormGroup<{
    userId: FormControl<UserId>;
    timezone: FormControl<string>;
    originator: FormControl<EntityId>;
    makePublic: FormControl<boolean>;
  }>;

  entityType = EntityType;

  allowedOriginatorTypes: EntityType[] = reportOriginatorTypes;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: GenerateReportDialogData,
              public dialogRef: MatDialogRef<GenerateReportDialogComponent, boolean>,
              private reportService: ReportService) {
    super(store, router, dialogRef);
    const authUser = getCurrentAuthUser(this.store);
    this.generateReportForm = new FormGroup({
      userId: new FormControl<UserId>(new UserId(authUser.userId), {nonNullable: true, validators: [Validators.required]}),
      timezone: new FormControl<string>(getDefaultTimezone(), {nonNullable: true, validators: [Validators.required]}),
      originator: new FormControl<EntityId>(null),
      makePublic: new FormControl<boolean>(false),
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  generate(): void {
    const {userId, timezone, originator, makePublic} = this.generateReportForm.getRawValue();
    const request: ReportRequest = {
      reportTemplateId: this.data.reportTemplateId,
      userId: userId?.id,
      timezone,
      originator,
      makePublic
    };
    this.reportService.requestReport(request).subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: () => {
        // Keep the dialog open so the user can adjust the input and retry.
        // The failure message is surfaced by the global HTTP error interceptor.
      }
    });
  }
}

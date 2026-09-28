// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ReportTemplateSettings, TbReportFormat } from '@shared/models/report.models';
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormControl } from '@angular/forms';
import { deepTrim } from '@core/utils';

export interface ReportTemplateSettingsDialogData {
  subReport: boolean;
  format: TbReportFormat;
  settings: ReportTemplateSettings;
}

@Component({
    selector: 'tb-report-template-settings-dialog',
    templateUrl: './report-template-settings-dialog.component.html',
    standalone: false
})
export class ReportTemplateSettingsDialogComponent extends DialogComponent<ReportTemplateSettingsDialogComponent, ReportTemplateSettingsDialogData> {

  subReport: boolean;
  format: TbReportFormat;
  settings: ReportTemplateSettings;

  settingsFormControl: FormControl;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ReportTemplateSettingsDialogData,
              public dialogRef: MatDialogRef<ReportTemplateSettingsDialogComponent, ReportTemplateSettingsDialogData>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.subReport = data.subReport;
    this.format = data.format;
    this.settings = data.settings;
    this.settingsFormControl = this.fb.control(this.settings);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const settings = {...this.settings, ...this.settingsFormControl.getRawValue()};
    this.dialogRef.close(deepTrim(settings));
  }
}

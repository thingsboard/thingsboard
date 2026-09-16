// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { DashboardService } from '@core/http/dashboard.service';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface DashboardInfoDialogData {
  dashboard: Dashboard;
}

@Component({
    selector: 'tb-import-dashboard-file-dialog',
    templateUrl: './import-dashboard-file-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class ImportDashboardFileDialogComponent extends DialogComponent<ImportDashboardFileDialogComponent> implements OnInit {

  private dashboard: Dashboard;
  currentFileName: string = '';
  uploadFileFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardInfoDialogData,
              private dashboardService: DashboardService,
              protected dialogRef: MatDialogRef<ImportDashboardFileDialogComponent>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.dashboard = data.dashboard;
  }

  ngOnInit(): void {
    this.uploadFileFormGroup = this.fb.group({
      file: [null]
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save() {
    const fileControl = this.uploadFileFormGroup.get('file');
    if (!fileControl || !fileControl.value) {
      return;
    }

    const dashboardContent = {
      ...fileControl.value,
      description: this.dashboard.configuration.description
    };
    this.dashboard.configuration = dashboardContent;

    this.dashboardService.saveDashboard(this.dashboard).subscribe(() => {
      this.dialogRef.close(true);
    })
  }

  loadDataFromJsonContent(content: string): any {
    try {
      const importData = JSON.parse(content);
      return importData ? importData['configuration'] : importData;
    } catch (err) {
      this.store.dispatch(new ActionNotificationShow({message: err.message, type: 'error'}));
      return null;
    }
  }
}

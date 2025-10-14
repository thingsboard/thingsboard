///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
  styleUrls: []
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

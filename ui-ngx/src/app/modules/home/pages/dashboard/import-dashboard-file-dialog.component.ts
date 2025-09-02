///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {UntypedFormBuilder, UntypedFormGroup} from '@angular/forms';
import {DashboardService} from '@core/http/dashboard.service';
import {Dashboard, DashboardInfo} from '@app/shared/models/dashboard.models';
import {ActionNotificationShow} from '@core/notification/notification.actions';
import {TranslateService} from '@ngx-translate/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {Router} from '@angular/router';

export interface DashboardInfoDialogData {
  dashboard: Dashboard;
}

@Component({
  selector: 'tb-import-dashboard-file-dialog',
  templateUrl: './import-dashboard-file-dialog.component.html',
  styleUrls: []
})
export class ImportDashboardFileDialogComponent extends DialogComponent<ImportDashboardFileDialogComponent> implements OnInit {

  dashboard: Dashboard;
  currentFileName: string = '';
  uploadFileFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardInfoDialogData,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              public dialogRef: MatDialogRef<ImportDashboardFileDialogComponent>,
              public fb: UntypedFormBuilder) {
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

  save(){
    const fileControl = this.uploadFileFormGroup.get('file');
    if(!fileControl || !fileControl.value){
      return;
    }

    const dashboardContent = {
      ...fileControl.value,
      description: this.dashboard.configuration.description
    };
    this.dashboard.configuration = dashboardContent;

    this.dashboardService.saveDashboard(this.dashboard).subscribe(()=>{
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

///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { UntypedFormBuilder } from '@angular/forms';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardInfo } from '@app/shared/models/dashboard.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface MakeDashboardPublicDialogData {
  dashboard: DashboardInfo;
}

@Component({
    selector: 'tb-make-dashboard-public-dialog',
    templateUrl: './make-dashboard-public-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class MakeDashboardPublicDialogComponent extends DialogComponent<MakeDashboardPublicDialogComponent> implements OnInit {

  dashboard: DashboardInfo;

  publicLink: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MakeDashboardPublicDialogData,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              public dialogRef: MatDialogRef<MakeDashboardPublicDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.dashboard = data.dashboard;
    this.publicLink = dashboardService.getPublicDashboardLink(this.dashboard);
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close();
  }


  onPublicLinkCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('dashboard.public-link-copied-message'),
        type: 'success',
        target: 'makeDashboardPublicDialogContent',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

}

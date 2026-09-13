// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

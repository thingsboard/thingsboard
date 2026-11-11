// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder } from '@angular/forms';
import { DashboardService } from '@core/http/dashboard.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityGroupInfo, ShortEntityView } from '@shared/models/entity-group.models';
import { DashboardInfo } from '@shared/models/dashboard.models';

export interface PublicDashboardLinkDialogData {
  dashboard: ShortEntityView | DashboardInfo;
  entityGroup: EntityGroupInfo;
}

@Component({
    selector: 'tb-public-dashboard-link-dialog',
    templateUrl: './public-dashboard-link.dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class PublicDashboardLinkDialogComponent extends DialogComponent<PublicDashboardLinkDialogComponent> implements OnInit {

  dashboard: ShortEntityView | DashboardInfo;
  entityGroup: EntityGroupInfo;

  publicLink: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: PublicDashboardLinkDialogData,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              public dialogRef: MatDialogRef<PublicDashboardLinkDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.dashboard = data.dashboard;
    this.entityGroup = data.entityGroup;
    this.publicLink = dashboardService.getPublicDashboardLink(this.dashboard, this.entityGroup);
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
        target: 'publicDashboardLinkDialogContent',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { Dashboard } from '@shared/models/dashboard.models';
import { IDashboardComponent } from '@home/models/dashboard-component.models';

export interface EmbedDashboardDialogData {
  dashboard: Dashboard;
  state: string;
  title: string;
  hideToolbar: boolean;
  width?: number;
  height?: number;
  parentDashboard?: IDashboardComponent;
}

@Component({
    selector: 'tb-embed-dashboard-dialog',
    templateUrl: './embed-dashboard-dialog.component.html',
    styleUrls: ['./embed-dashboard-dialog.component.scss'],
    standalone: false
})
export class EmbedDashboardDialogComponent extends DialogComponent<EmbedDashboardDialogComponent>
  implements OnInit {

  @ViewChild('dashboardContent', {read: ViewContainerRef, static: true}) dashboardContentContainer: ViewContainerRef;

  dashboard = this.data.dashboard;
  state = this.data.state;
  title = this.data.title;
  hideToolbar = this.data.hideToolbar;
  parentDashboard = this.data.parentDashboard;

  dialogStyle: any = {};

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EmbedDashboardDialogData,
              public dialogRef: MatDialogRef<EmbedDashboardDialogComponent>) {
    super(store, router, dialogRef);
    if (this.data.width) {
      this.dialogStyle.width = this.data.width + 'vw';
    }
    if (this.data.height) {
      this.dialogStyle.height = this.data.height + 'vh';
    }
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close(null);
  }

}

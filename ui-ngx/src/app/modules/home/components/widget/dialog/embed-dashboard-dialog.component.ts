///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {
  Component,
  ComponentFactoryResolver,
  Inject,
  Injector,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { Dashboard } from '@shared/models/dashboard.models';

export interface EmbedDashboardDialogData {
  dashboard: Dashboard;
  state: string;
  title: string;
  hideToolbar: boolean;
  width?: number;
  height?: number;
}

@Component({
  selector: 'tb-embed-dashboard-dialog',
  templateUrl: './embed-dashboard-dialog.component.html',
  styleUrls: ['./embed-dashboard-dialog.component.scss']
})
export class EmbedDashboardDialogComponent extends DialogComponent<EmbedDashboardDialogComponent>
  implements OnInit {

  @ViewChild('dashboardContent', {read: ViewContainerRef, static: true}) dashboardContentContainer: ViewContainerRef;

  dashboard = this.data.dashboard;
  state = this.data.state;
  title = this.data.title;
  hideToolbar = this.data.hideToolbar;

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

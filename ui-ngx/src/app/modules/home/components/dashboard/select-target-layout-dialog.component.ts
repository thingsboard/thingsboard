// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardLayoutId } from '@app/shared/models/dashboard.models';

@Component({
    selector: 'tb-select-target-layout-dialog',
    templateUrl: './select-target-layout-dialog.component.html',
    styleUrls: ['./layout-button.scss'],
    standalone: false
})
export class SelectTargetLayoutDialogComponent extends DialogComponent<SelectTargetLayoutDialogComponent, DashboardLayoutId>
  implements OnInit {

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<SelectTargetLayoutDialogComponent, DashboardLayoutId>) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
  }

  selectLayout(layoutId: DashboardLayoutId) {
    this.dialogRef.close(layoutId);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';

export interface AgentAppAssignCompareDialogData {
  appName: string;
  profileName: string;
  currentYaml: string;
  futureYaml: string;
}

@Component({
  selector: 'tb-agent-app-assign-compare-dialog',
  templateUrl: './agent-app-assign-compare-dialog.component.html',
  styleUrls: ['./agent-app-assign-compare-dialog.component.scss'],
  standalone: false
})
export class AgentAppAssignCompareDialogComponent
  extends DialogComponent<AgentAppAssignCompareDialogComponent, boolean> {

  diffSyncScroll = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppAssignCompareDialogData,
              public dialogRef: MatDialogRef<AgentAppAssignCompareDialogComponent, boolean>) {
    super(store, router, dialogRef);
  }

  cancel() {
    this.dialogRef.close(false);
  }

  confirm() {
    this.dialogRef.close(true);
  }
}

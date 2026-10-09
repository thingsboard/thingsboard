// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';

export type AgentAppVersionWarningContext = 'assign' | 'update';

// false — cancelled; true — proceed anyway; 'upgrade' — user chose the
// "Upgrade first" shortcut offered when the profile targets the app's next version.
export type AgentAppVersionWarningResult = boolean | 'upgrade';

export interface AgentAppVersionWarningDialogData {
  context: AgentAppVersionWarningContext;
  appName: string;
  profileName: string;
  appVersion: string;
  profileVersion: string;
  nextVersion?: string | null;
}

@Component({
  selector: 'tb-agent-app-version-warning-dialog',
  templateUrl: './agent-app-version-warning-dialog.component.html',
  styleUrls: ['./agent-app-version-warning-dialog.component.scss'],
  standalone: false
})
export class AgentAppVersionWarningDialogComponent
  extends DialogComponent<AgentAppVersionWarningDialogComponent, AgentAppVersionWarningResult> {

  readonly upgradeAvailable: boolean;
  readonly titleKey: string;
  readonly textKey: string;
  readonly confirmKey: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppVersionWarningDialogData,
              public dialogRef: MatDialogRef<AgentAppVersionWarningDialogComponent, AgentAppVersionWarningResult>) {
    super(store, router, dialogRef);
    this.upgradeAvailable = !!data.nextVersion && data.profileVersion === data.nextVersion;
    if (data.context === 'assign') {
      this.titleKey = this.upgradeAvailable
        ? 'agent.app-version-upgrade-available-title'
        : 'agent.app-version-mismatch-title';
      this.textKey = this.upgradeAvailable
        ? 'agent.app-assign-upgrade-available-text'
        : 'agent.app-assign-version-mismatch-text';
      this.confirmKey = 'agent.app-assign-anyway';
    } else {
      this.titleKey = 'agent.app-update-drift-title';
      this.textKey = 'agent.app-update-drift-text';
      this.confirmKey = 'agent.app-update-anyway';
    }
  }

  get destructive(): boolean {
    return !this.upgradeAvailable;
  }

  get showUpgradeFirst(): boolean {
    return this.upgradeAvailable && this.data.context === 'assign';
  }

  cancel() {
    this.dialogRef.close(false);
  }

  confirm() {
    this.dialogRef.close(true);
  }

  upgradeFirst() {
    this.dialogRef.close('upgrade');
  }
}

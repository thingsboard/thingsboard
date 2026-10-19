// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { AgentAppProfile, AgentApplicationType } from '@shared/models/agent.models';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';

export interface AgentProfileAssignProfileDialogData {
  selectedIds?: string[];
  lockedAppType?: AgentApplicationType;
}

@Component({
  selector: 'tb-agent-profile-assign-profile-dialog',
  templateUrl: './agent-profile-assign-profile-dialog.component.html',
  styleUrls: ['./agent-profile-assign-profile-dialog.component.scss'],
  standalone: false
})
export class AgentProfileAssignProfileDialogComponent
  extends DialogComponent<AgentProfileAssignProfileDialogComponent, string[]> {

  formGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileAssignProfileDialogData,
              public dialogRef: MatDialogRef<AgentProfileAssignProfileDialogComponent, string[]>) {
    super(store, router, dialogRef);
    this.formGroup = this.fb.group({
      appProfileIds: [data?.selectedIds ?? []]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    const ids: string[] = this.formGroup.value?.appProfileIds ?? [];
    this.dialogRef.close(ids);
  }

  createAppProfile() {
    this.dismissAutocomplete();
    setTimeout(() => this.openAppProfileWizard(), 0);
  }

  private dismissAutocomplete() {
    const active = document.activeElement as HTMLElement | null;
    active?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    active?.blur();
  }

  private openAppProfileWizard() {
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { lockedAppType: this.data?.lockedAppType }
      }
    ).afterClosed().subscribe(saved => {
      if (saved) {
        const currentIds: string[] = this.formGroup.value?.appProfileIds ?? [];
        if (!currentIds.includes(saved.id.id)) {
          this.formGroup.get('appProfileIds').setValue([...currentIds, saved.id.id]);
        }
      }
    });
  }
}

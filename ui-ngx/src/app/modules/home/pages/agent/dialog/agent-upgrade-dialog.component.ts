// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { AgentAppEvent } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';

export interface AgentUpgradeDialogData {
  agentId: string;
  agentName: string;
  currentImageRef?: string;
  suggestedImageRef?: string;
}

/**
 * Collects the image the agent should replace itself with. The target is an opaque image reference
 * rather than a version number, so a digest-pinned reference is as valid as a tag; the field is
 * pre-filled with the newest published image when one is known.
 */
@Component({
  selector: 'tb-agent-upgrade-dialog',
  templateUrl: './agent-upgrade-dialog.component.html',
  standalone: false
})
export class AgentUpgradeDialogComponent extends DialogComponent<AgentUpgradeDialogComponent, AgentAppEvent> {

  upgradeFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private agentService: AgentService,
              private fb: UntypedFormBuilder,
              @Inject(MAT_DIALOG_DATA) public data: AgentUpgradeDialogData,
              public dialogRef: MatDialogRef<AgentUpgradeDialogComponent, AgentAppEvent>) {
    super(store, router, dialogRef);
    this.upgradeFormGroup = this.fb.group({
      imageRef: [data.suggestedImageRef || '', [Validators.required, Validators.maxLength(255)]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  upgrade(): void {
    if (this.upgradeFormGroup.invalid) {
      this.upgradeFormGroup.markAllAsTouched();
      return;
    }
    const imageRef = this.upgradeFormGroup.get('imageRef').value.trim();
    this.agentService.upgradeAgent(this.data.agentId, imageRef).subscribe(event => this.dialogRef.close(event));
  }
}

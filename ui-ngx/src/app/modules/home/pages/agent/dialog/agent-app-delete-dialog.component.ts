// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentApplication,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppStep
} from '@shared/models/agent.models';
import {
  buildComposeDownInput,
  extractComposeVolumeKeys
} from '@home/pages/agent/util/agent-app-steps';

export interface AgentAppDeleteDialogData {
  application: AgentApplication;
  agentName?: string;
  composeDownStep?: AgentAppStep | null;
}

@Component({
  selector: 'tb-agent-app-delete-dialog',
  templateUrl: './agent-app-delete-dialog.component.html',
  styleUrls: ['./agent-app-delete-dialog.component.scss'],
  standalone: false
})
export class AgentAppDeleteDialogComponent
  extends DialogComponent<AgentAppDeleteDialogComponent, AgentAppEvent | null> {

  application: AgentApplication;
  agentName: string;
  removeVolumes = false;
  volumeKeys: string[] = [];
  submitting = false;
  composeDownStep: AgentAppStep | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppDeleteDialogData,
              public dialogRef: MatDialogRef<AgentAppDeleteDialogComponent, AgentAppEvent | null>) {
    super(store, router, dialogRef);
    this.application = data.application;
    this.agentName = data.agentName || '';
    this.volumeKeys = extractComposeVolumeKeys(this.application);
    this.composeDownStep = data.composeDownStep ?? null;
  }

  cancel() {
    this.dialogRef.close(null);
  }

  confirm() {
    if (this.submitting) {
      return;
    }
    this.submitting = true;
    const stepInputs: { [stepId: string]: any } = {};
    if (this.composeDownStep) {
      stepInputs[this.composeDownStep.id] = buildComposeDownInput(this.composeDownStep, this.removeVolumes);
    }
    this.agentService.createAgentAppEvent(this.application.id.id, {
      actionType: AgentAppEventActionType.DELETE,
      stepInputs
    }).subscribe({
      next: event => this.dialogRef.close(event),
      error: () => {
        this.submitting = false;
      }
    });
  }
}

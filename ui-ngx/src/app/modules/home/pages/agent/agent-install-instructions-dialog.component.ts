// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Agent } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';

export interface AgentInstallInstructionsDialogData {
  agent: Agent;
  afterAdd: boolean;
  instructions?: string;
}

@Component({
  selector: 'tb-agent-install-instructions-dialog',
  templateUrl: './agent-install-instructions-dialog.component.html',
  styleUrls: ['./agent-install-instructions-dialog.component.scss'],
  standalone: false
})
export class AgentInstallInstructionsDialogComponent
  extends DialogComponent<AgentInstallInstructionsDialogComponent> {

  agent: Agent;
  afterAdd: boolean;
  instructions?: string;
  dialogTitle: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AgentInstallInstructionsDialogData,
              public dialogRef: MatDialogRef<AgentInstallInstructionsDialogComponent>) {
    super(store, router, dialogRef);
    this.agent = data.agent;
    this.afterAdd = data.afterAdd;
    this.instructions = data.instructions;
    this.dialogTitle = this.afterAdd
      ? 'agent.agent-created-successfully'
      : 'agent.install-instructions';
  }

  close() {
    this.dialogRef.close();
  }

  goToAgent() {
    if (this.agent?.id?.id) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), this.agent.id.id));
    }
    this.dialogRef.close();
  }
}

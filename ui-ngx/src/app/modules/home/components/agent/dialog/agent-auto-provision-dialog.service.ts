// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AgentApplicationType } from '@shared/models/agent.models';
import {
  AgentAutoProvisionDialogData,
  AgentAutoProvisionDialogComponent
} from '@home/components/agent/dialog/agent-auto-provision-dialog.component';

// Exposed via ServicesMap so dashboard widget customFunctions can open the
// auto-provision dialog without referencing the component class directly.
@Injectable({ providedIn: 'root' })
export class AgentAutoProvisionDialogService {

  constructor(private dialog: MatDialog) {}

  open(appType?: AgentApplicationType): Observable<boolean> {
    return this.dialog.open<AgentAutoProvisionDialogComponent, AgentAutoProvisionDialogData, boolean>(
      AgentAutoProvisionDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { appType }
      }
    ).afterClosed();
  }
}

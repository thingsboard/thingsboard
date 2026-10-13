// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AgentProfile } from '@shared/models/agent.models';
import { AgentProfileWizardData } from '@home/pages/agent/wizard/agent-profile-wizard.component';
import {
  AgentProfilePresetDialogComponent
} from '@home/pages/agent/dialog/agent-profile-preset-dialog.component';

@Injectable({ providedIn: 'root' })
export class AgentProfileCreateDialogService {

  constructor(private dialog: MatDialog) {}

  open(data: AgentProfileWizardData): Observable<AgentProfile | undefined> {
    return this.dialog.open<AgentProfilePresetDialogComponent, AgentProfileWizardData, AgentProfile>(
      AgentProfilePresetDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data
      }).afterClosed();
  }
}

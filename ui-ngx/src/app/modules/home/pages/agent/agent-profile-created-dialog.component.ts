// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AgentProfile,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeSupportsAppAutoInstall,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';

export interface AgentProfileCreatedDialogData {
  agentProfile: AgentProfile;
  dockerCommand: string;
}

@Component({
  selector: 'tb-agent-profile-created-dialog',
  templateUrl: './agent-profile-created-dialog.component.html',
  styleUrls: ['./agent-profile-created-dialog.component.scss'],
  standalone: false
})
export class AgentProfileCreatedDialogComponent
  extends DialogComponent<AgentProfileCreatedDialogComponent> {

  agentProfile: AgentProfile;
  dockerCommand: string;

  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;

  get autoInstallEnabled(): boolean {
    return agentProvisionTypeSupportsAppAutoInstall(this.agentProfile?.provisionType);
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileCreatedDialogData,
              public dialogRef: MatDialogRef<AgentProfileCreatedDialogComponent>) {
    super(store, router, dialogRef);
    this.agentProfile = data.agentProfile;
    this.dockerCommand = data.dockerCommand || '';
  }

  onCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.install-command-copied-message'),
      type: 'success',
      duration: 1000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  close() {
    this.dialogRef.close();
  }

  goToProfile() {
    if (this.agentProfile?.id?.id) {
      this.router.navigateByUrl(`/edgeManagement/profiles/agent/${this.agentProfile.id.id}`);
    }
    this.dialogRef.close();
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppTemplate,
  AgentAppConfigType,
  DockerComposeConfig
} from '@shared/models/agent.models';
import * as YAML from 'yaml';
import { dumpCompose, dumpRawTemplateCompose, parseComposeYaml } from '@home/pages/agent/util/agent-compose-yaml';

export interface AgentAppProfileUpgradeDialogData {
  profile: AgentAppProfile;
}

@Component({
  selector: 'tb-agent-app-profile-upgrade-dialog',
  templateUrl: './agent-app-profile-upgrade-dialog.component.html',
  styleUrls: ['./agent-app-profile-upgrade-dialog.component.scss'],
  standalone: false
})
export class AgentAppProfileUpgradeDialogComponent
  extends DialogComponent<AgentAppProfileUpgradeDialogComponent, AgentAppProfile | null>
  implements OnInit {

  profile: AgentAppProfile;
  mergedProfile: AgentAppProfile | null = null;
  fromVersion: string | null = null;
  toVersion: string | null = null;
  template: AgentAppTemplate | null = null;

  proposedYaml = '';
  currentYaml = '';
  composeYaml = '';
  diffSyncScroll = false;

  loadingTemplate = true;
  loadError = '';
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppProfileUpgradeDialogData,
              public dialogRef: MatDialogRef<AgentAppProfileUpgradeDialogComponent, AgentAppProfile | null>) {
    super(store, router, dialogRef);
    this.profile = data.profile;
  }

  ngOnInit() {
    this.resolveUpgradeTemplate();
  }

  cancel() {
    this.dialogRef.close(null);
  }

  canSubmit(): boolean {
    return !!this.template && !this.submitting && !this.loadError && !this.isComposeYamlInvalid();
  }

  isComposeYamlInvalid(): boolean {
    if (!this.composeYaml?.trim()) { return false; }
    try {
      YAML.parse(this.composeYaml);
      return false;
    } catch (_) {
      return true;
    }
  }

  submit() {
    if (!this.template || this.submitting || this.loadError) { return; }
    if (this.isComposeYamlInvalid()) {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('agent.app-compose-invalid-yaml'),
        type: 'error',
        duration: 3000,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
      return;
    }
    const compose = parseComposeYaml(
      this.composeYaml,
      this.composeOf(this.mergedProfile) || this.composeOf(this.profile)
    );
    this.submitting = true;

    const config: DockerComposeConfig = {
      ...((this.profile.config as DockerComposeConfig) || { type: AgentAppConfigType.DOCKER_COMPOSE }),
      compose
    };
    const updated: AgentAppProfile = {
      ...this.profile,
      templateVersion: this.template.currentVersion,
      config
    };

    this.agentService.saveAgentAppProfile(updated).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => { this.submitting = false; }
    });
  }

  private resolveUpgradeTemplate() {
    if (!this.profile.templateVersion) {
      this.failLoad('agent.app-upgrade-no-template');
      return;
    }
    this.agentService.getAgentAppTemplateByVersion(this.profile.appType, AgentAppConfigType.DOCKER_COMPOSE, this.profile.templateVersion).subscribe({
      next: current => {
        this.fromVersion = current.currentVersion || null;
        if (!current.nextVersion) {
          this.failLoad('agent.app-upgrade-no-next-version');
          return;
        }
        const configType = current.configType || AgentAppConfigType.DOCKER_COMPOSE;
        this.agentService.getAgentAppTemplateByVersion(
          current.appType, configType, current.nextVersion
        ).subscribe({
          next: next => this.applyUpgradeTemplate(next),
          error: () => this.failLoad('agent.app-upgrade-load-failed')
        });
      },
      error: () => this.failLoad('agent.app-upgrade-load-failed')
    });
  }

  private applyUpgradeTemplate(tpl: AgentAppTemplate) {
    this.template = tpl;
    this.toVersion = tpl.currentVersion || null;
    this.proposedYaml = dumpRawTemplateCompose(tpl, (this.profile.config as DockerComposeConfig)?.composeType);
    const draft = { ...this.profile, templateVersion: tpl.currentVersion } as AgentAppProfile;
    this.agentService.mergeProfileForPreview(
      tpl.currentVersion, draft, undefined, AgentAppEventActionType.UPGRADE
    ).subscribe({
      next: merged => {
        this.mergedProfile = merged;
        this.finishApplyTemplate(dumpCompose(merged));
      },
      error: () => this.failLoad('agent.app-upgrade-load-failed')
    });
  }

  private finishApplyTemplate(currentYaml: string) {
    this.currentYaml = currentYaml;
    this.composeYaml = this.currentYaml;
    this.loadingTemplate = false;
  }

  private composeOf(profile: AgentAppProfile | null): any {
    return (profile?.config as DockerComposeConfig)?.compose;
  }

  private failLoad(messageKey: string) {
    this.loadError = this.translate.instant(messageKey);
    this.loadingTemplate = false;
  }

}

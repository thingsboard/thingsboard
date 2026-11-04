// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AgentApplication, AgentInfo } from '@shared/models/agent.models';

@Component({
  selector: 'tb-agent-deploy-status',
  templateUrl: './agent-deploy-status.component.html',
  styleUrls: ['./agent-deploy-status.component.scss'],
  standalone: false
})
export class AgentDeployStatusComponent {

  @Input() agent: AgentInfo;
  @Input() application: AgentApplication | null = null;

  @Output() goToAgent = new EventEmitter<void>();
  @Output() goToAgentApplication = new EventEmitter<void>();
  @Output() goToAgentEvents = new EventEmitter<void>();

  showInstallCommand = false;

  constructor(private translate: TranslateService) {}

  get online(): boolean {
    return !!this.agent?.active;
  }

  get managingDuration(): string {
    if (!this.application?.createdTime) {
      return '';
    }
    const ms = Date.now() - this.application.createdTime;
    const days = Math.floor(ms / 86_400_000);
    if (days >= 1) {
      return this.translate.instant('agent.deploy-status-duration-days', { count: days });
    }
    const hours = Math.floor(ms / 3_600_000);
    if (hours >= 1) {
      return this.translate.instant('agent.deploy-status-duration-hours', { count: hours });
    }
    const minutes = Math.max(1, Math.floor(ms / 60_000));
    return this.translate.instant('agent.deploy-status-duration-minutes', { count: minutes });
  }

}

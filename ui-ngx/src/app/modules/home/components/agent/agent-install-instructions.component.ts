// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Agent } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';

@Component({
  selector: 'tb-agent-install-instructions',
  templateUrl: './agent-install-instructions.component.html',
  styleUrls: ['./agent-install-instructions.component.scss'],
  standalone: false
})
export class AgentInstallInstructionsComponent implements OnChanges {

  @Input() agent: Agent;
  @Input() edge = false;
  @Input() instructions?: string;

  dockerCommand = '';

  constructor(private store: Store<AppState>,
              private translate: TranslateService,
              private agentService: AgentService) {}

  ngOnChanges(changes: SimpleChanges) {
    if (this.instructions !== undefined) {
      this.dockerCommand = this.instructions || '';
      return;
    }
    if (changes.agent && this.agent?.id?.id) {
      this.loadInstructions();
    }
  }

  private loadInstructions() {
    this.agentService.getAgentInstallInstructions(this.agent.id.id).subscribe({
      next: res => { this.dockerCommand = res?.instructions || ''; },
      error: () => { this.dockerCommand = ''; }
    });
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
}

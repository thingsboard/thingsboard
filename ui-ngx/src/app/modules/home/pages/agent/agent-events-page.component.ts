// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit, ViewContainerRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { AgentEventsTableConfig } from '@home/pages/agent/table/agent-events-table-config';
import { agentEntityUrl, resolveAgentIdParam } from '@home/pages/agent/util/agent-route-params';

@Component({
  selector: 'tb-agent-events-page',
  template: `
    @if (tableConfig) {
      <tb-entities-table [entitiesTableConfig]="tableConfig"></tb-entities-table>
    }
    `,
  styles: [`
    :host { display: block; height: 100%; }
    :host ::ng-deep mat-row:has(.tb-agent-app-event-inflight) {
      background-color: #fff8e1;
      cursor: pointer;
    }
    :host ::ng-deep mat-row:has(.tb-agent-app-event-inflight) .mat-mdc-cell {
      background-color: #fff8e1;
      cursor: pointer;
    }
    :host ::ng-deep mat-row:has(.tb-agent-app-event-error),
    :host ::ng-deep mat-row:has(.tb-agent-app-event-error) .mat-mdc-cell {
      background-color: #fdecea;
    }
  `],
  standalone: false
})
export class AgentEventsPageComponent implements OnInit {

  tableConfig: AgentEventsTableConfig;

  constructor(private route: ActivatedRoute,
              private agentService: AgentService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
    const agentId = resolveAgentIdParam(this.route.snapshot);
    if (!agentId) {
      return;
    }
    this.agentService.getAgentInfoById(agentId).subscribe({
      next: agent => this.buildConfig(agentId, agent),
      error: () => this.buildConfig(agentId, null)
    });
  }

  private buildConfig(agentId: string, agent: any): void {
    const agentUrl = agentEntityUrl(this.route.snapshot, agentId);
    this.tableConfig = new AgentEventsTableConfig(
      agentId,
      agent,
      this.agentService,
      this.dialogService,
      this.dialog,
      this.translate,
      this.datePipe,
      this.overlay,
      this.viewContainerRef,
      agentUrl
    );
    this.tableConfig.backNavigationCommands = [agentUrl];
  }
}

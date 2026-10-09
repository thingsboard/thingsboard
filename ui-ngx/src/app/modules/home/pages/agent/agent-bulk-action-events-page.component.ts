// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit, ViewContainerRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import {
  AgentBulkActionEventsTableConfig
} from '@home/pages/agent/table/agent-bulk-action-events-table-config';

@Component({
  selector: 'tb-agent-bulk-action-events-page',
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
export class AgentBulkActionEventsPageComponent implements OnInit {

  tableConfig: AgentBulkActionEventsTableConfig;

  constructor(private route: ActivatedRoute,
              private agentService: AgentService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private router: Router) {}

  ngOnInit(): void {
    const bulkActionId = this.route.snapshot.params['bulkActionId'];
    if (!bulkActionId) {
      return;
    }
    this.tableConfig = new AgentBulkActionEventsTableConfig(
      bulkActionId,
      this.agentService,
      this.dialogService,
      this.dialog,
      this.translate,
      this.datePipe,
      this.overlay,
      this.viewContainerRef,
      this.router
    );
    this.agentService.getAgentBulkAction(bulkActionId).subscribe(action => {
      if (action?.agentProfileId) {
        this.tableConfig.backNavigationCommands = ['/edgeManagement/profiles/agent/' + action.agentProfileId];
      }
    });
  }
}

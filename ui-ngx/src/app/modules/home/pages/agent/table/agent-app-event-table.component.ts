// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, Input, OnChanges, SimpleChanges, ViewChild, ViewContainerRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentApplicationInfo } from '@shared/models/agent.models';
import { AgentAppEventTableConfig } from './agent-app-event-table-config';

@Component({
  selector: 'tb-agent-app-event-table',
  template: '<tb-entities-table [entitiesTableConfig]="tableConfig"></tb-entities-table>',
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
export class AgentAppEventTableComponent implements AfterViewInit, OnChanges {

  @Input() application: AgentApplicationInfo;
  @Input() active: boolean;

  @ViewChild(EntitiesTableComponent, { static: true }) entitiesTable: EntitiesTableComponent;

  tableConfig: AgentAppEventTableConfig;

  constructor(private agentService: AgentService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef) {}

  ngAfterViewInit(): void {
    this.rebuild();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.application && !changes.application.firstChange) {
      this.rebuild();
      return;
    }
    if (changes.active && !changes.active.firstChange
        && changes.active.currentValue && !changes.active.previousValue) {
      this.entitiesTable?.updateData();
    }
  }

  private rebuild(): void {
    if (!this.application) { return; }
    this.tableConfig = new AgentAppEventTableConfig(
      this.application,
      this.agentService,
      this.dialogService,
      this.dialog,
      this.translate,
      this.datePipe,
      this.overlay,
      this.viewContainerRef
    );
  }
}

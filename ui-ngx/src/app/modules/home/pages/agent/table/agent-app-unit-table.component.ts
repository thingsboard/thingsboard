// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, Input, NgZone, OnChanges, OnDestroy, SimpleChanges, ViewChild, ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentApplicationInfo } from '@shared/models/agent.models';
import { AgentAppUnitTableConfig } from './agent-app-unit-table-config';

@Component({
  selector: 'tb-agent-app-unit-table',
  template: '<tb-entities-table [entitiesTableConfig]="tableConfig"></tb-entities-table>',
  styles: [':host { display: block; height: 100%; }'],
  standalone: false
})
export class AgentAppUnitTableComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() application: AgentApplicationInfo;
  @Input() active: boolean;

  @ViewChild(EntitiesTableComponent, { static: true }) entitiesTable: EntitiesTableComponent;

  tableConfig: AgentAppUnitTableConfig;

  constructor(private agentService: AgentService,
              private attributeService: AttributeService,
              private translate: TranslateService,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private router: Router) {}

  ngAfterViewInit(): void {
    this.rebuild();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.application && !changes.application.firstChange) {
      this.rebuild();
    }
  }

  ngOnDestroy(): void {
    this.tableConfig?.destroySubscriptions();
  }

  private rebuild(): void {
    if (!this.application) { return; }
    this.tableConfig?.destroySubscriptions();
    this.tableConfig = new AgentAppUnitTableConfig(
      this.application,
      this.agentService,
      this.attributeService,
      this.translate,
      this.overlay,
      this.viewContainerRef,
      this.telemetryWsService,
      this.zone,
      this.router
    );
  }
}

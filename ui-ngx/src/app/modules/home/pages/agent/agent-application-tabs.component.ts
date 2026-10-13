// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AgentApplicationInfo } from '@shared/models/agent.models';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-agent-application-tabs',
  templateUrl: './agent-application-tabs.component.html',
  styleUrls: ['./agent-application-tabs.component.scss'],
  standalone: false
})
export class AgentApplicationTabsComponent extends EntityTabsComponent<AgentApplicationInfo> {

  readonly attributeScopes = AttributeScope;
  readonly latestTelemetryTypes = LatestTelemetry;

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}

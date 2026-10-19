// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AgentInfo } from '@shared/models/agent.models';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';

@Component({
  selector: 'tb-agent-tabs',
  templateUrl: './agent-tabs.component.html',
  styleUrls: [],
  standalone: false
})
export class AgentTabsComponent extends EntityTabsComponent<AgentInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AgentProfileInfo } from '@shared/models/agent.models';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';

@Component({
  selector: 'tb-agent-profile-tabs',
  templateUrl: './agent-profile-tabs.component.html',
  styleUrls: [],
  standalone: false
})
export class AgentProfileTabsComponent extends EntityTabsComponent<AgentProfileInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }
}

///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject, InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardInfo } from '../models/dashboard.models';

export const DASHBOARD_SELECT_PANEL_DATA = new InjectionToken<any>('DashboardSelectPanelData');

export interface DashboardSelectPanelData {
  dashboards$: Observable<Array<DashboardInfo>>;
  dashboardId: string;
  onDashboardSelected: (dashboardId: string) => void;
}

@Component({
    selector: 'tb-dashboard-select-panel',
    templateUrl: './dashboard-select-panel.component.html',
    styleUrls: ['./dashboard-select-panel.component.scss'],
    standalone: false
})
export class DashboardSelectPanelComponent {

  dashboards$: Observable<Array<DashboardInfo>>;
  dashboardId: string;

  constructor(@Inject(DASHBOARD_SELECT_PANEL_DATA)
              private data: DashboardSelectPanelData) {
    this.dashboards$ = this.data.dashboards$;
    this.dashboardId = this.data.dashboardId;
  }

  public dashboardSelected(dashboardId: string) {
    this.data.onDashboardSelected(dashboardId);
  }
}

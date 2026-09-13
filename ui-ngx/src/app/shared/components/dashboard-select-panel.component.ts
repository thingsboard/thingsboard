// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

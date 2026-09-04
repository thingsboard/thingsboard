// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Dashboard } from '@shared/models/dashboard.models';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'tb-dashboard-view',
    templateUrl: './dashboard-view.component.html',
    styleUrls: ['./dashboard-view.component.scss'],
    standalone: false
})
export class DashboardViewComponent extends PageComponent {

  dashboard: Dashboard = this.route.snapshot.data.dashboard;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute) {
    super(store);
  }
}

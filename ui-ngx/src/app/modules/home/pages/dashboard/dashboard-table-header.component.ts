// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Dashboard, DashboardInfo } from '@shared/models/dashboard.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
    selector: 'tb-dashboard-table-header',
    templateUrl: './dashboard-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class DashboardTableHeaderComponent extends EntityTableHeaderComponent<DashboardInfo | Dashboard> implements OnInit {

  includeCustomersLabel: string;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    this.includeCustomersLabel = (getCurrentAuthUser(this.store).authority === Authority.CUSTOMER_USER ||
      this.entitiesTableConfig.customerId) ? 'entity.include-sub-customer-entities' : 'entity.include-customer-entities';
  }

  includeCustomersChanged(includeCustomers: boolean) {
    this.entitiesTableConfig.componentsData.includeCustomersChanged(includeCustomers);
  }

}

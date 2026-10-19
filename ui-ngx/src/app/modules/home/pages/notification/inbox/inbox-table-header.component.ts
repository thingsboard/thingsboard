// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Notification } from '@shared/models/notification.models';

@Component({
    selector: 'tb-inbox-table-header',
    templateUrl: './inbox-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class InboxTableHeaderComponent extends EntityTableHeaderComponent<Notification> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  changeUnreadOnly(unreadOnly: boolean) {
    this.entitiesTableConfig.componentsData.unreadOnly = unreadOnly;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

}

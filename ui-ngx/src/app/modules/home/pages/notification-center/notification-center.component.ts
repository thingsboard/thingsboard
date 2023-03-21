///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import {
  NotificationTableComponent
} from '@home/pages/notification-center/notification-table/notification-table.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Authority } from '@shared/models/authority.enum';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';

@Component({
  selector: 'tb-notification-center',
  templateUrl: './notification-center.component.html',
  styleUrls: ['notification-center.component.scss']
})
export class NotificationCenterComponent extends PageComponent {

  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser: AuthUser = this.authState.authUser;

  entityType = EntityType;

  @ViewChild('matTabGroup') matTabs: MatTabGroup;
  @ViewChild('requestTab') requestTab: MatTab;
  @ViewChild('notificationRequest') notificationRequestTable: NotificationTableComponent;
  @ViewChildren(NotificationTableComponent) tableComponent: QueryList<NotificationTableComponent>;


  constructor(
    protected store: Store<AppState>) {
    super(store);
  }

  updateData() {
    this.currentTableComponent?.updateData();
  }

  openSetting() {

  }

  private get currentTableComponent(): NotificationTableComponent {
    return this.tableComponent.get(this.matTabs.selectedIndex);
  }

  sendNotification($event: Event) {
    this.notificationRequestTable.entityTableConfig
      .onEntityAction({event: $event, action: this.requestTab.isActive ? 'add' : 'add-without-update', entity: null});
  }

  public isCustomerUser(): boolean {
    return this.authUser.authority === Authority.CUSTOMER_USER;
  }
}

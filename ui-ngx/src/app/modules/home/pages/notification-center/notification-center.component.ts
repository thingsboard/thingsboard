///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { MatTabGroup } from '@angular/material/tabs';
import {
  NotificationTableComponent
} from '@home/pages/notification-center/notification-table/notification-table.component';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-notification-center',
  templateUrl: './notification-center.component.html',
  styleUrls: ['notification-center.component.scss']
})
export class NotificationCenterComponent extends PageComponent {

  entityType = EntityType;

  @ViewChild('matTabGroup', {static: true}) matTabs: MatTabGroup;
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
}

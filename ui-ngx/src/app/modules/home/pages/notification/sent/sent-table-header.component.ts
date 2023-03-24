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

import { Component } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { NotificationRequest, NotificationRequestInfo } from '@shared/models/notification.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-sent-table-header',
  templateUrl: './sent-table-header.component.html',
  styleUrls: ['sent-table-header.component.scss']
})
export class SentTableHeaderComponent extends EntityTableHeaderComponent<NotificationRequest, PageLink, NotificationRequestInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  sendNotification($event: Event) {
    this.entitiesTableConfig.onEntityAction({event: $event, action: 'add', entity: null});
  }

}

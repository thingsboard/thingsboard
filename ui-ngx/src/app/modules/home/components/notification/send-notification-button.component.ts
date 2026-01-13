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

import { Component } from '@angular/core';
import {
  RequestNotificationDialogData,
  SentNotificationDialogComponent
} from '@home/pages/notification/sent/sent-notification-dialog.componet';
import { NotificationTemplate } from '@shared/models/notification.models';
import { MatDialog } from '@angular/material/dialog';
import { ActiveComponentService } from '@core/services/active-component.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-send-notification-button',
  templateUrl: './send-notification-button.component.html',
})
export class SendNotificationButtonComponent {

  authUser = getCurrentAuthUser(this.store);

  constructor(private dialog: MatDialog,
              private store: Store<AppState>,
              private activeComponentService: ActiveComponentService) {
  }

  sendNotification($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SentNotificationDialogComponent, RequestNotificationDialogData,
      NotificationTemplate>(SentNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true
      }
    }).afterClosed().subscribe((res) => {
      if (res) {
        const comp = this.activeComponentService.getCurrentActiveComponent();
        if (comp instanceof EntitiesTableComponent) {
          const entitiesTableComponent = comp as EntitiesTableComponent;
          if (entitiesTableComponent.entitiesTableConfig.entityType === EntityType.NOTIFICATION_REQUEST) {
            entitiesTableComponent.entitiesTableConfig.updateData();
          }
        }
      }
    });
  }

  public show(): boolean {
    return this.authUser.authority !== Authority.CUSTOMER_USER;
  }

}

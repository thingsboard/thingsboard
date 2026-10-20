// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';

@Component({
    selector: 'tb-send-notification-button',
    templateUrl: './send-notification-button.component.html',
    standalone: false
})
export class SendNotificationButtonComponent {

  private authUser: AuthUser = getCurrentAuthUser(this.store);

  private hasAiPermission: boolean = getCurrentAuthState(this.store).aiEnabled &&
    this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL) &&
    this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);

  constructor(private dialog: MatDialog,
              private store: Store<AppState>,
              private activeComponentService: ActiveComponentService,
              private userPermissionsService: UserPermissionsService,
              private panelService: AiAssistantPanelService) {
  }

  showAiAssistant(): boolean {
    return this.hasAiPermission && !this.panelService.open();
  }

  toggleAiAssistant($event: Event): void {
    $event?.stopPropagation();
    this.panelService.toggle();
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
    return !this.isCustomer() && this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);
  }

  private isCustomer(): boolean {
    return this.authUser.authority === Authority.CUSTOMER_USER;
  }

}

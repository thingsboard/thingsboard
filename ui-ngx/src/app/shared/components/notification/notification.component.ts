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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  ActionButtonLinkType,
  Notification,
  NotificationStatus,
  NotificationType,
  NotificationTypeIcons
} from '@shared/models/notification.models';
import { UtilsService } from '@core/services/utils.service';
import { Router } from '@angular/router';
import {
  alarmSeverityBackgroundColors,
  alarmSeverityColors,
  alarmSeverityTranslations
} from '@shared/models/alarm.models';
import tinycolor from 'tinycolor2';
import { StateObject } from '@core/api/widget-api.models';
import { objToBase64URI } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss']
})
export class NotificationComponent implements OnInit {

  @Input()
  notification: Notification;

  @Input()
  onClose: () => void;

  @Output()
  markAsRead = new EventEmitter<string>();

  @Input()
  @coerceBoolean()
  preview = false;

  showIcon = false;
  showButton = false;
  buttonLabel = '';
  hideMarkAsReadButton = false;

  notificationType = NotificationType;
  notificationTypeIcons = NotificationTypeIcons;
  alarmSeverityTranslations = alarmSeverityTranslations;

  currentDate = Date.now();

  title = '';
  message = '';

  constructor(
    private utils: UtilsService,
    private router: Router
  ) {
  }

  ngOnInit() {
    this.showIcon = this.notification.additionalConfig?.icon?.enabled;
    this.showButton = this.notification.additionalConfig?.actionButtonConfig?.enabled;
    this.hideMarkAsReadButton = this.notification.status === NotificationStatus.READ;
    this.title = this.utils.customTranslation(this.notification.subject, this.notification.subject);
    this.message = this.utils.customTranslation(this.notification.text, this.notification.text);
    if (this.showButton) {
      this.buttonLabel = this.utils.customTranslation(this.notification.additionalConfig.actionButtonConfig.text,
                                                      this.notification.additionalConfig.actionButtonConfig.text);
    }
  }

  markRead($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.preview) {
      this.markAsRead.next(this.notification.id.id);
    }
  }

  navigate($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.preview) {
      let link: string;
      if (this.notification.additionalConfig.actionButtonConfig.linkType === ActionButtonLinkType.DASHBOARD) {
        let state = null;
        if (this.notification.additionalConfig.actionButtonConfig.dashboardState ||
          this.notification.additionalConfig.actionButtonConfig.setEntityIdInState) {
          const stateObject: StateObject = {};
          if (this.notification.additionalConfig.actionButtonConfig.setEntityIdInState) {
            stateObject.params = {
              entityId: this.notification.info?.stateEntityId ?? null
            };
          } else {
            stateObject.params = {};
          }
          if (this.notification.additionalConfig.actionButtonConfig.dashboardState) {
            stateObject.id = this.notification.additionalConfig.actionButtonConfig.dashboardState;
          }
          state = objToBase64URI([ stateObject ]);
        }
        link = `/dashboards/${this.notification.additionalConfig.actionButtonConfig.dashboardId}`;
        if (state) {
          link += `?state=${state}`;
        }
      } else {
        link = this.notification.additionalConfig.actionButtonConfig.link;
      }
      if (link.startsWith('/')) {
        this.router.navigateByUrl(this.router.parseUrl(link)).then(() => {
        });
      } else {
        window.open(link, '_blank');
      }
      if (this.onClose) {
        this.onClose();
      }
    }
  }

  alarmColorSeverityBackground() {
    return alarmSeverityBackgroundColors.get(this.notification.info.alarmSeverity);
  }

  notificationColor(): string {
    if (this.notification.type === NotificationType.ALARM && !this.notification.info.cleared) {
      return alarmSeverityColors.get(this.notification.info.alarmSeverity);
    }
    return 'transparent';
  }

  notificationBackgroundColor(): string {
    if (this.notification.type === NotificationType.ALARM && !this.notification.info.cleared) {
      return '#fff';
    }
    return 'transparent';
  }

  notificationIconColor(): object {
    if (this.notification.type === NotificationType.ALARM) {
      return {color: alarmSeverityColors.get(this.notification.info.alarmSeverity)};
    } else if (this.notification.type === NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT) {
      return {color: '#D12730'};
    } else if (this.notification.type === NotificationType.ENTITIES_LIMIT_INCREASE_REQUEST) {
      return {color: '#305680'};
    }
    return null;
  }
}

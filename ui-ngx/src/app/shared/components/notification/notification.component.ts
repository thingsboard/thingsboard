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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  AlarmSeverityNotificationColors,
  Notification,
  NotificationType,
  NotificationTypeIcons
} from '@shared/models/notification.models';
import { UtilsService } from '@core/services/utils.service';
import { Router } from '@angular/router';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { alarmSeverityTranslations } from '@shared/models/alarm.models';
import * as tinycolor_ from 'tinycolor2';

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

  private previewValue = false;
  get preview(): boolean {
    return this.previewValue;
  }
  @Input()
  set preview(value: boolean) {
    this.previewValue = coerceBooleanProperty(value);
  }

  showIcon = false;
  showButton = false;
  buttonLabel = '';

  tinycolor = tinycolor_;

  notificationType = NotificationType;
  notificationTypeIcons = NotificationTypeIcons;
  alarmSeverityTranslations = alarmSeverityTranslations;

  constructor(
    private utils: UtilsService,
    private router: Router
  ) {
  }

  ngOnInit() {
    this.showIcon = this.notification.additionalConfig?.icon?.enabled;
    this.showButton = this.notification.additionalConfig?.actionButtonConfig?.enabled;
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
      if (this.notification.additionalConfig.actionButtonConfig.link.startsWith('/')) {
        this.router.navigateByUrl(this.router.parseUrl(this.notification.additionalConfig.actionButtonConfig.link)).then(() => {
        });
      } else {
        window.open(this.notification.additionalConfig.actionButtonConfig.link, '_blank');
      }
      if (this.onClose) {
        this.onClose();
      }
    }
  }

  alarmColorSeverity(alpha: number) {
    return this.tinycolor(AlarmSeverityNotificationColors.get(this.notification.info.alarmSeverity)).setAlpha(alpha).toRgbString();
  }

  notificationColor(): string {
    if (this.notification.type === NotificationType.ALARM) {
      return AlarmSeverityNotificationColors.get(this.notification.info.alarmSeverity);
    }
    return 'transparent';
  }

  notificationIconColor(): object {
    if (this.notification.type === NotificationType.ALARM) {
      return {color: AlarmSeverityNotificationColors.get(this.notification.info.alarmSeverity)};
    }
    return null;
  }
}

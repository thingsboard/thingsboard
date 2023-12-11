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

import {
  CmdUpdateMsg,
  CmdUpdateType,
  NotificationCountUpdate,
  NotificationsUpdate,
  WebsocketCmd,
  WebsocketDataMsg,
  WsCmdType
} from '@shared/models/telemetry/telemetry.models';
import { NgZone } from '@angular/core';
import { isDefinedAndNotNull } from '@core/utils';
import { Notification } from '@shared/models/notification.models';
import { WsService, WsSubscriber } from '@shared/models/websocket/websocket.models';
import { BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { WebsocketService } from '@core/ws/websocket.service';

export class NotificationSubscriber extends WsSubscriber {
  private notificationCountSubject = new BehaviorSubject<NotificationCountUpdate>({
    cmdId: 0,
    cmdUpdateType: undefined,
    errorCode: 0,
    errorMsg: '',
    totalUnreadCount: 0,
    sequenceNumber: 0
  });
  private notificationsSubject = new BehaviorSubject<NotificationsUpdate>({
    cmdId: 0,
    cmdUpdateType: undefined,
    errorCode: 0,
    errorMsg: '',
    notifications: null,
    totalUnreadCount: 0,
    sequenceNumber: 0
  });

  public messageLimit = 10;

  public notificationCount$ = this.notificationCountSubject.asObservable().pipe(map(msg => msg.totalUnreadCount));
  public notifications$ = this.notificationsSubject.asObservable().pipe(map(msg => msg.notifications ));

  public static createNotificationCountSubscription(websocketService: WebsocketService<WsSubscriber>,
                                                    zone: NgZone): NotificationSubscriber {
    const subscriptionCommand = new UnreadCountSubCmd();
    const subscriber = new NotificationSubscriber(websocketService, zone);
    subscriber.subscriptionCommands.push(subscriptionCommand);
    return subscriber;
  }

  public static createNotificationsSubscription(websocketService: WebsocketService<WsSubscriber>,
                                                zone: NgZone, limit = 10): NotificationSubscriber {
    const subscriptionCommand = new UnreadSubCmd(limit);
    const subscriber = new NotificationSubscriber(websocketService, zone);
    subscriber.messageLimit = limit;
    subscriber.subscriptionCommands.push(subscriptionCommand);
    return subscriber;
  }

  public static createMarkAsReadCommand(websocketService: WebsocketService<WsSubscriber>,
                                        ids: string[]): NotificationSubscriber {
    const subscriptionCommand = new MarkAsReadCmd(ids);
    const subscriber = new NotificationSubscriber(websocketService);
    subscriber.subscriptionCommands.push(subscriptionCommand);
    return subscriber;
  }

  public static createMarkAllAsReadCommand(websocketService: WebsocketService<WsSubscriber>): NotificationSubscriber {
    const subscriptionCommand = new MarkAllAsReadCmd();
    const subscriber = new NotificationSubscriber(websocketService);
    subscriber.subscriptionCommands.push(subscriptionCommand);
    return subscriber;
  }

  constructor(private websocketService: WsService<any>, protected zone?: NgZone) {
    super(websocketService, zone);
  }

  onNotificationCountUpdate(message: NotificationCountUpdate) {
    const currentNotificationCount = this.notificationCountSubject.value;
    if (message.sequenceNumber <= currentNotificationCount.sequenceNumber) {
      return;
    }
    if (this.zone) {
      this.zone.run(
        () => {
          this.notificationCountSubject.next(message);
        }
      );
    } else {
      this.notificationCountSubject.next(message);
    }
  }

  public complete() {
    this.notificationCountSubject.complete();
    this.notificationsSubject.complete();
    super.complete();
  }

  onNotificationsUpdate(message: NotificationsUpdate) {
    const currentNotifications = this.notificationsSubject.value;
    if (message.sequenceNumber <= currentNotifications.sequenceNumber) {
      message.totalUnreadCount = currentNotifications.totalUnreadCount;
    }
    let processMessage = message;
    if (isDefinedAndNotNull(currentNotifications) && message.update) {
      currentNotifications.notifications.unshift(message.update);
      if (currentNotifications.notifications.length > this.messageLimit) {
        currentNotifications.notifications.pop();
      }
      processMessage = currentNotifications;
      processMessage.totalUnreadCount = message.totalUnreadCount;
    }
    if (this.zone) {
      this.zone.run(
        () => {
          this.notificationsSubject.next(processMessage);
          this.notificationCountSubject.next(processMessage);
        }
      );
    } else {
      this.notificationsSubject.next(processMessage);
      this.notificationCountSubject.next(processMessage);
    }
  }
}

export class UnreadCountSubCmd implements WebsocketCmd {
  cmdId: number;
  type = WsCmdType.NOTIFICATIONS_COUNT;
}

export class UnreadSubCmd implements WebsocketCmd {
  limit: number;
  cmdId: number;
  type = WsCmdType.NOTIFICATIONS;

  constructor(limit = 10) {
    this.limit = limit;
  }
}

export class UnsubscribeCmd implements WebsocketCmd {
  cmdId: number;
  type = WsCmdType.NOTIFICATIONS_UNSUBSCRIBE;
}

export class MarkAsReadCmd implements WebsocketCmd {

  cmdId: number;
  notifications: string[];
  type = WsCmdType.MARK_NOTIFICATIONS_AS_READ;

  constructor(ids: string[]) {
    this.notifications = ids;
  }
}

export class MarkAllAsReadCmd implements WebsocketCmd {
  cmdId: number;
  type = WsCmdType.MARK_ALL_NOTIFICATIONS_AS_READ;
}

export interface NotificationCountUpdateMsg extends CmdUpdateMsg {
  cmdUpdateType: CmdUpdateType.NOTIFICATIONS_COUNT;
  totalUnreadCount: number;
  sequenceNumber: number;
}

export interface NotificationsUpdateMsg extends CmdUpdateMsg {
  cmdUpdateType: CmdUpdateType.NOTIFICATIONS;
  update?: Notification;
  notifications?: Notification[];
  totalUnreadCount: number;
  sequenceNumber: number;
}

export const isNotificationCountUpdateMsg = (message: WebsocketDataMsg): message is NotificationCountUpdateMsg => {
  const updateMsg = (message as CmdUpdateMsg);
  return updateMsg.cmdId !== undefined && updateMsg.cmdUpdateType === CmdUpdateType.NOTIFICATIONS_COUNT;
};

export const isNotificationsUpdateMsg = (message: WebsocketDataMsg): message is NotificationsUpdateMsg => {
  const updateMsg = (message as CmdUpdateMsg);
  return updateMsg.cmdId !== undefined && updateMsg.cmdUpdateType === CmdUpdateType.NOTIFICATIONS;
};

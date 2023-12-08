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

import { Inject, Injectable, NgZone } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { WINDOW } from '@core/services/window.service';
import {
  isNotificationCountUpdateMsg,
  isNotificationsUpdateMsg,
  MarkAllAsReadCmd,
  MarkAsReadCmd,
  NotificationCountUpdate,
  NotificationPluginCmdWrapper,
  NotificationSubscriber,
  NotificationsUpdate,
  UnreadCountSubCmd,
  UnreadSubCmd,
  UnsubscribeCmd,
  WebsocketNotificationMsg
} from '@shared/models/websocket/notification-ws.models';
import { WebsocketService } from '@core/ws/websocket.service';


// @dynamic
@Injectable({
  providedIn: 'root'
})
export class NotificationWebsocketService extends WebsocketService<NotificationSubscriber> {

  cmdWrapper: NotificationPluginCmdWrapper;

  constructor(protected store: Store<AppState>,
              protected authService: AuthService,
              protected ngZone: NgZone,
              @Inject(WINDOW) protected window: Window) {
    super(store, authService, ngZone, 'api/ws/plugins/notifications', new NotificationPluginCmdWrapper(), window);
    this.errorName = 'WebSocket Notification Error';
  }

  public subscribe(subscriber: NotificationSubscriber) {
    this.isActive = true;
    subscriber.subscriptionCommands.forEach(
      (subscriptionCommand) => {
        const cmdId = this.nextCmdId();
        this.subscribersMap.set(cmdId, subscriber);
        subscriptionCommand.cmdId = cmdId;
        if (subscriptionCommand instanceof UnreadCountSubCmd) {
          this.cmdWrapper.unreadCountSubCmd = subscriptionCommand;
        } else if (subscriptionCommand instanceof UnreadSubCmd) {
          this.cmdWrapper.unreadSubCmd = subscriptionCommand;
        } else if (subscriptionCommand instanceof MarkAsReadCmd) {
          this.cmdWrapper.markAsReadCmd = subscriptionCommand;
          this.subscribersMap.delete(cmdId);
        } else if (subscriptionCommand instanceof MarkAllAsReadCmd) {
          this.cmdWrapper.markAllAsReadCmd = subscriptionCommand;
          this.subscribersMap.delete(cmdId);
        }
      }
    );
    if (this.cmdWrapper.unreadCountSubCmd || this.cmdWrapper.unreadSubCmd) {
      this.subscribersCount++;
    }
    this.publishCommands();
  }

  public update(subscriber: NotificationSubscriber) {
    if (!this.isReconnect) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand.cmdId && subscriptionCommand instanceof UnreadSubCmd) {
            this.cmdWrapper.unreadSubCmd = subscriptionCommand;
          }
        }
      );
      this.publishCommands();
    }
  }

  public unsubscribe(subscriber: NotificationSubscriber) {
    if (this.isActive) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand instanceof UnreadCountSubCmd
              || subscriptionCommand instanceof UnreadSubCmd) {
            const unreadCountUnsubscribeCmd = new UnsubscribeCmd();
            unreadCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.unsubCmd = unreadCountUnsubscribeCmd;
          }
          const cmdId = subscriptionCommand.cmdId;
          if (cmdId) {
            this.subscribersMap.delete(cmdId);
          }
        }
      );
      this.reconnectSubscribers.delete(subscriber);
      this.subscribersCount--;
      this.publishCommands();
    }
  }

  processOnMessage(message: WebsocketNotificationMsg) {
    let subscriber: NotificationSubscriber;
    if (isNotificationCountUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onNotificationCountUpdate(new NotificationCountUpdate(message));
      }
    } else if (isNotificationsUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onNotificationsUpdate(new NotificationsUpdate(message));
      }
    }
  }

}

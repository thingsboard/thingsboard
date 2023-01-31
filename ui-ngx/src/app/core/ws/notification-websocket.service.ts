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

import { Inject, Injectable, NgZone } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { WINDOW } from '@core/services/window.service';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  isNotificationCountUpdateMsg,
  isNotificationsUpdateMsg,
  MarkAllAsReadCmd,
  MarkAsReadCmd,
  NotificationCountUpdate,
  NotificationPluginCmdsWrapper,
  NotificationSubscriber,
  NotificationsUpdate,
  NotificationWsService,
  UnreadCountSubCmd,
  UnreadSubCmd,
  UnsubscribeCmd,
  WebsocketNotificationMsg
} from '@shared/models/notification-ws.models';
import Timeout = NodeJS.Timeout;

const RECONNECT_INTERVAL = 2000;
const WS_IDLE_TIMEOUT = 90000;

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class NotificationWebsocketService implements NotificationWsService {

  isActive = false;
  isOpening = false;
  isOpened = false;
  isReconnect = false;

  socketCloseTimer: Timeout;
  reconnectTimer: Timeout;

  lastCmdId = 0;
  subscribersCount = 0;
  subscribersMap = new Map<number, NotificationSubscriber>();

  reconnectSubscribers = new Set<NotificationSubscriber>();

  cmdsWrapper = new NotificationPluginCmdsWrapper();
  notificationUri: string;

  dataStream: WebSocketSubject<NotificationPluginCmdsWrapper | WebsocketNotificationMsg>;

  constructor(private store: Store<AppState>,
              private authService: AuthService,
              private ngZone: NgZone,
              @Inject(WINDOW) private window: Window) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      () => {
        this.reset(true);
      }
    );

    let port = this.window.location.port;
    if (this.window.location.protocol === 'https:') {
      if (!port) {
        port = '443';
      }
      this.notificationUri = 'wss:';
    } else {
      if (!port) {
        port = '80';
      }
      this.notificationUri = 'ws:';
    }
    this.notificationUri += `//${this.window.location.hostname}:${port}/api/ws/plugins/notifications`;
  }

  public subscribe(subscriber: NotificationSubscriber) {
    this.isActive = true;
    subscriber.subscriptionCommands.forEach(
      (subscriptionCommand) => {
        const cmdId = this.nextCmdId();
        this.subscribersMap.set(cmdId, subscriber);
        subscriptionCommand.cmdId = cmdId;
        if (subscriptionCommand instanceof UnreadCountSubCmd) {
          this.cmdsWrapper.unreadCountSubCmd = subscriptionCommand;
        } else if (subscriptionCommand instanceof UnreadSubCmd) {
          this.cmdsWrapper.unreadSubCmd = subscriptionCommand;
        } else if (subscriptionCommand instanceof MarkAsReadCmd) {
          this.cmdsWrapper.markAsReadCmd = subscriptionCommand;
          this.subscribersMap.delete(cmdId);
        } else if (subscriptionCommand instanceof MarkAllAsReadCmd) {
          this.cmdsWrapper.markAllAsReadCmd = subscriptionCommand;
          this.subscribersMap.delete(cmdId);
        }
      }
    );
    if (this.cmdsWrapper.markAsReadCmd || this.cmdsWrapper.markAllAsReadCmd) {
      this.subscribersCount++;
    }
    this.publishCommands();
  }

  public update(subscriber: NotificationSubscriber) {
    if (!this.isReconnect) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand.cmdId && subscriptionCommand instanceof UnreadSubCmd) {
            this.cmdsWrapper.unreadSubCmd = subscriptionCommand;
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
            this.cmdsWrapper.unsubCmd = unreadCountUnsubscribeCmd;
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

  private nextCmdId(): number {
    this.lastCmdId++;
    return this.lastCmdId;
  }

  private publishCommands() {
    while (this.isOpened && this.cmdsWrapper.hasCommands()) {
      this.dataStream.next(this.cmdsWrapper.preparePublishCommands());
      this.cmdsWrapper.clear();
      this.checkToClose();
    }
    this.tryOpenSocket();
  }

  private checkToClose() {
    if (this.subscribersCount === 0 && this.isOpened) {
      if (!this.socketCloseTimer) {
        this.socketCloseTimer = setTimeout(
          () => this.closeSocket(), WS_IDLE_TIMEOUT);
      }
    }
  }

  private reset(close: boolean) {
    if (this.socketCloseTimer) {
      clearTimeout(this.socketCloseTimer);
      this.socketCloseTimer = null;
    }
    this.lastCmdId = 0;
    this.subscribersMap.clear();
    this.subscribersCount = 0;
    this.cmdsWrapper.clear();
    if (close) {
      this.closeSocket();
    }
  }

  private closeSocket() {
    this.isActive = false;
    if (this.isOpened) {
      this.dataStream.unsubscribe();
    }
  }

  private tryOpenSocket() {
    if (this.isActive) {
      if (!this.isOpened && !this.isOpening) {
        this.isOpening = true;
        if (AuthService.isJwtTokenValid()) {
          this.openSocket(AuthService.getJwtToken());
        } else {
          this.authService.refreshJwtToken().subscribe(() => {
              this.openSocket(AuthService.getJwtToken());
            },
            () => {
              this.isOpening = false;
              this.authService.logout(true, true);
            }
          );
        }
      }
      if (this.socketCloseTimer) {
        clearTimeout(this.socketCloseTimer);
        this.socketCloseTimer = null;
      }
    }
  }

  private openSocket(token: string) {
    const uri = `${this.notificationUri}?token=${token}`;
    this.dataStream = webSocket(
      {
        url: uri,
        openObserver: {
          next: () => {
            this.onOpen();
          }
        },
        closeObserver: {
          next: (e: CloseEvent) => {
            this.onClose(e);
          }
        }
      }
    );

    this.dataStream.subscribe((message) => {
        this.ngZone.runOutsideAngular(() => {
          this.onMessage(message as WebsocketNotificationMsg);
        });
    },
    (error) => {
      this.onError(error);
    });
  }

  private onOpen() {
    this.isOpening = false;
    this.isOpened = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.isReconnect) {
      this.isReconnect = false;
      this.reconnectSubscribers.forEach(
        (reconnectSubscriber) => {
          reconnectSubscriber.onReconnected();
          this.subscribe(reconnectSubscriber);
        }
      );
      this.reconnectSubscribers.clear();
    } else {
      this.publishCommands();
    }
  }

  private onMessage(message: WebsocketNotificationMsg) {
    if (message.errorCode) {
      this.showWsError(message.errorCode, message.errorMsg);
    } else {
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
    this.checkToClose();
  }

  private onError(errorEvent) {
    if (errorEvent) {
      console.warn('WebSocket error event', errorEvent);
    }
    this.isOpening = false;
  }

  private onClose(closeEvent: CloseEvent) {
    if (closeEvent && closeEvent.code > 1001 && closeEvent.code !== 1006
      && closeEvent.code !== 1011 && closeEvent.code !== 1012 && closeEvent.code !== 4500) {
      this.showWsError(closeEvent.code, closeEvent.reason);
    }
    this.isOpening = false;
    this.isOpened = false;
    if (this.isActive) {
      if (!this.isReconnect) {
        this.reconnectSubscribers.clear();
        this.subscribersMap.forEach(
          (subscriber) => {
            this.reconnectSubscribers.add(subscriber);
          }
        );
        this.reset(false);
        this.isReconnect = true;
      }
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
      }
      this.reconnectTimer = setTimeout(() => this.tryOpenSocket(), RECONNECT_INTERVAL);
    }
  }

  private showWsError(errorCode: number, errorMsg: string) {
    let message = errorMsg;
    if (!message) {
      message += `WebSocket Notification Error: error code - ${errorCode}.`;
    }
    this.store.dispatch(new ActionNotificationShow(
      {
        message, type: 'error'
      }));
  }

}

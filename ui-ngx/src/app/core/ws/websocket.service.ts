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

import { CmdWrapper, WsService, WsSubscriber } from '@shared/models/websocket/websocket.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { NgZone } from '@angular/core';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import {
  AuthWsCmd,
  CmdUpdateMsg,
  NotificationSubscriber,
  TelemetrySubscriber,
  WebsocketDataMsg
} from '@shared/models/telemetry/telemetry.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { NotificationType } from '@core/notification/notification.models';
import Timeout = NodeJS.Timeout;

const RECONNECT_INTERVAL = 2000;
const MAX_RECONNECT_INTERVAL = 60000;
const WS_IDLE_TIMEOUT = 90000;
const MAX_PUBLISH_COMMANDS = 10;

export abstract class WebsocketService<T extends WsSubscriber> implements WsService<T> {

  isActive = false;
  isOpening = false;
  isOpened = false;
  isReconnect = false;

  socketCloseTimer: Timeout;
  reconnectTimer: Timeout;

  lastCmdId = 0;
  subscribersCount = 0;
  subscribersMap = new Map<number, TelemetrySubscriber | NotificationSubscriber>();

  reconnectSubscribers = new Set<WsSubscriber>();

  wsUri: string;

  dataStream: WebSocketSubject<CmdWrapper | CmdUpdateMsg | AuthWsCmd>;

  errorName = 'WebSocket Error';

  // Exponential backoff: tracks the number of consecutive failed reconnect attempts.
  // Reset only after a productive connection (i.e. at least one message received).
  // This prevents the open→immediately-closed cycle from resetting the counter.
  private reconnectAttempts = 0;

  // Stores the last close-event error code shown to the user during a reconnect cycle.
  // Only suppresses duplicate notifications for the same error code; a new error code is still shown.
  // Cleared after receiving a successful message.
  private lastShownCloseCode: number | null = null;

  protected constructor(protected store: Store<AppState>,
                        protected authService: AuthService,
                        protected ngZone: NgZone,
                        protected apiEndpoint: string,
                        protected cmdWrapper: CmdWrapper,
                        protected window: Window) {
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
      this.wsUri = 'wss:';
    } else {
      if (!port) {
        port = '80';
      }
      this.wsUri = 'ws:';
    }
    this.wsUri += `//${this.window.location.hostname}:${port}/${apiEndpoint}`;
  }

  abstract subscribe(subscriber: WsSubscriber);

  abstract update(subscriber: T);

  abstract unsubscribe(subscriber: T);

  abstract processOnMessage(message: WebsocketDataMsg);

  protected nextCmdId(): number {
    this.lastCmdId++;
    return this.lastCmdId;
  }

  protected publishCommands() {
    while (this.isOpened && this.cmdWrapper.hasCommands()) {
      this.dataStream.next(this.cmdWrapper.preparePublishCommands(MAX_PUBLISH_COMMANDS));
      this.checkToClose();
    }
    if (this.subscribersCount > 0) {
      this.tryOpenSocket();
    }
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
    this.cmdWrapper.clear();
    if (close) {
      this.reconnectAttempts = 0;
      this.lastShownCloseCode = null;
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
          this.authService.refreshJwtToken().subscribe({
            next: () => {
              this.openSocket(AuthService.getJwtToken());
            },
            error: () => {
              this.isOpening = false;
              this.authService.logout(true, true);
            }
          });
        }
      }
      if (this.socketCloseTimer) {
        clearTimeout(this.socketCloseTimer);
        this.socketCloseTimer = null;
      }
    }
  }

  private openSocket(token: string) {
    const uri = `${this.wsUri}`;
    this.dataStream = webSocket<CmdUpdateMsg>(
      {
        url: uri,
        openObserver: {
          next: () => {
            this.onOpen(token);
          }
        },
        closeObserver: {
          next: (e: CloseEvent) => {
            this.onClose(e);
          }
        }
      }
    );

    this.dataStream.subscribe({
      next: (message: CmdUpdateMsg) => {
        this.ngZone.runOutsideAngular(() => {
          this.onMessage(message);
        });
      },
      error: (error) => {
        this.onError(error);
      }
    });
  }

  private onOpen(token: string) {
    this.isOpening = false;
    this.isOpened = true;
    this.cmdWrapper.setAuth(token);
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

  private onMessage(message: CmdUpdateMsg) {
    if (message.errorCode) {
      this.showWsError(message.errorCode, message.errorMsg);
    } else {
      this.processOnMessage(message as WebsocketDataMsg);
    }
    this.checkToClose();
    if (this.reconnectAttempts) {
      this.reconnectAttempts = 0;
      this.lastShownCloseCode = null;
    }
  }

  private onError(errorEvent) {
    if (errorEvent) {
      console.warn('WebSocket error event', errorEvent);
    }
    this.isOpening = false;
  }

  private onClose(closeEvent: CloseEvent) {
    // Show error notification only when the error code changes to prevent notification spam,
    // while still surfacing new, potentially actionable errors during a reconnect cycle.
    // lastShownCloseCode is cleared only after a productive connection (onMessage).
    if (closeEvent && closeEvent.code > 1001 && closeEvent.code !== 1006
      && closeEvent.code !== 1011 && closeEvent.code !== 1012 && closeEvent.code !== 4500
      && this.lastShownCloseCode !== closeEvent.code) {
      this.lastShownCloseCode = closeEvent.code;
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
      const delay = Math.min(RECONNECT_INTERVAL * Math.pow(2, this.reconnectAttempts), MAX_RECONNECT_INTERVAL);
      this.reconnectAttempts = Math.min(this.reconnectAttempts + 1, 10);
      this.reconnectTimer = setTimeout(() => this.tryOpenSocket(), delay);
    }
  }

  private showWsError(errorCode: number, errorMsg: string) {
    let message = errorMsg;
    let notificationType: NotificationType = 'error';

    if (errorCode === 1008 || (errorMsg && errorMsg.includes('limit reached'))) {
      message = 'Too many active sessions. Please close unused browser tabs or sign out from other devices';
      notificationType = 'warn';
    } else if (errorCode === 1009) {
      message = 'Too much data to display. Please refresh the page or narrow your request.';
      notificationType = 'warn';
    } else if (!message) {
      message = `${this.errorName}: error code - ${errorCode}.`;
    }

    this.store.dispatch(new ActionNotificationShow({
      message, type: notificationType
    }));
  }
}

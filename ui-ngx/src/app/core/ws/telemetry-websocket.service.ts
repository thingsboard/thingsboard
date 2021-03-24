///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import {
  AlarmDataCmd,
  AlarmDataUnsubscribeCmd,
  AlarmDataUpdate,
  AttributesSubscriptionCmd,
  EntityCountCmd, EntityCountUnsubscribeCmd,
  EntityCountUpdate,
  EntityDataCmd,
  EntityDataUnsubscribeCmd,
  EntityDataUpdate,
  GetHistoryCmd,
  isAlarmDataUpdateMsg,
  isEntityCountUpdateMsg,
  isEntityDataUpdateMsg,
  SubscriptionCmd,
  SubscriptionUpdate,
  TelemetryFeature,
  TelemetryPluginCmdsWrapper,
  TelemetryService,
  TelemetrySubscriber,
  TimeseriesSubscriptionCmd,
  WebsocketDataMsg
} from '@app/shared/models/telemetry/telemetry.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { WINDOW } from '@core/services/window.service';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import Timeout = NodeJS.Timeout;

const RECONNECT_INTERVAL = 2000;
const WS_IDLE_TIMEOUT = 90000;
const MAX_PUBLISH_COMMANDS = 10;

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class TelemetryWebsocketService implements TelemetryService {

  isActive = false;
  isOpening = false;
  isOpened = false;
  isReconnect = false;

  socketCloseTimer: Timeout;
  reconnectTimer: Timeout;

  lastCmdId = 0;
  subscribersCount = 0;
  subscribersMap = new Map<number, TelemetrySubscriber>();

  reconnectSubscribers = new Set<TelemetrySubscriber>();

  cmdsWrapper = new TelemetryPluginCmdsWrapper();
  telemetryUri: string;

  dataStream: WebSocketSubject<TelemetryPluginCmdsWrapper | WebsocketDataMsg>;

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
      this.telemetryUri = 'wss:';
    } else {
      if (!port) {
        port = '80';
      }
      this.telemetryUri = 'ws:';
    }
    this.telemetryUri += `//${this.window.location.hostname}:${port}/api/ws/plugins/telemetry`;
  }

  public subscribe(subscriber: TelemetrySubscriber) {
    this.isActive = true;
    subscriber.subscriptionCommands.forEach(
      (subscriptionCommand) => {
        const cmdId = this.nextCmdId();
        this.subscribersMap.set(cmdId, subscriber);
        subscriptionCommand.cmdId = cmdId;
        if (subscriptionCommand instanceof SubscriptionCmd) {
          if (subscriptionCommand.getType() === TelemetryFeature.TIMESERIES) {
            this.cmdsWrapper.tsSubCmds.push(subscriptionCommand as TimeseriesSubscriptionCmd);
          } else {
            this.cmdsWrapper.attrSubCmds.push(subscriptionCommand as AttributesSubscriptionCmd);
          }
        } else if (subscriptionCommand instanceof GetHistoryCmd) {
          this.cmdsWrapper.historyCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof EntityDataCmd) {
          this.cmdsWrapper.entityDataCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof AlarmDataCmd) {
          this.cmdsWrapper.alarmDataCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof EntityCountCmd) {
          this.cmdsWrapper.entityCountCmds.push(subscriptionCommand);
        }
      }
    );
    this.subscribersCount++;
    this.publishCommands();
  }

  public update(subscriber: TelemetrySubscriber) {
    if (!this.isReconnect) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand.cmdId && subscriptionCommand instanceof EntityDataCmd) {
            this.cmdsWrapper.entityDataCmds.push(subscriptionCommand);
          }
        }
      );
      this.publishCommands();
    }
  }

  public unsubscribe(subscriber: TelemetrySubscriber) {
    if (this.isActive) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand instanceof SubscriptionCmd) {
            subscriptionCommand.unsubscribe = true;
            if (subscriptionCommand.getType() === TelemetryFeature.TIMESERIES) {
              this.cmdsWrapper.tsSubCmds.push(subscriptionCommand as TimeseriesSubscriptionCmd);
            } else {
              this.cmdsWrapper.attrSubCmds.push(subscriptionCommand as AttributesSubscriptionCmd);
            }
          } else if (subscriptionCommand instanceof EntityDataCmd) {
            const entityDataUnsubscribeCmd = new EntityDataUnsubscribeCmd();
            entityDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdsWrapper.entityDataUnsubscribeCmds.push(entityDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof AlarmDataCmd) {
            const alarmDataUnsubscribeCmd = new AlarmDataUnsubscribeCmd();
            alarmDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdsWrapper.alarmDataUnsubscribeCmds.push(alarmDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof EntityCountCmd) {
            const entityCountUnsubscribeCmd = new EntityCountUnsubscribeCmd();
            entityCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdsWrapper.entityCountUnsubscribeCmds.push(entityCountUnsubscribeCmd);
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
      this.dataStream.next(this.cmdsWrapper.preparePublishCommands(MAX_PUBLISH_COMMANDS));
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
              this.authService.logout(true);
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
    const uri = `${this.telemetryUri}?token=${token}`;
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
          this.onMessage(message as WebsocketDataMsg);
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

  private onMessage(message: WebsocketDataMsg) {
    if (message.errorCode) {
      this.showWsError(message.errorCode, message.errorMsg);
    } else {
      let subscriber: TelemetrySubscriber;
      if (isEntityDataUpdateMsg(message)) {
        subscriber = this.subscribersMap.get(message.cmdId);
        if (subscriber) {
          subscriber.onEntityData(new EntityDataUpdate(message));
        }
      } else if (isAlarmDataUpdateMsg(message)) {
        subscriber = this.subscribersMap.get(message.cmdId);
        if (subscriber) {
          subscriber.onAlarmData(new AlarmDataUpdate(message));
        }
      } else if (isEntityCountUpdateMsg(message)) {
        subscriber = this.subscribersMap.get(message.cmdId);
        if (subscriber) {
          subscriber.onEntityCount(new EntityCountUpdate(message));
        }
      } else if (message.subscriptionId) {
        subscriber = this.subscribersMap.get(message.subscriptionId);
        if (subscriber) {
          subscriber.onData(new SubscriptionUpdate(message));
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
    if (closeEvent && closeEvent.code > 1001 && closeEvent.code !== 1006) {
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
      message += `WebSocket Error: error code - ${errorCode}.`;
    }
    this.store.dispatch(new ActionNotificationShow(
      {
        message, type: 'error'
      }));
  }

}

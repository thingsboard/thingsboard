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

import { Inject, Injectable, NgZone } from '@angular/core';
import {
  AlarmCountCmd,
  AlarmCountUnsubscribeCmd,
  AlarmCountUpdate,
  AlarmDataCmd,
  AlarmDataUnsubscribeCmd,
  AlarmDataUpdate,
  AlarmStatusCmd,
  AlarmStatusUnsubscribeCmd,
  AlarmStatusUpdate,
  EntityCountCmd,
  EntityCountUnsubscribeCmd,
  EntityCountUpdate,
  EntityDataCmd,
  EntityDataUnsubscribeCmd,
  EntityDataUpdate,
  isAlarmCountUpdateMsg,
  isAlarmDataUpdateMsg,
  isAlarmStatusUpdateMsg,
  isEntityCountUpdateMsg,
  isEntityDataUpdateMsg,
  isNotificationCountUpdateMsg,
  isNotificationsUpdateMsg,
  MarkAllAsReadCmd,
  MarkAsReadCmd,
  NotificationCountUpdate,
  NotificationSubscriber,
  NotificationsUpdate,
  SubscriptionCmd,
  SubscriptionUpdate,
  TelemetryPluginCmdsWrapper,
  TelemetrySubscriber,
  UnreadCountSubCmd,
  UnreadSubCmd,
  UnsubscribeCmd,
  WebsocketDataMsg
} from '@app/shared/models/telemetry/telemetry.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { WINDOW } from '@core/services/window.service';
import { WebsocketService } from '@core/ws/websocket.service';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class TelemetryWebsocketService extends WebsocketService<TelemetrySubscriber> {

  cmdWrapper: TelemetryPluginCmdsWrapper;

  constructor(protected store: Store<AppState>,
              protected authService: AuthService,
              protected ngZone: NgZone,
              @Inject(WINDOW) protected window: Window) {
    super(store, authService, ngZone, 'api/ws', new TelemetryPluginCmdsWrapper(), window);
  }

  public subscribe(subscriber: TelemetrySubscriber) {
    this.isActive = true;
    subscriber.subscriptionCommands.forEach(
      (subscriptionCommand) => {
        const cmdId = this.nextCmdId();
        if (!(subscriptionCommand instanceof MarkAsReadCmd) && !(subscriptionCommand instanceof MarkAllAsReadCmd)) {
          this.subscribersMap.set(cmdId, subscriber);
        }
        subscriptionCommand.cmdId = cmdId;
        this.cmdWrapper.cmds.push(subscriptionCommand);
      }
    );
    this.subscribersCount++;
    this.publishCommands();
  }

  public update(subscriber: TelemetrySubscriber) {
    if (!this.isReconnect) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand.cmdId && (subscriptionCommand instanceof EntityDataCmd || subscriptionCommand instanceof UnreadSubCmd)) {
            this.cmdWrapper.cmds.push(subscriptionCommand);
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
            this.cmdWrapper.cmds.push(subscriptionCommand);
          } else if (subscriptionCommand instanceof EntityDataCmd) {
            const entityDataUnsubscribeCmd = new EntityDataUnsubscribeCmd();
            entityDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.cmds.push(entityDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof AlarmDataCmd) {
            const alarmDataUnsubscribeCmd = new AlarmDataUnsubscribeCmd();
            alarmDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.cmds.push(alarmDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof EntityCountCmd) {
            const entityCountUnsubscribeCmd = new EntityCountUnsubscribeCmd();
            entityCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.cmds.push(entityCountUnsubscribeCmd);
          } else if (subscriptionCommand instanceof AlarmCountCmd) {
            const alarmCountUnsubscribeCmd = new AlarmCountUnsubscribeCmd();
            alarmCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.cmds.push(alarmCountUnsubscribeCmd);
          } else if (subscriptionCommand instanceof AlarmStatusCmd) {
            const alarmCountUnsubscribeCmd = new AlarmStatusUnsubscribeCmd();
            alarmCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.cmds.push(alarmCountUnsubscribeCmd);
          } else if (subscriptionCommand instanceof UnreadCountSubCmd || subscriptionCommand instanceof UnreadSubCmd) {
            const notificationsUnsubCmds = new UnsubscribeCmd();
            notificationsUnsubCmds.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.cmds.push(notificationsUnsubCmds);
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

  processOnMessage(message: WebsocketDataMsg) {
    let subscriber: TelemetrySubscriber | NotificationSubscriber;
    if ('cmdId' in message && message.cmdId) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber instanceof NotificationSubscriber) {
        if (isNotificationCountUpdateMsg(message)) {
          subscriber.onNotificationCountUpdate(new NotificationCountUpdate(message));
        } else if (isNotificationsUpdateMsg(message)) {
          subscriber.onNotificationsUpdate(new NotificationsUpdate(message));
        }
      } else if (subscriber instanceof TelemetrySubscriber) {
        if (isEntityDataUpdateMsg(message)) {
          subscriber.onEntityData(new EntityDataUpdate(message));
        } else if (isAlarmDataUpdateMsg(message)) {
          subscriber.onAlarmData(new AlarmDataUpdate(message));
        } else if (isEntityCountUpdateMsg(message)) {
          subscriber.onEntityCount(new EntityCountUpdate(message));
        } else if (isAlarmCountUpdateMsg(message)) {
          subscriber.onAlarmCount(new AlarmCountUpdate(message));
        } else if (isAlarmStatusUpdateMsg(message)) {
          subscriber.onAlarmStatus(new AlarmStatusUpdate(message))
        }
      }
    } else if ('subscriptionId' in message && message.subscriptionId) {
      subscriber = this.subscribersMap.get(message.subscriptionId) as TelemetrySubscriber;
      if (subscriber) {
        subscriber.onData(new SubscriptionUpdate(message));
      }
    }
  }

}

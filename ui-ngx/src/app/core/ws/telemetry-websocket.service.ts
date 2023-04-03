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
import {
  AlarmDataCmd,
  AlarmDataUnsubscribeCmd,
  AlarmDataUpdate,
  AttributesSubscriptionCmd,
  EntityCountCmd,
  EntityCountUnsubscribeCmd,
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
  TelemetrySubscriber,
  TimeseriesSubscriptionCmd,
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
    super(store, authService, ngZone, 'api/ws/plugins/telemetry', new TelemetryPluginCmdsWrapper(), window);
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
            this.cmdWrapper.tsSubCmds.push(subscriptionCommand as TimeseriesSubscriptionCmd);
          } else {
            this.cmdWrapper.attrSubCmds.push(subscriptionCommand as AttributesSubscriptionCmd);
          }
        } else if (subscriptionCommand instanceof GetHistoryCmd) {
          this.cmdWrapper.historyCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof EntityDataCmd) {
          this.cmdWrapper.entityDataCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof AlarmDataCmd) {
          this.cmdWrapper.alarmDataCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof EntityCountCmd) {
          this.cmdWrapper.entityCountCmds.push(subscriptionCommand);
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
            this.cmdWrapper.entityDataCmds.push(subscriptionCommand);
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
              this.cmdWrapper.tsSubCmds.push(subscriptionCommand as TimeseriesSubscriptionCmd);
            } else {
              this.cmdWrapper.attrSubCmds.push(subscriptionCommand as AttributesSubscriptionCmd);
            }
          } else if (subscriptionCommand instanceof EntityDataCmd) {
            const entityDataUnsubscribeCmd = new EntityDataUnsubscribeCmd();
            entityDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.entityDataUnsubscribeCmds.push(entityDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof AlarmDataCmd) {
            const alarmDataUnsubscribeCmd = new AlarmDataUnsubscribeCmd();
            alarmDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.alarmDataUnsubscribeCmds.push(alarmDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof EntityCountCmd) {
            const entityCountUnsubscribeCmd = new EntityCountUnsubscribeCmd();
            entityCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.entityCountUnsubscribeCmds.push(entityCountUnsubscribeCmd);
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

}

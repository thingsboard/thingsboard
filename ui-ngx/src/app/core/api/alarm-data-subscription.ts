///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  AlarmDataCmd,
  DataKeyType,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { DatasourceType } from '@shared/models/widget.models';
import {
  AlarmData,
  AlarmDataPageLink,
  EntityFilter,
  EntityKey,
  EntityKeyType,
  KeyFilter
} from '@shared/models/query/query.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { AlarmDataListener } from '@core/api/alarm-data.service';
import { PageData } from '@shared/models/page/page-data';
import { deepClone, isDefined, isDefinedAndNotNull, isObject } from '@core/utils';
import { simulatedAlarm } from '@shared/models/alarm.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';

export interface AlarmSubscriptionDataKey {
  name: string;
  type: DataKeyType;
}

export interface AlarmDataSubscriptionOptions {
  datasourceType: DatasourceType;
  dataKeys: Array<AlarmSubscriptionDataKey>;
  entityFilter?: EntityFilter;
  pageLink?: AlarmDataPageLink;
  keyFilters?: Array<KeyFilter>;
  additionalKeyFilters?: Array<KeyFilter>;
  subscriptionTimewindow?: SubscriptionTimewindow;
}

export class AlarmDataSubscription {

  private alarmDataSubscriptionOptions = this.listener.alarmDataSubscriptionOptions;
  private datasourceType: DatasourceType = this.alarmDataSubscriptionOptions.datasourceType;

  private history: boolean;
  private realtime: boolean;

  private subscriber: TelemetrySubscriber;
  private alarmDataCommand: AlarmDataCmd;

  private pageData: PageData<AlarmData>;
  private prematureUpdates: Array<Array<AlarmData>>;
  private alarmIdToDataIndex: {[id: string]: number};

  private subsTw: SubscriptionTimewindow;

  constructor(private listener: AlarmDataListener,
              private telemetryService: TelemetryWebsocketService) {
  }

  public unsubscribe() {
    if (this.datasourceType === DatasourceType.entity) {
      if (this.subscriber) {
        this.subscriber.unsubscribe();
        this.subscriber = null;
      }
    }
  }

  public subscribe() {
    this.subsTw = this.alarmDataSubscriptionOptions.subscriptionTimewindow;
    this.history = this.alarmDataSubscriptionOptions.subscriptionTimewindow &&
      isObject(this.alarmDataSubscriptionOptions.subscriptionTimewindow.fixedWindow);
    this.realtime = this.alarmDataSubscriptionOptions.subscriptionTimewindow &&
      isDefinedAndNotNull(this.alarmDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs);
    if (this.datasourceType === DatasourceType.entity) {
      this.subscriber = new TelemetrySubscriber(this.telemetryService);
      this.alarmDataCommand = new AlarmDataCmd();

      const alarmFields: Array<EntityKey> =
        this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.alarm).map(
          dataKey => ({ type: EntityKeyType.ALARM_FIELD, key: dataKey.name })
        );

      const entityFields: Array<EntityKey> =
        this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.entityField).map(
          dataKey => ({ type: EntityKeyType.ENTITY_FIELD, key: dataKey.name })
        );

      const attrFields = this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.attribute).map(
        dataKey => ({ type: EntityKeyType.ATTRIBUTE, key: dataKey.name })
      );
      const tsFields = this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.timeseries).map(
        dataKey => ({ type: EntityKeyType.TIME_SERIES, key: dataKey.name })
      );
      const latestValues = attrFields.concat(tsFields);

      let keyFilters = this.alarmDataSubscriptionOptions.keyFilters;
      if (this.alarmDataSubscriptionOptions.additionalKeyFilters) {
        if (keyFilters) {
          keyFilters = keyFilters.concat(this.alarmDataSubscriptionOptions.additionalKeyFilters);
        } else {
          keyFilters = this.alarmDataSubscriptionOptions.additionalKeyFilters;
        }
      }
      this.alarmDataCommand.query = {
        entityFilter: this.alarmDataSubscriptionOptions.entityFilter,
        pageLink: deepClone(this.alarmDataSubscriptionOptions.pageLink),
        keyFilters,
        alarmFields,
        entityFields,
        latestValues
      };
      if (this.history) {
        this.alarmDataCommand.query.pageLink.startTs = this.subsTw.fixedWindow.startTimeMs;
        this.alarmDataCommand.query.pageLink.endTs = this.subsTw.fixedWindow.endTimeMs;
      } else {
        this.alarmDataCommand.query.pageLink.timeWindow = this.subsTw.realtimeWindowMs;
      }

      this.subscriber.setTsOffset(this.subsTw.tsOffset);
      this.subscriber.subscriptionCommands.push(this.alarmDataCommand);

      this.subscriber.alarmData$.subscribe((alarmDataUpdate) => {
        if (alarmDataUpdate.data) {
          this.onPageData(alarmDataUpdate.data, alarmDataUpdate.allowedEntities, alarmDataUpdate.totalEntities);
          if (this.prematureUpdates) {
            for (const update of this.prematureUpdates) {
              this.onDataUpdate(update);
            }
            this.prematureUpdates = null;
          }
        } else if (alarmDataUpdate.update) {
          if (!this.pageData) {
            if (!this.prematureUpdates) {
              this.prematureUpdates = [];
            }
            this.prematureUpdates.push(alarmDataUpdate.update);
          } else {
            this.onDataUpdate(alarmDataUpdate.update);
          }
        }
      });

      this.subscriber.subscribe();

    } else if (this.datasourceType === DatasourceType.function) {
      const alarm = deepClone(simulatedAlarm);
      alarm.createdTime += this.subsTw.tsOffset;
      alarm.startTs += this.subsTw.tsOffset;
      const pageData: PageData<AlarmData> = {
        data: [{...alarm, entityId: '1', latest: {}}],
        hasNext: false,
        totalElements: 1,
        totalPages: 1
      };
      this.onPageData(pageData, 1024, 1);
    }
  }

  private resetData() {
    this.alarmIdToDataIndex = {};
    for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
      const alarmData = this.pageData.data[dataIndex];
      this.alarmIdToDataIndex[alarmData.id.id] = dataIndex;
    }
  }

  private onPageData(pageData: PageData<AlarmData>, allowedEntities: number, totalEntities: number) {
    this.pageData = pageData;
    this.resetData();
    this.listener.alarmsLoaded(pageData, allowedEntities, totalEntities);
  }

  private onDataUpdate(update: Array<AlarmData>) {
    for (const alarmData of update) {
      const dataIndex = this.alarmIdToDataIndex[alarmData.id.id];
      if (isDefined(dataIndex) && dataIndex >= 0) {
        this.pageData.data[dataIndex] = alarmData;
      }
    }
    this.listener.alarmsUpdated(update, this.pageData);
  }

}

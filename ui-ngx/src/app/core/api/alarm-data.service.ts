///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { Datasource, DatasourceType } from '@shared/models/widget.models';
import { PageData } from '@shared/models/page/page-data';
import { AlarmData, AlarmDataPageLink, KeyFilter } from '@shared/models/query/query.models';
import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  AlarmDataSubscription,
  AlarmDataSubscriptionOptions,
  AlarmSubscriptionDataKey
} from '@core/api/alarm-data-subscription';
import { deepClone } from '@core/utils';

export interface AlarmDataListener {
  subscriptionTimewindow?: SubscriptionTimewindow;
  alarmSource: Datasource;
  alarmsLoaded: (pageData: PageData<AlarmData>, allowedEntities: number, totalEntities: number) => void;
  alarmsUpdated: (update: Array<AlarmData>, pageData: PageData<AlarmData>) => void;
  alarmDataSubscriptionOptions?: AlarmDataSubscriptionOptions;
  subscription?: AlarmDataSubscription;
}

@Injectable({
  providedIn: 'root'
})
export class AlarmDataService {

  constructor(private telemetryService: TelemetryWebsocketService) {}


  public subscribeForAlarms(listener: AlarmDataListener,
                            pageLink: AlarmDataPageLink,
                            keyFilters: KeyFilter[]) {
    const alarmSource = listener.alarmSource;
    listener.alarmDataSubscriptionOptions = this.createAlarmSubscriptionOptions(listener, pageLink, keyFilters);
    if (alarmSource.type === DatasourceType.entity && (!alarmSource.entityFilter || !pageLink)) {
      return;
    }
    listener.subscription = new AlarmDataSubscription(listener, this.telemetryService);
    return listener.subscription.subscribe();
  }

  public stopSubscription(listener: AlarmDataListener) {
    if (listener.subscription) {
      listener.subscription.unsubscribe();
    }
  }

  private createAlarmSubscriptionOptions(listener: AlarmDataListener,
                                         pageLink: AlarmDataPageLink,
                                         additionalKeyFilters: KeyFilter[]): AlarmDataSubscriptionOptions {
    const alarmSource = listener.alarmSource;
    const alarmSubscriptionDataKeys: Array<AlarmSubscriptionDataKey> = [];
    alarmSource.dataKeys.forEach((dataKey) => {
      const alarmSubscriptionDataKey: AlarmSubscriptionDataKey = {
        name: dataKey.name,
        type: dataKey.type
      };
      alarmSubscriptionDataKeys.push(alarmSubscriptionDataKey);
    });
    const alarmDataSubscriptionOptions: AlarmDataSubscriptionOptions = {
      datasourceType: alarmSource.type,
      dataKeys: alarmSubscriptionDataKeys,
      subscriptionTimewindow: deepClone(listener.subscriptionTimewindow)
    };
    if (alarmDataSubscriptionOptions.datasourceType === DatasourceType.entity) {
      alarmDataSubscriptionOptions.entityFilter = alarmSource.entityFilter;
      alarmDataSubscriptionOptions.pageLink = pageLink;
      alarmDataSubscriptionOptions.keyFilters = alarmSource.keyFilters;
      alarmDataSubscriptionOptions.additionalKeyFilters = additionalKeyFilters;
    }
    return alarmDataSubscriptionOptions;
  }

}

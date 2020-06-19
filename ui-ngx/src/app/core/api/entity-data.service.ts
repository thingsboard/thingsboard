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

import { DataSetHolder, Datasource, DatasourceType, widgetType } from '@shared/models/widget.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { EntityData, EntityDataPageLink, EntityFilter, KeyFilter } from '@shared/models/query/query.models';
import { PageData } from '@shared/models/page/page-data';
import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { UtilsService } from '@core/services/utils.service';
import { SubscriptionDataKey } from '@core/api/datasource-subcription';
import { deepClone, objectHashCode } from '@core/utils';
import { EntityDataSubscription, EntityDataSubscriptionOptions } from '@core/api/entity-data-subscription';

export interface EntityDataListener {
  subscriptionType: widgetType;
  subscriptionTimewindow: SubscriptionTimewindow;
  configDatasource: Datasource;
  configDatasourceIndex: number;
  dataLoaded: (pageData: PageData<EntityData>, data: Array<Array<DataSetHolder>>, datasourceIndex: number) => void;
  dataUpdated: (data: DataSetHolder, datasourceIndex: number, dataIndex: number, dataKeyIndex: number, detectChanges: boolean) => void;
  updateRealtimeSubscription: () => SubscriptionTimewindow;
  setRealtimeSubscription: (subscriptionTimewindow: SubscriptionTimewindow) => void;
  entityDataSubscriptionKey?: number;
}

@Injectable({
  providedIn: 'root'
})
export class EntityDataService {

  private subscriptions: {[entityDataSubscriptionKey: string]: EntityDataSubscription} = {};

  constructor(private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService) {}

  public subscribeToEntityData(listener: EntityDataListener) {
    const datasource = listener.configDatasource;
    if (datasource.type === DatasourceType.entity && (!datasource.entityFilter || !datasource.pageLink)) {
      return;
    }
    const subscriptionDataKeys: Array<SubscriptionDataKey> = [];
    datasource.dataKeys.forEach((dataKey) => {
      const subscriptionDataKey: SubscriptionDataKey = {
        name: dataKey.name,
        type: dataKey.type,
        funcBody: dataKey.funcBody,
        postFuncBody: dataKey.postFuncBody
      };
      subscriptionDataKeys.push(subscriptionDataKey);
    });

    const entityDataSubscriptionOptions: EntityDataSubscriptionOptions = {
      datasourceType: datasource.type,
      dataKeys: subscriptionDataKeys,
      type: listener.subscriptionType
    };

    if (listener.subscriptionType === widgetType.timeseries) {
      entityDataSubscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
    }
    if (entityDataSubscriptionOptions.datasourceType === DatasourceType.entity) {
      entityDataSubscriptionOptions.entityFilter = datasource.entityFilter;
      entityDataSubscriptionOptions.pageLink = datasource.pageLink;
      entityDataSubscriptionOptions.keyFilters = datasource.keyFilters;
    }
    listener.entityDataSubscriptionKey = objectHashCode(entityDataSubscriptionOptions);
    let subscription: EntityDataSubscription;
    if (this.subscriptions[listener.entityDataSubscriptionKey]) {
      subscription = this.subscriptions[listener.entityDataSubscriptionKey];
      subscription.syncListener(listener);
    } else {
      subscription = new EntityDataSubscription(entityDataSubscriptionOptions,
        this.telemetryService, this.utils);
      this.subscriptions[listener.entityDataSubscriptionKey] = subscription;
      subscription.start();
    }
    subscription.addListener(listener);
  }

  public unsubscribeFromDatasource(listener: EntityDataListener) {
    if (listener.entityDataSubscriptionKey) {
      const subscription = this.subscriptions[listener.entityDataSubscriptionKey];
      if (subscription) {
        subscription.removeListener(listener);
        if (!subscription.hasListeners()) {
          subscription.unsubscribe();
          delete this.subscriptions[listener.entityDataSubscriptionKey];
        }
      }
      listener.entityDataSubscriptionKey = null;
    }
  }

}

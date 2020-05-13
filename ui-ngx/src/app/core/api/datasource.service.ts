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

import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@app/shared/models/entity-type.models';
import { DataSetHolder, Datasource, DatasourceType, widgetType } from '@shared/models/widget.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import {
  DatasourceSubscription,
  DatasourceSubscriptionOptions,
  SubscriptionDataKey
} from '@core/api/datasource-subcription';
import { deepClone, objectHashCode } from '@core/utils';

export interface DatasourceListener {
  subscriptionType: widgetType;
  subscriptionTimewindow: SubscriptionTimewindow;
  datasource: Datasource;
  entityType: EntityType;
  entityId: string;
  datasourceIndex: number;
  dataUpdated: (data: DataSetHolder, datasourceIndex: number, dataKeyIndex: number, detectChanges: boolean) => void;
  updateRealtimeSubscription: () => SubscriptionTimewindow;
  setRealtimeSubscription: (subscriptionTimewindow: SubscriptionTimewindow) => void;
  datasourceSubscriptionKey?: number;
}

@Injectable({
  providedIn: 'root'
})
export class DatasourceService {

  private subscriptions: {[datasourceSubscriptionKey: string]: DatasourceSubscription} = {};

  constructor(private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService) {}

  public subscribeToDatasource(listener: DatasourceListener) {
    const datasource = listener.datasource;
    if (datasource.type === DatasourceType.entity && (!listener.entityId || !listener.entityType)) {
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

    const datasourceSubscriptionOptions: DatasourceSubscriptionOptions = {
      datasourceType: datasource.type,
      dataKeys: subscriptionDataKeys,
      type: listener.subscriptionType
    };

    if (listener.subscriptionType === widgetType.timeseries) {
      datasourceSubscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
    }
    if (datasourceSubscriptionOptions.datasourceType === DatasourceType.entity) {
      datasourceSubscriptionOptions.entityType = listener.entityType;
      datasourceSubscriptionOptions.entityId = listener.entityId;
    }
    listener.datasourceSubscriptionKey = objectHashCode(datasourceSubscriptionOptions);
    let subscription: DatasourceSubscription;
    if (this.subscriptions[listener.datasourceSubscriptionKey]) {
      subscription = this.subscriptions[listener.datasourceSubscriptionKey];
      subscription.syncListener(listener);
    } else {
      subscription = new DatasourceSubscription(datasourceSubscriptionOptions,
                                                this.telemetryService, this.utils);
      this.subscriptions[listener.datasourceSubscriptionKey] = subscription;
      subscription.start();
    }
    subscription.addListener(listener);
  }

  public unsubscribeFromDatasource(listener: DatasourceListener) {
    if (listener.datasourceSubscriptionKey) {
      const subscription = this.subscriptions[listener.datasourceSubscriptionKey];
      if (subscription) {
        subscription.removeListener(listener);
        if (!subscription.hasListeners()) {
          subscription.unsubscribe();
          delete this.subscriptions[listener.datasourceSubscriptionKey];
        }
      }
      listener.datasourceSubscriptionKey = null;
    }
  }
}

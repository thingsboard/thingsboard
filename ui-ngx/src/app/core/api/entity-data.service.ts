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
import { EntityData, EntityDataPageLink, KeyFilter } from '@shared/models/query/query.models';
import { PageData } from '@shared/models/page/page-data';
import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { UtilsService } from '@core/services/utils.service';
import { deepClone } from '@core/utils';
import {
  EntityDataSubscription,
  EntityDataSubscriptionOptions,
  SubscriptionDataKey
} from '@core/api/entity-data-subscription';
import { Observable, of } from 'rxjs';

export interface EntityDataListener {
  subscriptionType: widgetType;
  subscriptionTimewindow?: SubscriptionTimewindow;
  configDatasource: Datasource;
  configDatasourceIndex: number;
  dataLoaded: (pageData: PageData<EntityData>, data: Array<Array<DataSetHolder>>, datasourceIndex: number) => void;
  dataUpdated: (data: DataSetHolder, datasourceIndex: number, dataIndex: number, dataKeyIndex: number, detectChanges: boolean) => void;
  initialPageDataChanged?: (nextPageData: PageData<EntityData>) => void;
  updateRealtimeSubscription?: () => SubscriptionTimewindow;
  setRealtimeSubscription?: (subscriptionTimewindow: SubscriptionTimewindow) => void;
  subscription?: EntityDataSubscription;
}

export interface EntityDataLoadResult {
  pageData: PageData<EntityData>;
  data: Array<Array<DataSetHolder>>;
  datasourceIndex: number;
}

@Injectable({
  providedIn: 'root'
})
export class EntityDataService {

  constructor(private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService) {}

  public prepareSubscription(listener: EntityDataListener): Observable<EntityDataLoadResult> {
    const datasource = listener.configDatasource;
    if (datasource.type === DatasourceType.entity && (!datasource.entityFilter || !datasource.pageLink)) {
      return of(null);
    }
    listener.subscription = this.createSubscription(listener,
      datasource.pageLink, datasource.keyFilters,
      false);
    return listener.subscription.subscribe();
  }

  public startSubscription(listener: EntityDataListener) {
    if (listener.subscription) {
      if (listener.subscriptionType === widgetType.timeseries) {
        listener.subscription.entityDataSubscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
      }
      listener.subscription.start();
    }
  }

  public subscribeForPaginatedData(listener: EntityDataListener,
                                   pageLink: EntityDataPageLink,
                                   keyFilters: KeyFilter[]): Observable<EntityDataLoadResult> {
    const datasource = listener.configDatasource;
    if (datasource.type === DatasourceType.entity && (!datasource.entityFilter || !pageLink)) {
      return of(null);
    }
    listener.subscription = this.createSubscription(listener,
      pageLink, keyFilters,  true);
    if (listener.subscriptionType === widgetType.timeseries) {
      listener.subscription.entityDataSubscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
    }
    return listener.subscription.subscribe();
  }

  public stopSubscription(listener: EntityDataListener) {
    if (listener.subscription) {
      listener.subscription.unsubscribe();
    }
  }

  private createSubscription(listener: EntityDataListener,
                             pageLink: EntityDataPageLink,
                             keyFilters: KeyFilter[],
                             isPaginatedDataSubscription: boolean): EntityDataSubscription {
    const datasource = listener.configDatasource;
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
    if (entityDataSubscriptionOptions.datasourceType === DatasourceType.entity) {
      entityDataSubscriptionOptions.entityFilter = datasource.entityFilter;
      entityDataSubscriptionOptions.pageLink = pageLink;
      entityDataSubscriptionOptions.keyFilters = keyFilters;
    }
    entityDataSubscriptionOptions.isPaginatedDataSubscription = isPaginatedDataSubscription;
    return new EntityDataSubscription(entityDataSubscriptionOptions,
      listener, this.telemetryService, this.utils);
  }

}

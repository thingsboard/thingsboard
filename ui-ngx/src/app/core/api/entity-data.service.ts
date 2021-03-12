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

import { DataSetHolder, Datasource, DatasourceType, widgetType } from '@shared/models/widget.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { EntityData, EntityDataPageLink, KeyFilter } from '@shared/models/query/query.models';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
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
  dataLoaded: (pageData: PageData<EntityData>,
               data: Array<Array<DataSetHolder>>,
               datasourceIndex: number, pageLink: EntityDataPageLink) => void;
  dataUpdated: (data: DataSetHolder, datasourceIndex: number, dataIndex: number, dataKeyIndex: number, detectChanges: boolean) => void;
  initialPageDataChanged?: (nextPageData: PageData<EntityData>) => void;
  updateRealtimeSubscription?: () => SubscriptionTimewindow;
  setRealtimeSubscription?: (subscriptionTimewindow: SubscriptionTimewindow) => void;
  subscriptionOptions?: EntityDataSubscriptionOptions;
  subscription?: EntityDataSubscription;
}

export interface EntityDataLoadResult {
  pageData: PageData<EntityData>;
  data: Array<Array<DataSetHolder>>;
  datasourceIndex: number;
  pageLink: EntityDataPageLink;
}

@Injectable({
  providedIn: 'root'
})
export class EntityDataService {

  constructor(private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService) {}

  private static isUnresolvedDatasource(datasource: Datasource, pageLink: EntityDataPageLink): boolean {
    if (datasource.type === DatasourceType.entity) {
      return !datasource.entityFilter || !pageLink;
    } else if (datasource.type === DatasourceType.entityCount) {
      return !datasource.entityFilter;
    } else {
      return false;
    }
  }

  public prepareSubscription(listener: EntityDataListener,
                             ignoreDataUpdateOnIntervalTick = false): Observable<EntityDataLoadResult> {
    const datasource = listener.configDatasource;
    listener.subscriptionOptions = this.createSubscriptionOptions(
      datasource,
      listener.subscriptionType,
      datasource.pageLink,
      datasource.keyFilters,
      null,
      false,
      ignoreDataUpdateOnIntervalTick);
    if (EntityDataService.isUnresolvedDatasource(datasource, datasource.pageLink)) {
      return of(null);
    }
    listener.subscription = new EntityDataSubscription(listener, this.telemetryService, this.utils);
    return listener.subscription.subscribe();
  }

  public startSubscription(listener: EntityDataListener) {
    if (listener.subscription) {
      if (listener.subscriptionType === widgetType.timeseries) {
        listener.subscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
      }
      listener.subscription.start();
    }
  }

  public subscribeForPaginatedData(listener: EntityDataListener,
                                   pageLink: EntityDataPageLink,
                                   keyFilters: KeyFilter[],
                                   ignoreDataUpdateOnIntervalTick = false): Observable<EntityDataLoadResult> {
    const datasource = listener.configDatasource;
    listener.subscriptionOptions = this.createSubscriptionOptions(
      datasource,
      listener.subscriptionType,
      pageLink,
      datasource.keyFilters,
      keyFilters,
      true,
      ignoreDataUpdateOnIntervalTick);
    if (EntityDataService.isUnresolvedDatasource(datasource, pageLink)) {
      listener.dataLoaded(emptyPageData<EntityData>(), [],
        listener.configDatasourceIndex, listener.subscriptionOptions.pageLink);
      return of(null);
    }
    listener.subscription = new EntityDataSubscription(listener, this.telemetryService, this.utils);
    if (listener.subscriptionType === widgetType.timeseries) {
      listener.subscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
    }
    return listener.subscription.subscribe();
  }

  public stopSubscription(listener: EntityDataListener) {
    if (listener.subscription) {
      listener.subscription.unsubscribe();
    }
  }

  private createSubscriptionOptions(datasource: Datasource,
                                    subscriptionType: widgetType,
                                    pageLink: EntityDataPageLink,
                                    keyFilters: KeyFilter[],
                                    additionalKeyFilters: KeyFilter[],
                                    isPaginatedDataSubscription: boolean,
                                    ignoreDataUpdateOnIntervalTick: boolean): EntityDataSubscriptionOptions {
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
      type: subscriptionType
    };
    if (entityDataSubscriptionOptions.datasourceType === DatasourceType.entity ||
      entityDataSubscriptionOptions.datasourceType === DatasourceType.entityCount) {
      entityDataSubscriptionOptions.entityFilter = datasource.entityFilter;
      entityDataSubscriptionOptions.keyFilters = keyFilters;
      entityDataSubscriptionOptions.additionalKeyFilters = additionalKeyFilters;
      if (entityDataSubscriptionOptions.datasourceType === DatasourceType.entity) {
        entityDataSubscriptionOptions.pageLink = pageLink;
      }
    }
    entityDataSubscriptionOptions.isPaginatedDataSubscription = isPaginatedDataSubscription;
    entityDataSubscriptionOptions.ignoreDataUpdateOnIntervalTick = ignoreDataUpdateOnIntervalTick;
    return entityDataSubscriptionOptions;
  }
}

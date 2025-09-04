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

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeData, AttributeScope, DataSortOrder, TimeseriesData } from '@shared/models/telemetry/telemetry.models';
import { isDefinedAndNotNull } from '@core/utils';
import { AggregationType } from '@shared/models/time/time.models';

@Injectable({
  providedIn: 'root'
})
export class AttributeService {

  constructor(
    private http: HttpClient
  ) { }

  public getEntityAttributes(entityId: EntityId, attributeScope?: AttributeScope,
                             keys?: Array<string>, config?: RequestConfig): Observable<Array<AttributeData>> {
    let url = `/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/values/attributes`;

    if (attributeScope) {
      url += `/${attributeScope}`;
    }

    if (keys && keys.length) {
      url += `?keys=${keys.join(',')}`;
    }

    return this.http.get<Array<AttributeData>>(url, defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityAttributes(entityId: EntityId, attributeScope: AttributeScope, attributes: Array<AttributeData>,
                                config?: RequestConfig): Observable<any> {
    const keys = attributes.map(attribute => encodeURIComponent(attribute.key)).join(',');
    return this.http.delete(`/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/${attributeScope}` +
      `?keys=${keys}`,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityTimeseries(entityId: EntityId, timeseries: Array<AttributeData>, deleteAllDataForKeys = false,
                                startTs?: number, endTs?: number, rewriteLatestIfDeleted = false, deleteLatest = true,
                                config?: RequestConfig): Observable<any> {
    const keys = timeseries.map(attribute => encodeURIComponent(attribute.key)).join(',');
    let url = `/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/timeseries/delete?keys=${keys}`;
    if (isDefinedAndNotNull(deleteAllDataForKeys)) {
      url += `&deleteAllDataForKeys=${deleteAllDataForKeys}`;
    }
    if (isDefinedAndNotNull(rewriteLatestIfDeleted)) {
      url += `&rewriteLatestIfDeleted=${rewriteLatestIfDeleted}`;
    }
    if (isDefinedAndNotNull(deleteLatest)) {
      url += `&deleteLatest=${deleteLatest}`;
    }
    if (isDefinedAndNotNull(startTs)) {
      url += `&startTs=${startTs}`;
    }
    if (isDefinedAndNotNull(endTs)) {
      url += `&endTs=${endTs}`;
    }
    return this.http.delete(url, defaultHttpOptionsFromConfig(config));
  }

  public saveEntityAttributes(entityId: EntityId, attributeScope: AttributeScope, attributes: Array<AttributeData>,
                              config?: RequestConfig): Observable<any> {
    const attributesData: {[key: string]: any} = {};
    const deleteAttributes: AttributeData[] = [];
    attributes.forEach((attribute) => {
      if (isDefinedAndNotNull(attribute.value)) {
        attributesData[attribute.key] = attribute.value;
      } else {
        deleteAttributes.push(attribute);
      }
    });
    let deleteEntityAttributesObservable: Observable<any>;
    if (deleteAttributes.length) {
      deleteEntityAttributesObservable = this.deleteEntityAttributes(entityId, attributeScope, deleteAttributes, config);
    } else {
      deleteEntityAttributesObservable = of(null);
    }
    let saveEntityAttributesObservable: Observable<any>;
    if (Object.keys(attributesData).length) {
      saveEntityAttributesObservable = this.http.post(`/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/${attributeScope}`,
        attributesData, defaultHttpOptionsFromConfig(config));
    } else {
      saveEntityAttributesObservable = of(null);
    }
    return forkJoin([saveEntityAttributesObservable, deleteEntityAttributesObservable]);
  }

  public saveEntityTimeseries(entityId: EntityId, timeseriesScope: string, timeseries: Array<AttributeData>,
                              config?: RequestConfig): Observable<any> {
    const timeseriesData: {[key: string]: any} = {};
    const deleteTimeseries: AttributeData[] = [];
    timeseries.forEach((attribute) => {
      if (isDefinedAndNotNull(attribute.value)) {
        timeseriesData[attribute.key] = attribute.value;
      } else {
        deleteTimeseries.push(attribute);
      }
    });
    let deleteEntityTimeseriesObservable: Observable<any>;
    if (deleteTimeseries.length) {
      deleteEntityTimeseriesObservable = this.deleteEntityTimeseries(entityId, deleteTimeseries, true,
        null, null, false, true, config);
    } else {
      deleteEntityTimeseriesObservable = of(null);
    }
    let saveEntityTimeseriesObservable: Observable<any>;
    if (Object.keys(timeseriesData).length) {
      saveEntityTimeseriesObservable =
        this.http.post(`/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/timeseries/${timeseriesScope}`,
        timeseriesData, defaultHttpOptionsFromConfig(config));
    } else {
      saveEntityTimeseriesObservable = of(null);
    }
    return forkJoin([saveEntityTimeseriesObservable, deleteEntityTimeseriesObservable]);
  }

  public getEntityTimeseries(entityId: EntityId, keys: Array<string>, startTs: number, endTs: number,
                             limit: number = 100, agg: AggregationType = AggregationType.NONE, interval?: number,
                             orderBy: DataSortOrder = DataSortOrder.DESC, useStrictDataTypes: boolean = false,
                             config?: RequestConfig): Observable<TimeseriesData> {
    let url = `/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/values/timeseries?keys=${keys.join(',')}&startTs=${startTs}&endTs=${endTs}`;
    if (isDefinedAndNotNull(limit)) {
      url += `&limit=${limit}`;
    }
    if (isDefinedAndNotNull(agg)) {
      url += `&agg=${agg}`;
    }
    if (isDefinedAndNotNull(interval)) {
      url += `&interval=${interval}`;
    }
    if (isDefinedAndNotNull(orderBy)) {
      url += `&orderBy=${orderBy}`;
    }
    if (isDefinedAndNotNull(useStrictDataTypes)) {
      url += `&useStrictDataTypes=${useStrictDataTypes}`;
    }

    return this.http.get<TimeseriesData>(url, defaultHttpOptionsFromConfig(config));
  }

  public getEntityTimeseriesLatest(entityId: EntityId, keys?: Array<string>,
                                   useStrictDataTypes = false, config?: RequestConfig): Observable<TimeseriesData> {
    let url = `/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/values/timeseries?useStrictDataTypes=${useStrictDataTypes}`;
    if (isDefinedAndNotNull(keys) && keys.length) {
      url += `&keys=${keys.join(',')}`;
    }
    return this.http.get<TimeseriesData>(url, defaultHttpOptionsFromConfig(config));
  }
}

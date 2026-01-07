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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { QueueInfo, QueueStatisticsInfo, ServiceType } from '@shared/models/queue.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class QueueService {

  constructor(
    private http: HttpClient
  ) { }

  public getQueueById(queueId: string, config?: RequestConfig): Observable<QueueInfo> {
    return this.http.get<QueueInfo>(`/api/queues/${queueId}`, defaultHttpOptionsFromConfig(config));
  }

  public getQueueByName(queueName: string, config?: RequestConfig): Observable<QueueInfo> {
    return this.http.get<QueueInfo>(`/api/queues/name/${queueName}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantQueuesByServiceType(pageLink: PageLink,
                                      serviceType: ServiceType,
                                      config?: RequestConfig): Observable<PageData<QueueInfo>> {
    return this.http.get<PageData<QueueInfo>>(`/api/queues${pageLink.toQuery()}&serviceType=${serviceType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveQueue(queue: QueueInfo, serviceType: ServiceType, config?: RequestConfig): Observable<QueueInfo> {
    return this.http.post<QueueInfo>(`/api/queues?serviceType=${serviceType}`, queue, defaultHttpOptionsFromConfig(config));
  }

  public deleteQueue(queueId: string) {
    return this.http.delete(`/api/queues/${queueId}`);
  }

  private parseQueueStatName = (queueStat: QueueStatisticsInfo) => Object.defineProperty(queueStat, 'name', {
    get() { return `${this.queueName} (${this.serviceId})`; }
  });

  public getQueueStatistics(pageLink: PageLink, config?: RequestConfig): Observable<PageData<QueueStatisticsInfo>> {
    return this.http.get<PageData<QueueStatisticsInfo>>(`/api/queueStats${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)).pipe(
        map(queueData => {
          queueData.data.map(queueStat => this.parseQueueStatName(queueStat));
          return queueData;
        })
    );
  }

  public getQueueStatisticsById(queueStatId: string, config?: RequestConfig): Observable<QueueStatisticsInfo> {
    return this.http.get<QueueStatisticsInfo>(`/api/queueStats/${queueStatId}`, defaultHttpOptionsFromConfig(config)).pipe(
      map(queueStat => this.parseQueueStatName(queueStat)));
  }

  public getQueueStatisticsByIds(queueStatIds: Array<string>, config?: RequestConfig): Observable<Array<QueueStatisticsInfo>> {
    return this.http.get<Array<QueueStatisticsInfo>>(`/api/queueStats?queueStatsIds=${queueStatIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map(queueStats => queueStats.map(queueStat => this.parseQueueStatName(queueStat))
      )
    );
  }
}

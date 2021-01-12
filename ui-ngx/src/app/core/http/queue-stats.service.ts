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

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {QueueStats} from "@shared/models/queue-stats.model";

@Injectable({
  providedIn: 'root'
})
export class QueueStatsService {

  constructor(
    private http: HttpClient
  ) { }

  public getQueueStats(pageLink: PageLink, config?: RequestConfig): Observable<PageData<QueueStats>> {
    return this.http.get<PageData<QueueStats>>(`/api/queueStats${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getSingleQueueStats(queueStatsId: string, config?: RequestConfig): Observable<QueueStats> {
    return this.http.get<QueueStats>(`/api/queueStats/${queueStatsId}`, defaultHttpOptionsFromConfig(config));
  }

  public getQueueStatsByIds(queueStatsIds: Array<string>, config?: RequestConfig): Observable<Array<QueueStats>> {
    return this.http.get<Array<QueueStats>>(`/api/queueStats?queueStatsIds=${queueStatsIds.join(',')}`, defaultHttpOptionsFromConfig(config));
  }
}

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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import {
  Alarm,
  AlarmInfo,
  AlarmQuery, AlarmQueryV2,
  AlarmSearchStatus,
  AlarmSeverity,
  AlarmStatus
} from '@shared/models/alarm.models';
import { EntitySubtype } from '@shared/models/entity-type.models';
import { PageLink } from '@shared/models/page/page-link';

@Injectable({
  providedIn: 'root'
})
export class AlarmService {

  constructor(
    private http: HttpClient
  ) { }

  public getAlarm(alarmId: string, config?: RequestConfig): Observable<Alarm> {
    return this.http.get<Alarm>(`/api/alarm/${alarmId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAlarmInfo(alarmId: string, config?: RequestConfig): Observable<AlarmInfo> {
    return this.http.get<AlarmInfo>(`/api/alarm/info/${alarmId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAlarm(alarm: Alarm, config?: RequestConfig): Observable<Alarm> {
    return this.http.post<Alarm>('/api/alarm', alarm, defaultHttpOptionsFromConfig(config));
  }

  public ackAlarm(alarmId: string, config?: RequestConfig): Observable<AlarmInfo> {
    return this.http.post<AlarmInfo>(`/api/alarm/${alarmId}/ack`, null, defaultHttpOptionsFromConfig(config));
  }

  public clearAlarm(alarmId: string, config?: RequestConfig): Observable<AlarmInfo> {
    return this.http.post<AlarmInfo>(`/api/alarm/${alarmId}/clear`, null, defaultHttpOptionsFromConfig(config));
  }

  public assignAlarm(alarmId: string, assigneeId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/alarm/${alarmId}/assign/${assigneeId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public unassignAlarm(alarmId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/alarm/${alarmId}/assign`, defaultHttpOptionsFromConfig(config));
  }

  public deleteAlarm(alarmId: string, config?: RequestConfig): Observable<boolean> {
    return this.http.delete<boolean>(`/api/alarm/${alarmId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAlarms(query: AlarmQuery,
                   config?: RequestConfig): Observable<PageData<AlarmInfo>> {
    return this.http.get<PageData<AlarmInfo>>(`/api/alarm${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAlarmsV2(query: AlarmQueryV2,
                     config?: RequestConfig): Observable<PageData<AlarmInfo>> {
    return this.http.get<PageData<AlarmInfo>>(`/api/v2/alarm${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllAlarms(query: AlarmQuery,
                      config?: RequestConfig): Observable<PageData<AlarmInfo>> {
    return this.http.get<PageData<AlarmInfo>>(`/api/alarms${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllAlarmsV2(query: AlarmQueryV2,
      config?: RequestConfig): Observable<PageData<AlarmInfo>> {
      return this.http.get<PageData<AlarmInfo>>(`/api/v2/alarms${query.toQuery()}`,
        defaultHttpOptionsFromConfig(config));
  }

  public getHighestAlarmSeverity(entityId: EntityId, alarmSearchStatus: AlarmSearchStatus, alarmStatus: AlarmStatus,
                                 config?: RequestConfig): Observable<AlarmSeverity> {
    let url = `/api/alarm/highestSeverity/${entityId.entityType}/${entityId.id}`;
    if (alarmSearchStatus) {
      url += `?searchStatus=${alarmSearchStatus}`;
    } else if (alarmStatus) {
      url += `?status=${alarmStatus}`;
    }
    return this.http.get<AlarmSeverity>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getAlarmTypes(pageLink: PageLink, config?: RequestConfig): Observable<PageData<EntitySubtype>> {
    return this.http.get<PageData<EntitySubtype>>(`/api/alarm/types${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

}

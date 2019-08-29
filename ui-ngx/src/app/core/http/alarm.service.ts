///
/// Copyright © 2016-2019 The Thingsboard Authors
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
import { defaultHttpOptions } from './http-utils';
import { Observable } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import {
  Alarm,
  AlarmInfo,
  AlarmQuery,
  AlarmSearchStatus,
  AlarmSeverity,
  AlarmStatus
} from '@shared/models/alarm.models';

@Injectable({
  providedIn: 'root'
})
export class AlarmService {

  constructor(
    private http: HttpClient
  ) { }

  public getAlarm(alarmId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Alarm> {
    return this.http.get<Alarm>(`/api/alarm/${alarmId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getAlarmInfo(alarmId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<AlarmInfo> {
    return this.http.get<AlarmInfo>(`/api/alarm/info/${alarmId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveAlarm(alarm: Alarm, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Alarm> {
    return this.http.post<Alarm>('/api/alarm', alarm, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public ackAlarm(alarmId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<void> {
    return this.http.post<void>(`/api/alarm/${alarmId}/ack`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public clearAlarm(alarmId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<void> {
    return this.http.post<void>(`/api/alarm/${alarmId}/clear`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getAlarms(query: AlarmQuery,
                   ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<PageData<AlarmInfo>> {
    return this.http.get<PageData<AlarmInfo>>(`/api/alarm${query.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getHighestAlarmSeverity(entityId: EntityId, alarmSearchStatus: AlarmSearchStatus, alarmStatus: AlarmStatus,
                                 ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<AlarmSeverity> {
    let url = `/api/alarm/highestSeverity/${entityId.entityType}/${entityId.entityType}`;
    if (alarmSearchStatus) {
      url += `?searchStatus=${alarmSearchStatus}`;
    } else if (alarmStatus) {
      url += `?status=${alarmStatus}`;
    }
    return this.http.get<AlarmSeverity>(url,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }
}

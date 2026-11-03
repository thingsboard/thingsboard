// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AlarmComment, AlarmCommentInfo } from '@shared/models/alarm.models';

@Injectable({
  providedIn: 'root'
})
export class AlarmCommentService {

  constructor(
    private http: HttpClient
  ) { }

  public saveAlarmComment(alarmId: string, alarmComment: AlarmComment, config?: RequestConfig): Observable<AlarmComment> {
    return this.http.post<AlarmComment>(`/api/alarm/${alarmId}/comment`, alarmComment, defaultHttpOptionsFromConfig(config));
  }

  public getAlarmComments(alarmId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<AlarmCommentInfo>> {
    return this.http.get<PageData<AlarmComment>>(`/api/alarm/${alarmId}/comment${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteAlarmComments(alarmId: string, commentId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/alarm/${alarmId}/comment/${commentId}`, defaultHttpOptionsFromConfig(config));
  }

}

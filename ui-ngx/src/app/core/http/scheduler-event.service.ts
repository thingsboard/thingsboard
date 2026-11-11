// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import {
  SchedulerEvent,
  SchedulerEventInfo,
  SchedulerEventWithCustomerInfo
} from '@shared/models/scheduler-event.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  ScheduledReportInfo,
  ReportQuery
} from '@shared/models/report.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class SchedulerEventService {

  constructor(
    private http: HttpClient,
  ) {
  }

  public getSchedulerEvents(type: string = '', config?: RequestConfig): Observable<Array<SchedulerEventWithCustomerInfo>> {
    let url = '/api/schedulerEvents';
    if (isDefinedAndNotNull(type) && type !== '') {
      url += `?type=${type}`;
    }
    return this.http.get<Array<SchedulerEventWithCustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getSchedulerEventsByPageLink(type: string, pageLink: PageLink, edgeId?: string, config?: RequestConfig): Observable<PageData<SchedulerEventWithCustomerInfo>> {
    return this.http.get<PageData<SchedulerEventWithCustomerInfo>>(`/api/schedulerEvents${pageLink.toQuery()}${type ? `&type=${type}` : ''}${edgeId ? `&edgeId=${edgeId}` : ''}`, defaultHttpOptionsFromConfig(config));
  }

  public getCalendarSchedulerEvents(type: string, startTime: number, endTime: number, textSearch: string, edgeId?: string, config?: RequestConfig): Observable<Array<SchedulerEventWithCustomerInfo>> {
    return this.http.get<Array<SchedulerEventWithCustomerInfo>>(`/api/schedulerEvents?startTime=${startTime}&endTime=${endTime}${type ? `&type=${type}` : ''}${textSearch ? `&textSearch=${textSearch}` : ''}${edgeId ? `&edgeId=${edgeId}` : ''}`, defaultHttpOptionsFromConfig(config));
  }

  public getSchedulerEventsByIds(schedulerEventIds: Array<string>, config?: RequestConfig): Observable<Array<SchedulerEventInfo>> {
    return this.http.get<Array<SchedulerEventInfo>>(`/api/schedulerEvents?schedulerEventIds=${schedulerEventIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((schedulerEvents) => sortEntitiesByIds(schedulerEvents, schedulerEventIds))
    );
  }

  public getSchedulerEventInfo(schedulerEventId: string, config?: RequestConfig): Observable<SchedulerEventWithCustomerInfo> {
    return this.http.get<SchedulerEventWithCustomerInfo>(`/api/schedulerEvent/info/${schedulerEventId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getSchedulerEvent(schedulerEventId: string, config?: RequestConfig): Observable<SchedulerEvent> {
    return this.http.get<SchedulerEvent>(`/api/schedulerEvent/${schedulerEventId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveSchedulerEvent(schedulerEvent: SchedulerEvent, config?: RequestConfig): Observable<SchedulerEvent> {
    return this.http.post<SchedulerEvent>('/api/schedulerEvent', schedulerEvent, defaultHttpOptionsFromConfig(config));
  }

  public deleteSchedulerEvent(schedulerEventId: string, config?: RequestConfig) {
    return this.http.delete(`/api/schedulerEvent/${schedulerEventId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeSchedulerEvents(edgeId: string, config?: RequestConfig): Observable<Array<SchedulerEventWithCustomerInfo>> {
    return this.http.get<Array<SchedulerEventWithCustomerInfo>>(`/api/edge/${edgeId}/allSchedulerEvents`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignSchedulerEventToEdge(edgeId: string, schedulerEventId: string, config?: RequestConfig): Observable<SchedulerEventInfo> {
    return this.http.post<SchedulerEventInfo>(`/api/edge/${edgeId}/schedulerEvent/${schedulerEventId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignSchedulerEventFromEdge(edgeId: string, schedulerEventId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}/schedulerEvent/${schedulerEventId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public updateSchedulerStatus(schedulerEventId: string, enabled: boolean, config?: RequestConfig) {
    return this.http.put(`/api/schedulerEvent/${schedulerEventId}/enabled/${enabled}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getScheduledReports(query: ReportQuery, config?: RequestConfig): Observable<PageData<ScheduledReportInfo>> {
    return this.http.get<PageData<ScheduledReportInfo>>(`/api/scheduledReports${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import {
  ReportTemplate,
  ReportTemplateConfig,
  ReportTemplateInfo, ReportTemplateQuery,
  ReportTemplateType
} from '@shared/models/report.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class ReportTemplateService {

  constructor(
    private http: HttpClient,
  ) {
  }

  public getReportTemplate<Config extends ReportTemplateConfig>(reportTemplateId: string, config?: RequestConfig): Observable<ReportTemplate<Config>> {
    return this.http.get<ReportTemplate<Config>>(`/api/reportTemplate/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public getReportTemplateInfo(reportTemplateId: string, config?: RequestConfig): Observable<ReportTemplateInfo> {
    return this.http.get<ReportTemplateInfo>(`/api/reportTemplate/info/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveReportTemplate<Config extends ReportTemplateConfig>(reportTemplate: ReportTemplate<Config>, config?: RequestConfig): Observable<ReportTemplate<Config>> {
    return this.http.post<ReportTemplate<Config>>('/api/reportTemplate', reportTemplate, defaultHttpOptionsFromConfig(config));
  }

  public deleteReportTemplate(reportTemplateId: string, config?: RequestConfig) {
    return this.http.delete(`/api/reportTemplate/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAllReportTemplateInfos(query: ReportTemplateQuery, config?: RequestConfig): Observable<PageData<ReportTemplateInfo>> {
    return this.http.get<PageData<ReportTemplateInfo>>(`/api/reportTemplateInfos/all${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getReportTemplatesByIds(reportTemplateIds: string[], config?: RequestConfig): Observable<Array<ReportTemplateInfo>> {
    return this.http.get<Array<ReportTemplateInfo>>(`/api/reportTemplates?reportTemplateIds=${reportTemplateIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((reportTemplates) => sortEntitiesByIds(reportTemplates, reportTemplateIds))
    );
  }

}

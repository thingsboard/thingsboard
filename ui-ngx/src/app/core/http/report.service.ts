// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Report, ReportInfo, ReportQuery, ReportRequest } from '@shared/models/report.models';
import { Job } from '@shared/models/job.models';
import { map } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';

import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { PageData } from '@shared/models/page/page-data';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { baseUrl, getFilenameFromHttpHeader } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private http: HttpClient,
  ) {
  }

  public getReport(reportId: string, config?: RequestConfig): Observable<Report> {
    return this.http.get<Report>(`/api/v2/report/${reportId}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteReport(reportId: string, config?: RequestConfig) {
    return this.http.delete(`/api/v2/report/${reportId}`, defaultHttpOptionsFromConfig(config));
  }

  public getReports(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Report>> {
    return this.http.get<PageData<Report>>(`/api/v2/reports${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getReportInfos(query: ReportQuery, config?: RequestConfig): Observable<PageData<ReportInfo>> {
    return this.http.get<PageData<ReportInfo>>(`/api/v2/reportInfos/all${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public downloadReport(reportId: string): Observable<any> {
    const url = `/api/v2/report/${reportId}/download`;
    const response = this.http.get(url, {
      responseType: 'arraybuffer',
      observe: 'response'
    });
    return this.processDownloadReportResponse(response);
  }

  public requestReport(reportRequest: ReportRequest, config?: RequestConfig): Observable<Job> {
    return this.http.post<Job>('/api/v2/report/request', reportRequest, defaultHttpOptionsFromConfig(config));
  }

  public downloadTestReport(reportRequest: ReportRequest, downloadElseOpen = true): Observable<any> {
    const url = '/api/v2/report/test';
    const response = this.http.post(url, reportRequest, {
      responseType: 'arraybuffer',
      observe: 'response'
    });
    return this.processDownloadReportResponse(response, downloadElseOpen);
  }

  public updateReportPublicStatus(reportId: string, isPublic: boolean, config?: RequestConfig): Observable<Report> {
    return this.http.put<Report>(`/api/v2/report/${reportId}/public/${isPublic}`, null, defaultHttpOptionsFromConfig(config));
  }

  public getPublicReportDownloadUrl(publicKey: string): string {
    return `${baseUrl()}/api/v2/report/public/${publicKey}/download`;
  }

  public getReportsInfosByIds(reportIds: string[], config?: RequestConfig): Observable<Array<ReportInfo>> {
    return this.http.get<Array<ReportInfo>>(`/api/v2/reportInfos?reportIds=${reportIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((reportTemplates) => sortEntitiesByIds(reportTemplates, reportIds))
    );
  }

  private processDownloadReportResponse(response: Observable<HttpResponse<ArrayBuffer>>, downloadElseOpen = true): Observable<any> {

    return response.pipe(
      map((response) => {
        const headers = response.headers;
        const contentType = headers.get('content-type');
        const blob = new Blob([response.body], { type: contentType });
        const href = URL.createObjectURL(blob);
        if (downloadElseOpen) {
          const filename = getFilenameFromHttpHeader(headers);
          const linkElement = this.document.createElement('a');
          linkElement.setAttribute('href', href);
          linkElement.setAttribute('download', filename);
          linkElement.click();
          setTimeout(() => URL.revokeObjectURL(href), 0);
        } else {
          this.window.open(href, '_blank');
        }
        return null;
      })
    );
  }

}

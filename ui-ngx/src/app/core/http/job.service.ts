// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Job, JobQuery } from '@shared/models/job.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class JobService {

  constructor(private http: HttpClient) {
  }

  getJobById(id: string, config?: RequestConfig): Observable<Job> {
    return this.http.get<Job>(`/api/job/${id}`, defaultHttpOptionsFromConfig(config));
  }

  getJobs(query: JobQuery, config?: RequestConfig): Observable<PageData<Job>> {
    return this.http.get<PageData<Job>>(`/api/jobs${query.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  cancelJob(id: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/job/${id}/cancel`, null, defaultHttpOptionsFromConfig(config));
  }

  reprocessJob(id: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/job/${id}/reprocess`, null, defaultHttpOptionsFromConfig(config));
  }

  deleteJob(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/job/${id}`, defaultHttpOptionsFromConfig(config));
  }
}

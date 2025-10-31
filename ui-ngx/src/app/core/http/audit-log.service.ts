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
import { TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AuditLog, AuditLogQuery } from '@shared/models/audit-log.models';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {

  constructor(
    private http: HttpClient
  ) { }

  public getAuditLogs(auditLogQuery: AuditLogQuery,
                      config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs${auditLogQuery.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAuditLogsByCustomerId(auditLogQuery: AuditLogQuery,
                                  config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs/customer/${auditLogQuery.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAuditLogsByUserId(auditLogQuery: AuditLogQuery,
                              config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs/user/${auditLogQuery.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAuditLogsByEntityId(auditLogQuery: AuditLogQuery,
                                config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs/entity/${auditLogQuery.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

}

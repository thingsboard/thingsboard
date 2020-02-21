///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { AuditLog } from '@shared/models/audit-log.models';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {

  constructor(
    private http: HttpClient
  ) { }

  public getAuditLogs(pageLink: TimePageLink,
                      config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAuditLogsByCustomerId(customerId: string, pageLink: TimePageLink,
                                  config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs/customer/${customerId}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAuditLogsByUserId(userId: string, pageLink: TimePageLink,
                              config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs/user/${userId}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAuditLogsByEntityId(entityId: EntityId, pageLink: TimePageLink,
                                config?: RequestConfig): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(`/api/audit/logs/entity/${entityId.entityType}/${entityId.id}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

}

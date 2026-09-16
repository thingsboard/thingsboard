// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { createDefaultHttpOptions, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AuditLog, AuditLogFilter } from '@shared/models/audit-log.models';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {

  constructor(
    private http: HttpClient
  ) { }

  public getAuditLogs(pageLink: TimePageLink, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogs(pageLink: TimePageLink, filters: AuditLogFilter, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogs(
    pageLink: TimePageLink,
    filtersOrConfig?: AuditLogFilter | RequestConfig,
    config?: RequestConfig
  ): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(
      `/api/audit/logs${pageLink.toQuery()}`,
      createDefaultHttpOptions(filtersOrConfig, config)
    );
  }

  public getAuditLogsByCustomerId(customerId: string, pageLink: TimePageLink, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogsByCustomerId(customerId: string, pageLink: TimePageLink, filters: AuditLogFilter, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogsByCustomerId(
    customerId: string,
    pageLink: TimePageLink,
    filtersOrConfig?: AuditLogFilter | RequestConfig,
    config?: RequestConfig
  ): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(
      `/api/audit/logs/customer/${customerId}${pageLink.toQuery()}`,
      createDefaultHttpOptions(filtersOrConfig, config)
    );
  }

  public getAuditLogsByUserId(userId: string, pageLink: TimePageLink, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogsByUserId(userId: string, pageLink: TimePageLink, filters: AuditLogFilter, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogsByUserId(
    userId: string,
    pageLink: TimePageLink,
    filtersOrConfig?: AuditLogFilter | RequestConfig,
    config?: RequestConfig
  ): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(
      `/api/audit/logs/user/${userId}${pageLink.toQuery()}`,
      createDefaultHttpOptions(filtersOrConfig, config)
    );
  }

  public getAuditLogsByEntityId(entityId: EntityId, pageLink: TimePageLink, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogsByEntityId(entityId: EntityId, pageLink: TimePageLink, filters: AuditLogFilter, config?: RequestConfig): Observable<PageData<AuditLog>>;
  public getAuditLogsByEntityId(
    entityId: EntityId,
    pageLink: TimePageLink,
    filtersOrConfig?: AuditLogFilter | RequestConfig,
    config?: RequestConfig
  ): Observable<PageData<AuditLog>> {
    return this.http.get<PageData<AuditLog>>(
      `/api/audit/logs/entity/${entityId.entityType}/${entityId.id}${pageLink.toQuery()}`,
      createDefaultHttpOptions(filtersOrConfig, config)
    );
  }
}

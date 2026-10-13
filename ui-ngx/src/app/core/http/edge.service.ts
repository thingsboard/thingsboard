// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink, TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { Edge, EdgeEvent, EdgeInfo, EdgeInstructions, EdgeSearchQuery } from '@shared/models/edge.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BulkImportRequest, BulkImportResult } from '@shared/import-export/import-export.models';

@Injectable({
  providedIn: 'root'
})
export class EdgeService {

  constructor(
    private http: HttpClient
  ) { }

  public getEdges(edgeIds: Array<string>, config?: RequestConfig): Observable<Array<Edge>> {
    return this.http.get<Array<Edge>>(`/api/edges?edgeIds=${edgeIds.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdge(edgeId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.get<Edge>(`/api/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeInfo(edgeId: string, config?: RequestConfig): Observable<EdgeInfo> {
    return this.http.get<EdgeInfo>(`/api/edge/info/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveEdge(edge: Edge, entityGroupIds?: string | string[], config?: RequestConfig): Observable<Edge> {
    let url = '/api/edge';
    if (entityGroupIds) {
      if (Array.isArray(entityGroupIds)) {
        url += `?entityGroupIds=${entityGroupIds.join(',')}`;
      } else {
        url += `?entityGroupId=${entityGroupIds}`;
      }
    }
    return this.http.post<Edge>(url, edge, defaultHttpOptionsFromConfig(config));
  }

  public deleteEdge(edgeId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/edge/types', defaultHttpOptionsFromConfig(config));
  }

  /* public getCustomerEdgeInfos(customerId: string, pageLink: PageLink, type: string = '',
                               config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/customer/${customerId}/edgeInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  } */

  public assignEdgeToCustomer(customerId: string, edgeId: string,
                              config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/customer/${customerId}/edge/${edgeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEdgeFromCustomer(edgeId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/edge/${edgeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public makeEdgePublic(edgeId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/customer/public/edge/${edgeId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantEdges(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/tenant/edges${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantEdgeInfos(pageLink: PageLink, type: string = '',
                            config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/tenant/edgeInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getUserEdges(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/user/edges${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllEdgeInfos(includeCustomers: boolean,
                         pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<EdgeInfo>> {
    let url = `/api/edgeInfos/all${pageLink.toQuery()}&type=${type}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<EdgeInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEdgeInfos(includeCustomers: boolean, customerId: string,
                              pageLink: PageLink, type: string = '',
                              config?: RequestConfig): Observable<PageData<EdgeInfo>> {
    let url = `/api/customer/${customerId}/edgeInfos${pageLink.toQuery()}&type=${type}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<EdgeInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: EdgeSearchQuery, config?: RequestConfig): Observable<Array<Edge>> {
    return this.http.post<Array<Edge>>('/api/edges', query,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdgeEvents(entityId: EntityId, pageLink: TimePageLink,
                       config?: RequestConfig): Observable<PageData<EdgeEvent>> {
    return this.http.get<PageData<EdgeEvent>>(`/api/edge/${entityId.id}/events` + `${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public syncEdge(edgeId: string, config?: RequestConfig) {
    return this.http.post(`/api/edge/sync/${edgeId}`, edgeId, defaultHttpOptionsFromConfig(config));
  }

  public findMissingToRelatedRuleChains(edgeId: string, config?: RequestConfig): Observable<string> {
    return this.http.get<string>(`/api/edge/missingToRelatedRuleChains/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public findByName(edgeName: string, config?: RequestConfig): Observable<Edge> {
    return this.http.get<Edge>(`/api/tenant/edges?edgeName=${edgeName}`, defaultHttpOptionsFromConfig(config));
  }

  public bulkImportEdges(entitiesData: BulkImportRequest, config?: RequestConfig): Observable<BulkImportResult> {
    return this.http.post<BulkImportResult>('/api/edge/bulk_import', entitiesData, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeInstallInstructions(edgeId: string, method: string = 'ubuntu', config?: RequestConfig): Observable<EdgeInstructions> {
    return this.http.get<EdgeInstructions>(`/api/edge/instructions/install/${edgeId}/${method}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeUpgradeInstructions(edgeVersion: string, method: string = 'ubuntu', config?: RequestConfig): Observable<EdgeInstructions> {
    return this.http.get<EdgeInstructions>(`/api/edge/instructions/upgrade/${edgeVersion}/${method}`, defaultHttpOptionsFromConfig(config));
  }

  public findAllRelatedEdgesMissingAttributes(integrationId: string, config?: RequestConfig): Observable<string> {
    const url = `/api/edge/integration/${integrationId}/allMissingAttributes`;
    return this.http.get<string>(url, defaultHttpOptionsFromConfig(config));
  }

  public findEdgeMissingAttributes(integrationIds: Array<string>, edgeId: string, config?: RequestConfig): Observable<string> {
    const url = `/api/edge/integration/${edgeId}/missingAttributes?integrationIds=${integrationIds.join(',')}`;
    return this.http.get<string>(url, defaultHttpOptionsFromConfig(config));
  }

  public isEdgeUpgradeAvailable(edgeId: string, config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>(`/api/edge/${edgeId}/upgrade/available`, defaultHttpOptionsFromConfig(config));
  }
}

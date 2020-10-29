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
import { PageLink, TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { Edge, EdgeInfo, EdgeSearchQuery } from "@shared/models/edge.models";
import { EntityId } from "@shared/models/id/entity-id";
import { Event } from "@shared/models/event.models";
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

  public getEdge(edgeId: string, config?: RequestConfig): Observable<EdgeInfo> {
    return this.http.get<EdgeInfo>(`/api/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveEdge(edge: Edge, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>('/api/edge', edge, defaultHttpOptionsFromConfig(config));
  }

  public deleteEdge(edgeId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/edge/types', defaultHttpOptionsFromConfig(config));
  }

  public getTenantEdges(pageLink: PageLink, type: string = '',
                        config?: RequestConfig): Observable<PageData<EdgeInfo>> {
    return this.http.get<PageData<EdgeInfo>>(`/api/tenant/edges${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEdges(customerId: string, pageLink: PageLink, type: string = '',
                          config?: RequestConfig): Observable<PageData<EdgeInfo>> {
    return this.http.get<PageData<EdgeInfo>>(`/api/customer/${customerId}/edges${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEdgeInfos(customerId: string, pageLink: PageLink, type: string = '',
                               config?: RequestConfig): Observable<PageData<EdgeInfo>> {
    return this.http.get<PageData<EdgeInfo>>(`/api/customer/${customerId}/edgeInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignEdgeToCustomer(customerId: string, edgeId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/customer/${customerId}/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public unassignEdgeFromCustomer(edgeId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public makeEdgePublic(edgeId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/customer/public/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public setRootRuleChain(edgeId: string, ruleChainId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/edge/${edgeId}/${ruleChainId}/root`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantEdgeInfos(pageLink: PageLink, type: string = '',
                            config?: RequestConfig): Observable<PageData<EdgeInfo>> {
    return this.http.get<PageData<EdgeInfo>>(`/api/tenant/edgeInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: EdgeSearchQuery, config?: RequestConfig): Observable<Array<Edge>> {
    return this.http.post<Array<Edge>>('/api/edges', query, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeEvents(entityId: EntityId, pageLink: TimePageLink, config?: RequestConfig): Observable<PageData<Event>> {
    return this.http.get<PageData<Event>>(`/api/edge/${entityId.id}/events` + `${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }
}

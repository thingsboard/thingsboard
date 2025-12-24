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
import { createDefaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { EntityView, EntityViewInfo, EntityViewSearchQuery } from '@app/shared/models/entity-view.models';
import { SaveEntityParams } from '@shared/models/entity.models';

@Injectable({
  providedIn: 'root'
})
export class EntityViewService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantEntityViewInfos(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/tenant/entityViewInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEntityViewInfos(customerId: string, pageLink: PageLink, type: string = '',
                                    config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/customer/${customerId}/entityViewInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityView(entityViewId: string, config?: RequestConfig): Observable<EntityView> {
    return this.http.get<EntityView>(`/api/entityView/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityViews(entityViewIds: Array<string>, config?: RequestConfig): Observable<Array<EntityView>> {
    return this.http.get<Array<EntityView>>(`/api/entityViews?entityViewIds=${entityViewIds.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityViewInfo(entityViewId: string, config?: RequestConfig): Observable<EntityViewInfo> {
    return this.http.get<EntityViewInfo>(`/api/entityView/info/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveEntityView(entityView: EntityView, config?: RequestConfig): Observable<EntityView>;
  public saveEntityView(entityView: EntityView, saveParams: SaveEntityParams, config?: RequestConfig): Observable<EntityView>;
  public saveEntityView(entityView: EntityView, saveParamsOrConfig?: SaveEntityParams | RequestConfig, config?: RequestConfig): Observable<EntityView> {
    return this.http.post<EntityView>('/api/entityView', entityView,  createDefaultHttpOptions(saveParamsOrConfig, config));
  }

  public deleteEntityView(entityViewId: string, config?: RequestConfig) {
    return this.http.delete(`/api/entityView/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityViewTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/entityView/types', defaultHttpOptionsFromConfig(config));
  }

  public makeEntityViewPublic(entityViewId: string, config?: RequestConfig): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/customer/public/entityView/${entityViewId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public assignEntityViewToCustomer(customerId: string, entityViewId: string,
                                    config?: RequestConfig): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/customer/${customerId}/entityView/${entityViewId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEntityViewFromCustomer(entityViewId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/entityView/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: EntityViewSearchQuery,
                     config?: RequestConfig): Observable<Array<EntityView>> {
    return this.http.post<Array<EntityView>>('/api/entityViews', query, defaultHttpOptionsFromConfig(config));
  }

  public assignEntityViewToEdge(edgeId: string, entityViewId: string, config?: RequestConfig): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/edge/${edgeId}/entityView/${entityViewId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEntityViewFromEdge(edgeId: string, entityViewId: string,
                                    config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}/entityView/${entityViewId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdgeEntityViews(edgeId: string, pageLink: PageLink, type: string = '',
                            config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/edge/${edgeId}/entityViews${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config))
  }

}

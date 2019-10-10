///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {Injectable} from '@angular/core';
import {defaultHttpOptions} from './http-utils';
import {Observable} from 'rxjs/index';
import {HttpClient} from '@angular/common/http';
import {PageLink} from '@shared/models/page/page-link';
import {PageData} from '@shared/models/page/page-data';
import {EntitySubtype} from '@app/shared/models/entity-type.models';
import { EntityView, EntityViewInfo, EntityViewSearchQuery } from '@app/shared/models/entity-view.models';
import { Asset, AssetSearchQuery } from '@shared/models/asset.models';

@Injectable({
  providedIn: 'root'
})
export class EntityViewService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantEntityViewInfos(pageLink: PageLink, type: string = '', ignoreErrors: boolean = false,
                                  ignoreLoading: boolean = false): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/tenant/entityViewInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getCustomerEntityViewInfos(customerId: string, pageLink: PageLink, type: string = '', ignoreErrors: boolean = false,
                                    ignoreLoading: boolean = false): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/customer/${customerId}/entityViewInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getEntityView(entityViewId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<EntityView> {
    return this.http.get<EntityView>(`/api/entityView/${entityViewId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getEntityViewInfo(entityViewId: string, ignoreErrors: boolean = false,
                           ignoreLoading: boolean = false): Observable<EntityViewInfo> {
    return this.http.get<EntityViewInfo>(`/api/entityView/info/${entityViewId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveEntityView(entityView: EntityView, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<EntityView> {
    return this.http.post<EntityView>('/api/entityView', entityView, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteEntityView(entityViewId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/entityView/${entityViewId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getEntityViewTypes(ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/entityView/types', defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public makeEntityViewPublic(entityViewId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/customer/public/entityView/${entityViewId}`, null,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public assignEntityViewToCustomer(customerId: string, entityViewId: string,
                                    ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/customer/${customerId}/entityView/${entityViewId}`, null,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public unassignEntityViewFromCustomer(entityViewId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/customer/entityView/${entityViewId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findByQuery(query: EntityViewSearchQuery,
                     ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityView>> {
    return this.http.post<Array<EntityView>>('/api/entityViews', query, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}

///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Resource, ResourceInfo, ResourceSubType, ResourceType, TBResourceScope } from '@shared/models/resource.models';
import { catchError, mergeMap } from 'rxjs/operators';
import { isNotEmptyStr } from '@core/utils';
import { ResourcesService } from '@core/services/resources.service';

@Injectable({
  providedIn: 'root'
})
export class ResourceService {
  constructor(
    private http: HttpClient,
    private resourcesService: ResourcesService
  ) {

  }

  public getResources(pageLink: PageLink, resourceType?: ResourceType, resourceSubType?: ResourceSubType, config?: RequestConfig): Observable<PageData<ResourceInfo>> {
    let url = `/api/resource${pageLink.toQuery()}`;
    if (isNotEmptyStr(resourceType)) {
      url += `&resourceType=${resourceType}`;
    }
    if (isNotEmptyStr(resourceSubType)) {
      url += `&resourceSubType=${resourceSubType}`;
    }
    return this.http.get<PageData<ResourceInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getTenantResources(pageLink: PageLink, config?: RequestConfig): Observable<PageData<ResourceInfo>> {
    return this.http.get<PageData<ResourceInfo>>(`/api/resource/tenant${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config))
  }

  public getResource(resourceId: string, config?: RequestConfig): Observable<Resource> {
    return this.http.get<Resource>(`/api/resource/${resourceId}`, defaultHttpOptionsFromConfig(config));
  }

  public getResourceInfoById(resourceId: string, config?: RequestConfig): Observable<ResourceInfo> {
    return this.http.get<Resource>(`/api/resource/info/${resourceId}`, defaultHttpOptionsFromConfig(config));
  }

  public getResourceInfo(type: ResourceType, scope: TBResourceScope, key: string, config?: RequestConfig): Observable<ResourceInfo> {
    return this.http.get<Resource>(`/api/resource/${type}/${scope}/${key}/info`, defaultHttpOptionsFromConfig(config));
  }

  public downloadResource(resourceId: string): Observable<any> {
    return this.resourcesService.downloadResource(`/api/resource/${resourceId}/download`);
  }

  public saveResources(resources: Resource[], config?: RequestConfig): Observable<Resource[]> {
    let partSize = 100;
    partSize = resources.length > partSize ? partSize : resources.length;
    const resourceObservables: Observable<Resource>[] = [];
    for (let i = 0; i < partSize; i++) {
      resourceObservables.push(this.saveResource(resources[i], config).pipe(catchError(() => of({} as Resource))));
    }
    return forkJoin(resourceObservables).pipe(
      mergeMap((resource) => {
        resources.splice(0, partSize);
        if (resources.length) {
          return this.saveResources(resources, config);
        } else {
          return of(resource);
        }
      })
    );
  }

  public saveResource(resource: Resource, config?: RequestConfig): Observable<Resource> {
    return this.http.post<Resource>('/api/resource', resource, defaultHttpOptionsFromConfig(config));
  }

  public deleteResource(resourceId: string, force = false, config?: RequestConfig) {
    return this.http.delete(`/api/resource/${resourceId}?force=${force}`, defaultHttpOptionsFromConfig(config));
  }

  public getResourcesByIds(ids: string[], config?: RequestConfig): Observable<Array<ResourceInfo>> {
    return this.http.get<Array<ResourceInfo>>(`/api/resource?resourceIds=${ids.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

}

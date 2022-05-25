///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { BranchInfo, EntityVersion, VersionCreateRequest, VersionCreationResult } from '@shared/models/vc.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable({
  providedIn: 'root'
})
export class EntitiesVersionControlService {

  constructor(
    private http: HttpClient
  ) {
  }

  public listBranches(config?: RequestConfig): Observable<Array<BranchInfo>> {
    return this.http.get<Array<BranchInfo>>('/api/entities/vc/branches', defaultHttpOptionsFromConfig(config));
  }

  public saveEntitiesVersion(request: VersionCreateRequest, config?: RequestConfig): Observable<VersionCreationResult> {
    return this.http.post<VersionCreationResult>('/api/entities/vc/version', request, defaultHttpOptionsFromConfig(config));
  }

  public listEntityVersions(pageLink: PageLink, branch: string,
                            externalEntityId: EntityId,
                            config?: RequestConfig): Observable<PageData<EntityVersion>> {
    return this.http.get<PageData<EntityVersion>>(`/api/entities/vc/version/${branch}/${externalEntityId.entityType}/${externalEntityId.id}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public listEntityTypeVersions(pageLink: PageLink, branch: string,
                                entityType: EntityType,
                                config?: RequestConfig): Observable<PageData<EntityVersion>> {
    return this.http.get<PageData<EntityVersion>>(`/api/entities/vc/version/${branch}/${entityType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public listVersions(pageLink: PageLink, branch: string,
                      config?: RequestConfig): Observable<PageData<EntityVersion>> {
    return this.http.get<PageData<EntityVersion>>(`/api/entities/vc/version/${branch}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }
}

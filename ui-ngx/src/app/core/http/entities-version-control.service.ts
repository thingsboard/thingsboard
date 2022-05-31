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
import { Observable, of } from 'rxjs';
import {
  BranchInfo, EntityDataDiff,
  EntityVersion,
  VersionCreateRequest,
  VersionCreationResult,
  VersionLoadRequest, VersionLoadResult
} from '@shared/models/vc.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectIsUserLoaded } from '@core/auth/auth.selectors';
import { catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class EntitiesVersionControlService {

  branchList: Array<BranchInfo> = null;

  constructor(
    private http: HttpClient,
    private store: Store<AppState>
  ) {

    this.store.pipe(select(selectIsUserLoaded)).subscribe(
      () => {
        this.branchList = null;
      }
    );
  }

  public clearBranchList(): void {
    this.branchList = null;
  }

  public listBranches(): Observable<Array<BranchInfo>> {
    if (!this.branchList) {
      return this.http.get<Array<BranchInfo>>('/api/entities/vc/branches',
        defaultHttpOptionsFromConfig({ignoreErrors: true, ignoreLoading: false})).pipe(
        catchError(() => of([] as Array<BranchInfo>)),
        tap((list) => {
          this.branchList = list;
        })
      );
    } else {
      return of(this.branchList);
    }
  }

  public saveEntitiesVersion(request: VersionCreateRequest, config?: RequestConfig): Observable<VersionCreationResult> {
    return this.http.post<VersionCreationResult>('/api/entities/vc/version', request, defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        const branch = request.branch;
        if (this.branchList && !this.branchList.find(b => b.name === branch)) {
          this.branchList = null;
        }
      })
    );
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

  public loadEntitiesVersion(request: VersionLoadRequest, config?: RequestConfig): Observable<Array<VersionLoadResult>> {
    return this.http.post<Array<VersionLoadResult>>('/api/entities/vc/entity', request, defaultHttpOptionsFromConfig(config));
  }

  public compareEntityDataToVersion(branch: string,
                                    entityId: EntityId,
                                    versionId: string,
                                    config?: RequestConfig): Observable<EntityDataDiff> {
    return this.http.get<EntityDataDiff>(`/api/entities/vc/diff/${branch}/${entityId.entityType}/${entityId.id}?versionId=${versionId}`,
      defaultHttpOptionsFromConfig(config));
  }
}

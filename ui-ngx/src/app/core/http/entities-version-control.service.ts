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
  BranchInfo, EntityDataDiff, EntityDataInfo, EntityLoadError, entityLoadErrorTranslationMap, EntityLoadErrorType,
  EntityVersion,
  VersionCreateRequest,
  VersionCreationResult,
  VersionLoadRequest, VersionLoadResult
} from '@shared/models/vc.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectIsUserLoaded } from '@core/auth/auth.selectors';
import { catchError, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Injectable({
  providedIn: 'root'
})
export class EntitiesVersionControlService {

  branchList: Array<BranchInfo> = null;

  constructor(
    private http: HttpClient,
    private translate: TranslateService,
    private sanitizer: DomSanitizer,
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

  public getEntityDataInfo(externalEntityId: EntityId,
                           versionId: string,
                           config?: RequestConfig): Observable<EntityDataInfo> {
    return this.http.get<EntityDataInfo>(`/api/entities/vc/info/${versionId}/${externalEntityId.entityType}/${externalEntityId.id}`,
      defaultHttpOptionsFromConfig(config));
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

  public loadEntitiesVersion(request: VersionLoadRequest, config?: RequestConfig): Observable<VersionLoadResult> {
    return this.http.post<VersionLoadResult>('/api/entities/vc/entity', request, defaultHttpOptionsFromConfig(config));
  }

  public compareEntityDataToVersion(branch: string,
                                    entityId: EntityId,
                                    versionId: string,
                                    config?: RequestConfig): Observable<EntityDataDiff> {
    return this.http.get<EntityDataDiff>(`/api/entities/vc/diff/${branch}/${entityId.entityType}/${entityId.id}?versionId=${versionId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public entityLoadErrorToMessage(entityLoadError: EntityLoadError): SafeHtml {
    const type = entityLoadError.type;
    const messageId = entityLoadErrorTranslationMap.get(type);
    const messageArgs = {} as any;
    switch (type) {
      case EntityLoadErrorType.DEVICE_CREDENTIALS_CONFLICT:
        messageArgs.entityId = entityLoadError.source.id;
        break;
      case EntityLoadErrorType.MISSING_REFERENCED_ENTITY:
        messageArgs.sourceEntityTypeName =
          (this.translate.instant(entityTypeTranslations.get(entityLoadError.source.entityType).type) as string).toLowerCase();
        messageArgs.sourceEntityId = entityLoadError.source.id;
        messageArgs.targetEntityTypeName =
          (this.translate.instant(entityTypeTranslations.get(entityLoadError.target.entityType).type) as string).toLowerCase();
        messageArgs.targetEntityId = entityLoadError.target.id;
        break;
    }
    return this.sanitizer.bypassSecurityTrustHtml(this.translate.instant(messageId, messageArgs));
  }
}

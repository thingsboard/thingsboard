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

import { Injectable } from '@angular/core';
import { defaultHttpOptions } from './http-utils';
import { Observable } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { EntityRelation, EntityRelationInfo, EntityRelationsQuery } from '@shared/models/relation.models';
import { EntityId } from '@app/shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class EntityRelationService {

  constructor(
    private http: HttpClient
  ) { }

  public saveRelation(relation: EntityRelation, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<EntityRelation> {
    return this.http.post<EntityRelation>('/api/relation', relation, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteRelation(fromId: EntityId, relationType: string, toId: EntityId,
                        ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/relation?fromId=${fromId.id}&fromType=${fromId.entityType}` +
      `&relationType=${relationType}&toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteRelations(entityId: EntityId,
                         ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/relations?entityId=${entityId.id}&entityType=${entityId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getRelation(fromId: EntityId, relationType: string, toId: EntityId,
                     ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<EntityRelation> {
    return this.http.get<EntityRelation>(`/api/relation?fromId=${fromId.id}&fromType=${fromId.entityType}` +
      `&relationType=${relationType}&toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findByFrom(fromId: EntityId,
                    ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?fromId=${fromId.id}&fromType=${fromId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findInfoByFrom(fromId: EntityId,
                        ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelationInfo>> {
    return this.http.get<Array<EntityRelationInfo>>(
      `/api/relations/info?fromId=${fromId.id}&fromType=${fromId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findByFromAndType(fromId: EntityId, relationType: string,
                           ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?fromId=${fromId.id}&fromType=${fromId.entityType}&relationType=${relationType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findByTo(toId: EntityId,
                  ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findInfoByTo(toId: EntityId,
                      ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelationInfo>> {
    return this.http.get<Array<EntityRelationInfo>>(
      `/api/relations/info?toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findByToAndType(toId: EntityId, relationType: string,
                         ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?toId=${toId.id}&toType=${toId.entityType}&relationType=${relationType}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findByQuery(query: EntityRelationsQuery,
                     ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelation>> {
    return this.http.post<Array<EntityRelation>>(
      '/api/relations', query,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public findInfoByQuery(query: EntityRelationsQuery,
                         ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntityRelationInfo>> {
    return this.http.post<Array<EntityRelationInfo>>(
      '/api/relations/info', query,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}

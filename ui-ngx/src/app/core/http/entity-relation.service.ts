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
import { EntityRelation, EntityRelationInfo, EntityRelationsQuery } from '@shared/models/relation.models';
import { EntityId } from '@app/shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class EntityRelationService {

  constructor(
    private http: HttpClient
  ) { }

  public saveRelation(relation: EntityRelation, config?: RequestConfig): Observable<EntityRelation> {
    return this.http.post<EntityRelation>('/api/relation', relation, defaultHttpOptionsFromConfig(config));
  }

  public deleteRelation(fromId: EntityId, relationType: string, toId: EntityId,
                        config?: RequestConfig) {
    return this.http.delete(`/api/relation?fromId=${fromId.id}&fromType=${fromId.entityType}` +
      `&relationType=${relationType}&toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteRelations(entityId: EntityId,
                         config?: RequestConfig) {
    return this.http.delete(`/api/relations?entityId=${entityId.id}&entityType=${entityId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRelation(fromId: EntityId, relationType: string, toId: EntityId,
                     config?: RequestConfig): Observable<EntityRelation> {
    return this.http.get<EntityRelation>(`/api/relation?fromId=${fromId.id}&fromType=${fromId.entityType}` +
      `&relationType=${relationType}&toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByFrom(fromId: EntityId,
                    config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?fromId=${fromId.id}&fromType=${fromId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findInfoByFrom(fromId: EntityId,
                        config?: RequestConfig): Observable<Array<EntityRelationInfo>> {
    return this.http.get<Array<EntityRelationInfo>>(
      `/api/relations/info?fromId=${fromId.id}&fromType=${fromId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByFromAndType(fromId: EntityId, relationType: string,
                           config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?fromId=${fromId.id}&fromType=${fromId.entityType}&relationType=${relationType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByTo(toId: EntityId,
                  config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findInfoByTo(toId: EntityId,
                      config?: RequestConfig): Observable<Array<EntityRelationInfo>> {
    return this.http.get<Array<EntityRelationInfo>>(
      `/api/relations/info?toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByToAndType(toId: EntityId, relationType: string,
                         config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?toId=${toId.id}&toType=${toId.entityType}&relationType=${relationType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: EntityRelationsQuery,
                     config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.post<Array<EntityRelation>>(
      '/api/relations', query,
      defaultHttpOptionsFromConfig(config));
  }

  public findInfoByQuery(query: EntityRelationsQuery,
                         config?: RequestConfig): Observable<Array<EntityRelationInfo>> {
    return this.http.post<Array<EntityRelationInfo>>(
      '/api/relations/info', query,
      defaultHttpOptionsFromConfig(config));
  }

}

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
import { forkJoin, Observable, of } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';

@Injectable({
  providedIn: 'root'
})
export class AttributeService {

  constructor(
    private http: HttpClient
  ) { }

  public getEntityAttributes(entityId: EntityId, attributeScope: AttributeScope,
                             ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<AttributeData>> {
    return this.http.get<Array<AttributeData>>(`/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/values/attributes/` +
      `${attributeScope}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteEntityAttributes(entityId: EntityId, attributeScope: AttributeScope, attributes: Array<AttributeData>,
                                ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<any> {
    const keys = attributes.map(attribute => attribute.key).join(',');
    return this.http.delete(`/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/${attributeScope}` +
      `?keys=${keys}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveEntityAttributes(entityId: EntityId, attributeScope: AttributeScope, attributes: Array<AttributeData>,
                              ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<any> {
    const attributesData: {[key: string]: any} = {};
    const deleteAttributes: AttributeData[] = [];
    attributes.forEach((attribute) => {
      if (attribute.value !== null) {
        attributesData[attribute.key] = attribute.value;
      } else {
        deleteAttributes.push(attribute);
      }
    });
    let deleteEntityAttributesObservable: Observable<any>;
    if (deleteAttributes.length) {
      deleteEntityAttributesObservable = this.deleteEntityAttributes(entityId, attributeScope, deleteAttributes);
    } else {
      deleteEntityAttributesObservable = of(null);
    }
    let saveEntityAttributesObservable: Observable<any>;
    if (Object.keys(attributesData).length) {
      saveEntityAttributesObservable = this.http.post(`/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/${attributeScope}`,
        attributesData, defaultHttpOptions(ignoreLoading, ignoreErrors));
    } else {
      saveEntityAttributesObservable = of(null);
    }
    return forkJoin(saveEntityAttributesObservable, deleteEntityAttributesObservable);
  }
}

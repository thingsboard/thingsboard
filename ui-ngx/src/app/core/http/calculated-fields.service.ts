// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, defaultHttpOptionsFromParams, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageData } from '@shared/models/page/page-data';
import {
  CalculatedField,
  CalculatedFieldInfo,
  CalculatedFieldsQuery,
  CalculatedFieldTestScriptInputParams,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityTestScriptResult } from '@shared/models/entity.models';
import { CalculatedFieldEventBody } from '@shared/models/event.models';

@Injectable({
  providedIn: 'root'
})
export class CalculatedFieldsService {

  constructor(
    private http: HttpClient
  ) { }

  public getCalculatedFieldById(calculatedFieldId: string, config?: RequestConfig): Observable<CalculatedField> {
    return this.http.get<CalculatedField>(`/api/calculatedField/${calculatedFieldId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveCalculatedField(calculatedField: CalculatedField, config?: RequestConfig): Observable<CalculatedField> {
    return this.http.post<CalculatedField>('/api/calculatedField', calculatedField, defaultHttpOptionsFromConfig(config));
  }

  public deleteCalculatedField(calculatedFieldId: string, config?: RequestConfig): Observable<boolean> {
    return this.http.delete<boolean>(`/api/calculatedField/${calculatedFieldId}`, defaultHttpOptionsFromConfig(config));
  }

  public getCalculatedFields(pageLink: PageLink, query: CalculatedFieldsQuery, config?: RequestConfig): Observable<PageData<CalculatedFieldInfo>> {
    return this.http.get<PageData<CalculatedFieldInfo>>(`/api/calculatedFields${pageLink.toQuery()}`, defaultHttpOptionsFromParams(query, config));
  }

  public getCalculatedFieldsByEntityId({ entityType, id }: EntityId, pageLink: PageLink, type?: CalculatedFieldType, config?: RequestConfig): Observable<PageData<CalculatedField>> {
    return this.http.get<PageData<CalculatedField>>(`/api/${entityType}/${id}/calculatedFields${pageLink.toQuery()}`, defaultHttpOptionsFromParams({type} , config));
  }

  public testScript(inputParams: CalculatedFieldTestScriptInputParams, config?: RequestConfig): Observable<EntityTestScriptResult> {
    return this.http.post<EntityTestScriptResult>('/api/calculatedField/testScript', inputParams, defaultHttpOptionsFromConfig(config));
  }

  public getLatestCalculatedFieldDebugEvent(id: string, config?: RequestConfig): Observable<CalculatedFieldEventBody> {
    return this.http.get<CalculatedFieldEventBody>(`/api/calculatedField/${id}/debug`, defaultHttpOptionsFromConfig(config));
  }

  public getCalculatedFieldNames(pageLink: PageLink, type: CalculatedFieldType, config?: RequestConfig): Observable<PageData<string>> {
    return this.http.get<PageData<string>>(`/api/calculatedFields/names${pageLink.toQuery()}`, defaultHttpOptionsFromParams({type}, config));
  }
}

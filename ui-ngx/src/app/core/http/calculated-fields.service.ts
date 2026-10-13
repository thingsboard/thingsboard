// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, defaultHttpOptionsFromParams, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageData } from '@shared/models/page/page-data';
import {
  CalculatedField,
  CalculatedFieldInfo,
  CalculatedFieldReprocessingValidation,
  CalculatedFieldsQuery,
  CalculatedFieldTestScriptInputParams,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityTestScriptResult } from '@shared/models/entity.models';
import { CalculatedFieldEventBody } from '@shared/models/event.models';
import { Job } from '@app/shared/models/job.models';

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

  public reprocessCalculatedField(id: string, startTs: number, endTs: number, config?: RequestConfig): Observable<void > {
    return this.http.get<void>(`/api/calculatedField/${id}/reprocess?startTs=${startTs}&endTs=${endTs}`, defaultHttpOptionsFromConfig(config));
  }

  public getLastCalculatedFieldReprocessingJob(id: string, config?: RequestConfig): Observable<Job> {
    return this.http.get<Job>(`/api/calculatedField/${id}/reprocess/job`, defaultHttpOptionsFromConfig(config));
  }

  public validateCalculatedFieldReprocessing(id: string, config?: RequestConfig): Observable<CalculatedFieldReprocessingValidation> {
    return this.http.get<CalculatedFieldReprocessingValidation>(`/api/calculatedField/${id}/reprocess/validate`, defaultHttpOptionsFromConfig(config));
  }
}

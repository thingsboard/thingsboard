// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import {
  ConvertedInputMsgParams,
  ConvertedInputMsgResult,
  Converter,
  ConverterDebugInput,
  LatestConverterParameters,
  TestConverterResult,
  TestDownLinkInputParams,
  TestUpLinkInputParams
} from '@shared/models/converter.models';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { IntegrationType } from '@shared/models/integration.models';

@Injectable({
  providedIn: 'root'
})
export class ConverterService {

  constructor(
    private http: HttpClient
  ) { }

  public getConverters(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Converter>> {
    return this.getConvertersByEdgeTemplate(pageLink, false, null, config);
  }

  public getConvertersByEdgeTemplate(pageLink: PageLink, isEdgeTemplate: boolean, integrationType?: IntegrationType,
                                     config?: RequestConfig): Observable<PageData<Converter>> {
    let url = `/api/converters${pageLink.toQuery()}`;
    if (isEdgeTemplate) {
      url += `&isEdgeTemplate=${isEdgeTemplate}`;
    }
    if (integrationType) {
      url += `&integrationType=${integrationType}`;
    }
    return this.http.get<PageData<Converter>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getConvertersByIds(converterIds: Array<string>, config?: RequestConfig): Observable<Array<Converter>> {
    return this.http.get<Array<Converter>>(`/api/converters?converterIds=${converterIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((converters) => sortEntitiesByIds(converters, converterIds))
    );
  }

  public getConverter(converterId: string, config?: RequestConfig): Observable<Converter> {
    return this.http.get<Converter>(`/api/converter/${converterId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveConverter(converter: Converter, config?: RequestConfig): Observable<Converter> {
    return this.http.post<Converter>('/api/converter', converter, defaultHttpOptionsFromConfig(config));
  }

  public deleteConverter(converterId: string, config?: RequestConfig) {
    return this.http.delete(`/api/converter/${converterId}`, defaultHttpOptionsFromConfig(config));
  }

  public testUpLink(inputParams: TestUpLinkInputParams, scriptLang?: ScriptLanguage,
                    config?: RequestConfig): Observable<TestConverterResult> {
    let url = '/api/converter/testUpLink';
    if (scriptLang) {
      url += `?scriptLang=${scriptLang}`;
    }
    return this.http.post<TestConverterResult>(url, inputParams, defaultHttpOptionsFromConfig(config));
  }

  public testDownLink(inputParams: TestDownLinkInputParams, scriptLang?: ScriptLanguage,
                      config?: RequestConfig): Observable<TestConverterResult> {
    let url = '/api/converter/testDownLink';
    if (scriptLang) {
      url += `?scriptLang=${scriptLang}`;
    }
    return this.http.post<TestConverterResult>(url, inputParams, defaultHttpOptionsFromConfig(config));
  }

  public getLatestConverterDebugInput(converterId: string, parameters?: LatestConverterParameters,
                                      config?: RequestConfig): Observable<ConverterDebugInput> {
    let url = `/api/converter/${converterId}/debugIn`;
    if (parameters) {
      let params = new HttpParams();
      if (parameters.converterType) {
        params = params.set('converterType', parameters.converterType)
      }
      if (parameters.integrationType) {
        params = params.set('integrationType', parameters.integrationType)
      }
      if (parameters.integrationName) {
        params = params.set('integrationName', parameters.integrationName)
      }
      if (parameters.converterVersion) {
        params = params.set('converterVersion', parameters.converterVersion)
      }
      if (params.toString()) {
        url += `?${params.toString()}`;
      }
    }
    return this.http.get<ConverterDebugInput>(url, defaultHttpOptionsFromConfig(config));
  }

  public unwrapRawPayload(integrationType: IntegrationType, msg: ConvertedInputMsgParams, config?: RequestConfig): Observable<ConvertedInputMsgResult> {
    return this.http.post<ConvertedInputMsgResult>(`/api/converter/unwrap/${integrationType}`, msg, defaultHttpOptionsFromConfig(config));
  }

}

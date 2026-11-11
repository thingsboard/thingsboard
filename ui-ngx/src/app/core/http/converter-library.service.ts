// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Converter, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class ConverterLibraryService {

  private readonly baseUrl = '/api/converter/library';

  constructor(
    private http: HttpClient
  ) {
  }

  getVendors(integrationType: IntegrationType, converterType: ConverterType, config?: RequestConfig): Observable<Vendor[]> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/vendors?converterType=${converterType.toLowerCase()}`,
      defaultHttpOptionsFromConfig(config)
    ) as Observable<Vendor[]>;
  }

  getModels(
    integrationType: IntegrationType,
    vendorName: string,
    converterType: ConverterType,
    config?: RequestConfig
  ): Observable<Model[]> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${encodeURIComponent(vendorName)}/models?converterType=${converterType.toLowerCase()}`,
      defaultHttpOptionsFromConfig(config)
    ) as Observable<Model[]>;
  }

  getConverter(
    integrationType: IntegrationType,
    vendorName: string, modelName: string,
    converterType: ConverterType,
    config?: RequestConfig
  ): Observable<Converter> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${encodeURIComponent(vendorName)}/${encodeURIComponent(modelName)}/${converterType.toLowerCase()}`,
      defaultHttpOptionsFromConfig(config)
    ) as Observable<Converter>;
  }

  getConverterMetaData(integrationType: IntegrationType, vendorName: string, modelName: string, converterType: ConverterType, config?: RequestConfig): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${encodeURIComponent(vendorName)}/${encodeURIComponent(modelName)}/${converterType.toLowerCase()}/metadata`,
      {...{responseType: 'text'}, ...defaultHttpOptionsFromConfig(config)}
    );
  }

  getConverterPayload(integrationType: IntegrationType, vendorName: string, modelName: string, converterType: ConverterType, config?: RequestConfig): Observable<string> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${encodeURIComponent(vendorName)}/${encodeURIComponent(modelName)}/${converterType.toLowerCase()}/payload`,
      {...{responseType: 'text'}, ...defaultHttpOptionsFromConfig(config)}
    );
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomTranslationEditData, TranslationInfo } from '@shared/models/custom-translation.model';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { ResourcesService } from '@core/services/resources.service';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class CustomTranslationService {

  constructor(
    private http: HttpClient,
    private resourcesService: ResourcesService
  ) {}

  public getAvailableLocales(config?: RequestConfig): Observable<{[k: string]: string}> {
    return this.http.get<{[k: string]: string}>('/api/translation/availableLocales', defaultHttpOptionsFromConfig(config))
  }

  public getAvailableJavaLocales(config?: RequestConfig): Observable<{[k: string]: string}> {
    return this.http.get<{[k: string]: string}>('/api/translation/availableJavaLocales', defaultHttpOptionsFromConfig(config))
  }

  public getTranslationInfos(config?: RequestConfig): Observable<Array<TranslationInfo>> {
    return this.http.get<Array<TranslationInfo>>('/api/translation/info', defaultHttpOptionsFromConfig(config))
  }

  public getTranslationForBasicEdit(localeCode: string, config?: RequestConfig): Observable<CustomTranslationEditData> {
    return this.http.get<CustomTranslationEditData>(`/api/translation/edit/basic/${localeCode}`, defaultHttpOptionsFromConfig(config))
  }

  public getFullTranslation(localeCode: string, config?: RequestConfig): Observable<object> {
    return this.http.get<object>(`/api/translation/full/${localeCode}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadFullTranslation(localeCode: string, config?: RequestConfig): Observable<object> {
    return this.resourcesService.downloadResource(`/api/translation/full/${localeCode}/download`, config);
  }

  public deleteCustomTranslation(localeCode: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/translation/custom/${localeCode}`, defaultHttpOptionsFromConfig(config))
  }

  public deleteCustomTranslationKey(localesCode: string, key: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/translation/custom/${localesCode}/${key}`, defaultHttpOptionsFromConfig(config))
  }

  public saveCustomTranslation(localeCode: string, customTranslationValue: object, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/translation/custom/${localeCode}`, customTranslationValue, defaultHttpOptionsFromConfig(config))
  }

  public getCustomTranslation(localeCode: string, config?: RequestConfig): Observable<object> {
    return this.http.get<object>(`/api/translation/custom/${localeCode}`, defaultHttpOptionsFromConfig(config))
  }

  public patchCustomTranslation(localeCode: string, newCustomTranslation: object, config?: RequestConfig): Observable<void> {
    return this.http.patch<void>(`/api/translation/custom/${localeCode}`, newCustomTranslation, defaultHttpOptionsFromConfig(config))
  }
}

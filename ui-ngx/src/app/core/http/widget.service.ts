///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Injectable, Type } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable, of, ReplaySubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import {
  BaseWidgetType,
  DeprecatedFilter,
  fullWidgetTypeFqn,
  IWidgetSettingsComponent,
  migrateWidgetTypeToDynamicForms,
  WidgetSettingsComponent,
  WidgetType,
  widgetType,
  WidgetTypeDetails,
  WidgetTypeInfo,
  widgetTypesData
} from '@shared/models/widget.models';
import { toWidgetInfo, toWidgetTypeDetails, WidgetInfo } from '@app/modules/home/models/widget-component.models';
import { filter, map, mergeMap, tap } from 'rxjs/operators';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActivationEnd, Router } from '@angular/router';
import {
  BasicWidgetConfigComponent,
  IBasicWidgetConfigComponent
} from '@home/components/widget/config/widget-config.component.models';
import { ResourcesService } from '@core/services/resources.service';

@Injectable({
  providedIn: 'root'
})
export class WidgetService {

  private allWidgetsBundles: Array<WidgetsBundle>;
  private systemWidgetsBundles: Array<WidgetsBundle>;
  private tenantWidgetsBundles: Array<WidgetsBundle>;

  private widgetsInfoInMemoryCache = new Map<string, WidgetInfo>();

  private loadWidgetsBundleCacheSubject: ReplaySubject<void>;

  private basicWidgetSettingsComponentsMap: { [key: string]: Type<IBasicWidgetConfigComponent> } = {};
  private widgetSettingsComponentsMap: { [key: string]: Type<IWidgetSettingsComponent> } = {};

  constructor(
    private http: HttpClient,
    private router: Router,
    private resourcesService: ResourcesService,
  ) {
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(
      () => {
        this.invalidateWidgetsBundleCache();
      }
    );
  }

  public getWidgetScopeVariables(): string[] {
    return ['tinycolor', 'cssjs', 'moment', '$', 'jQuery'];
  }

  public getAllWidgetsBundles(config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(config).pipe(
      map(() => this.allWidgetsBundles)
    );
  }

  public getSystemWidgetsBundles(config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(config).pipe(
      map(() => this.systemWidgetsBundles)
    );
  }

  public getTenantWidgetsBundles(config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(config).pipe(
      map(() => this.tenantWidgetsBundles)
    );
  }

  public getWidgetBundles(pageLink: PageLink, fullSearch = false,
                          tenantOnly = false, scadaFirst = false, config?: RequestConfig): Observable<PageData<WidgetsBundle>> {
    return this.http.get<PageData<WidgetsBundle>>(
      `/api/widgetsBundles${pageLink.toQuery()}&tenantOnly=${tenantOnly}&fullSearch=${fullSearch}&scadaFirst=${scadaFirst}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public getWidgetsBundle(widgetsBundleId: string,
                          config?: RequestConfig): Observable<WidgetsBundle> {
    return this.http.get<WidgetsBundle>(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptionsFromConfig(config));
  }

  public exportWidgetsBundle(widgetsBundleId: string,
                          config?: RequestConfig): Observable<WidgetsBundle> {
    return this.http.get<WidgetsBundle>(`/api/widgetsBundle/${widgetsBundleId}?inlineImages=true`, defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetsBundle(widgetsBundle: WidgetsBundle,
                           config?: RequestConfig): Observable<WidgetsBundle> {
    return this.http.post<WidgetsBundle>('/api/widgetsBundle', widgetsBundle,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        this.invalidateWidgetsBundleCache();
      })
    );
  }

  public updateWidgetsBundleWidgetTypes(widgetsBundleId: string, widgetTypeIds: Array<string>,
                                        config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/widgetsBundle/${widgetsBundleId}/widgetTypes`, widgetTypeIds,
      defaultHttpOptionsFromConfig(config));
  }

  public updateWidgetsBundleWidgetFqns(widgetsBundleId: string, widgetTypeFqns: Array<string>,
                                       config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/widgetsBundle/${widgetsBundleId}/widgetTypeFqns`, widgetTypeFqns,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteWidgetsBundle(widgetsBundleId: string, config?: RequestConfig) {
    return this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptionsFromConfig(config))
      .pipe(
        tap(() => this.invalidateWidgetsBundleCache())
      );
  }

  public getBundleWidgetTypes(widgetsBundleId: string,
                              config?: RequestConfig): Observable<Array<WidgetType>> {
    return this.http.get<Array<WidgetType>>(`/api/widgetTypes?widgetsBundleId=${widgetsBundleId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public exportBundleWidgetTypesDetails(widgetsBundleId: string,
                                        includeResources = true,
                                        config?: RequestConfig): Observable<Array<WidgetTypeDetails>> {
    let url = `/api/widgetTypesDetails?widgetsBundleId=${widgetsBundleId}`
    if (includeResources) {
      url += '&includeResources=true';
    }
    return this.http.get<Array<WidgetTypeDetails>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypeFqns(widgetsBundleId: string,
                                 config?: RequestConfig): Observable<Array<string>> {
    return this.http.get<Array<string>>(`/api/widgetTypeFqns?widgetsBundleId=${widgetsBundleId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypeInfosList(widgetsBundleId: string,
                                      config?: RequestConfig): Observable<Array<WidgetTypeInfo>> {
    return this.getBundleWidgetTypeInfos(new PageLink(1024), widgetsBundleId, false, DeprecatedFilter.ALL, null, config).pipe(
      map((data) => data.data)
    );
  }

  public getBundleWidgetTypeInfos(pageLink: PageLink,
                                  widgetsBundleId: string,
                                  fullSearch = false,
                                  deprecatedFilter = DeprecatedFilter.ALL,
                                  widgetTypes: Array<widgetType> = null,
                                  config?: RequestConfig): Observable<PageData<WidgetTypeInfo>> {

    let url =
      `/api/widgetTypesInfos${pageLink.toQuery()}&widgetsBundleId=${widgetsBundleId}` +
      `&fullSearch=${fullSearch}&deprecatedFilter=${deprecatedFilter}`;
    if (widgetTypes && widgetTypes.length) {
      url += `&widgetTypeList=${widgetTypes.join(',')}`;
    }
    return this.http.get<PageData<WidgetTypeInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getWidgetType(fullFqn: string, config?: RequestConfig): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType?fqn=${fullFqn}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetTypeDetails(widgetInfo: WidgetInfo,
                               id: WidgetTypeId,
                               createdTime: number,
                               version: number,
                               config?: RequestConfig): Observable<WidgetTypeDetails> {
    const widgetTypeDetails = toWidgetTypeDetails(widgetInfo, id, undefined, createdTime, version);
    return this.http.post<WidgetTypeDetails>('/api/widgetType', widgetTypeDetails,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdated(savedWidgetType);
      }));
  }

  public saveImportedWidgetTypeDetails(widgetTypeDetails: WidgetTypeDetails,
                                       config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.post<WidgetTypeDetails>('/api/widgetType?updateExistingByFqn=true', widgetTypeDetails,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdated(savedWidgetType);
      }));
  }

  public getWidgetTypeById(widgetTypeId: string,
                           config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.get<WidgetTypeDetails>(`/api/widgetType/${widgetTypeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public exportWidgetType(widgetTypeId: string,
                          includeResources = true,
                          config?: RequestConfig): Observable<WidgetTypeDetails> {
    let url = `/api/widgetType/${widgetTypeId}`;
    if (includeResources) {
      url += '?includeResources=true';
    }
    return this.http.get<WidgetTypeDetails>(url, defaultHttpOptionsFromConfig(config));
  }

  public getWidgetTypeInfoById(widgetTypeId: string,
                               config?: RequestConfig): Observable<WidgetTypeInfo> {
    return this.http.get<WidgetTypeInfo>(`/api/widgetTypeInfo/${widgetTypeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetType(widgetTypeDetails: WidgetTypeDetails,
                        config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.post<WidgetTypeDetails>(`/api/widgetType`, widgetTypeDetails,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteWidgetType(widgetTypeId: string,
                          config?: RequestConfig) {
    return this.getWidgetTypeById(widgetTypeId, config).pipe(
      mergeMap((widgetTypeDetails) =>
        this.http.delete(`/api/widgetType/${widgetTypeId}`, defaultHttpOptionsFromConfig(config)).pipe(
          tap(() => {
            this.widgetTypeUpdated(widgetTypeDetails);
          })
        )
    ));
  }

  public getWidgetTypes(pageLink: PageLink, tenantOnly = false,
                        fullSearch = false, scadaFirst = false,
                        deprecatedFilter = DeprecatedFilter.ALL,
                        widgetTypes: Array<widgetType> = null,
                        config?: RequestConfig): Observable<PageData<WidgetTypeInfo>> {
    let url =
      `/api/widgetTypes${pageLink.toQuery()}&tenantOnly=${tenantOnly}&fullSearch=${fullSearch}
      &scadaFirst=${scadaFirst}&deprecatedFilter=${deprecatedFilter}`;
    if (widgetTypes && widgetTypes.length) {
      url += `&widgetTypeList=${widgetTypes.join(',')}`;
    }
    return this.http.get<PageData<WidgetTypeInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public addWidgetFqnToWidgetBundle(widgetsBundleId: string, fqn: string, config?: RequestConfig) {
    return this.getBundleWidgetTypeFqns(widgetsBundleId, config).pipe(
      mergeMap(widgetsBundleFqn => {
        widgetsBundleFqn.push(fqn);
        return this.updateWidgetsBundleWidgetFqns(widgetsBundleId, widgetsBundleFqn, config);
      })
    );
  }

  public getWidgetTemplate(widgetTypeParam: widgetType,
                           config?: RequestConfig): Observable<WidgetInfo> {
    const templateWidgetType = widgetTypesData.get(widgetTypeParam);
    return this.getWidgetType(templateWidgetType.template.fullFqn,
      config).pipe(
        map((result) => {
          result = migrateWidgetTypeToDynamicForms(result);
          const widgetInfo = toWidgetInfo(result);
          widgetInfo.fullFqn = undefined;
          return widgetInfo;
        })
      );
  }

  public getWidgetInfoFromCache(fullFqn: string): WidgetInfo | undefined {
    return this.widgetsInfoInMemoryCache.get(fullFqn);
  }

  public putWidgetInfoToCache(widgetInfo: WidgetInfo) {
    this.widgetsInfoInMemoryCache.set(widgetInfo.fullFqn, widgetInfo);
  }

  public registerBasicWidgetConfigComponents(module: any) {
    Object.assign(this.basicWidgetSettingsComponentsMap, this.resourcesService.extractComponentsFromModule<IBasicWidgetConfigComponent>(module, BasicWidgetConfigComponent));
  }

  public getBasicWidgetSettingsComponentBySelector(selector: string): Type<IBasicWidgetConfigComponent> {
    return this.basicWidgetSettingsComponentsMap[selector];
  }

  public putBasicWidgetSettingsComponentToMap(selector: string, compType: Type<IBasicWidgetConfigComponent>) {
    this.basicWidgetSettingsComponentsMap[selector] = compType;
  }

  public registerWidgetSettingsComponents(module: any) {
    Object.assign(this.widgetSettingsComponentsMap, this.resourcesService.extractComponentsFromModule<IWidgetSettingsComponent>(module, WidgetSettingsComponent));
  }

  public getWidgetSettingsComponentTypeBySelector(selector: string): Type<IWidgetSettingsComponent> {
    return this.widgetSettingsComponentsMap[selector];
  }

  public putWidgetSettingsComponentToMap(selector: string, compType: Type<IWidgetSettingsComponent>) {
    this.widgetSettingsComponentsMap[selector] = compType;
  }

  private widgetTypeUpdated(updatedWidgetType: BaseWidgetType): void {
    this.deleteWidgetInfoFromCache(fullWidgetTypeFqn(updatedWidgetType));
  }

  public deleteWidgetInfoFromCache(fullFqn: string) {
    this.widgetsInfoInMemoryCache.delete(fullFqn);
  }

  public getWidgetsBundlesByIds(widgetsBundleIds: Array<string>, config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.http.get<Array<WidgetsBundle>>(`/api/widgetsBundles?widgetsBundleIds=${widgetsBundleIds.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

  private loadWidgetsBundleCache(config?: RequestConfig): Observable<any> {
    if (!this.allWidgetsBundles) {
      if (!this.loadWidgetsBundleCacheSubject) {
        this.loadWidgetsBundleCacheSubject = new ReplaySubject<void>();
        this.http.get<Array<WidgetsBundle>>('/api/widgetsBundles',
          defaultHttpOptionsFromConfig(config)).subscribe(
          (allWidgetsBundles) => {
            this.allWidgetsBundles = allWidgetsBundles;
            this.systemWidgetsBundles = new Array<WidgetsBundle>();
            this.tenantWidgetsBundles = new Array<WidgetsBundle>();
            this.allWidgetsBundles = this.allWidgetsBundles.sort((wb1, wb2) => {
              let res = wb1.title.localeCompare(wb2.title);
              if (res === 0) {
                res = wb2.createdTime - wb1.createdTime;
              }
              return res;
            });
            this.allWidgetsBundles.forEach((widgetsBundle) => {
              if (widgetsBundle.tenantId.id === NULL_UUID) {
                this.systemWidgetsBundles.push(widgetsBundle);
              } else {
                this.tenantWidgetsBundles.push(widgetsBundle);
              }
            });
            this.loadWidgetsBundleCacheSubject.next();
            this.loadWidgetsBundleCacheSubject.complete();
          },
          () => {
            this.loadWidgetsBundleCacheSubject.error(null);
          });
      }
      return this.loadWidgetsBundleCacheSubject.asObservable();
    } else {
      return of(null);
    }
  }

  private invalidateWidgetsBundleCache() {
    this.allWidgetsBundles = undefined;
    this.systemWidgetsBundles = undefined;
    this.tenantWidgetsBundles = undefined;
    this.loadWidgetsBundleCacheSubject = undefined;
  }
}

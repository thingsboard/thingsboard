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
import { Observable, of, ReplaySubject, Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { Widget, WidgetType, widgetType, widgetTypesData } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ResourcesService } from '../services/resources.service';
import { toWidgetInfo, toWidgetType, WidgetInfo } from '@app/modules/home/models/widget-component.models';
import { filter, map, mergeMap, tap } from 'rxjs/operators';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActivationEnd, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class WidgetService {

  private widgetTypeUpdatedSubject = new Subject<WidgetType>();
  private widgetsBundleDeletedSubject = new Subject<WidgetsBundle>();

  private allWidgetsBundles: Array<WidgetsBundle>;
  private systemWidgetsBundles: Array<WidgetsBundle>;
  private tenantWidgetsBundles: Array<WidgetsBundle>;

  private loadWidgetsBundleCacheSubject: ReplaySubject<any>;

  constructor(
    private http: HttpClient,
    private utils: UtilsService,
    private resources: ResourcesService,
    private translate: TranslateService,
    private router: Router
  ) {
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(
      () => {
        this.invalidateWidgetsBundleCache();
      }
    );
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

  public getWidgetBundles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<WidgetsBundle>> {
    return this.http.get<PageData<WidgetsBundle>>(`/api/widgetsBundles${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getWidgetsBundle(widgetsBundleId: string,
                          config?: RequestConfig): Observable<WidgetsBundle> {
    return this.http.get<WidgetsBundle>(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptionsFromConfig(config));
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

  public deleteWidgetsBundle(widgetsBundleId: string, config?: RequestConfig) {
    return this.getWidgetsBundle(widgetsBundleId, config).pipe(
      mergeMap((widgetsBundle) => {
        return this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`,
          defaultHttpOptionsFromConfig(config)).pipe(
          tap(() => {
            this.invalidateWidgetsBundleCache();
            this.widgetsBundleDeletedSubject.next(widgetsBundle);
          })
        );
      }
    ));
  }

  public getBundleWidgetTypes(bundleAlias: string, isSystem: boolean,
                              config?: RequestConfig): Observable<Array<WidgetType>> {
    return this.http.get<Array<WidgetType>>(`/api/widgetTypes?isSystem=${isSystem}&bundleAlias=${bundleAlias}`,
      defaultHttpOptionsFromConfig(config));
  }

  public loadBundleLibraryWidgets(bundleAlias: string, isSystem: boolean,
                                  config?: RequestConfig): Observable<Array<Widget>> {
    return this.getBundleWidgetTypes(bundleAlias, isSystem, config).pipe(
      map((types) => {
        types = types.sort((a, b) => {
          let result = widgetType[b.descriptor.type].localeCompare(widgetType[a.descriptor.type]);
          if (result === 0) {
            result = b.createdTime - a.createdTime;
          }
          return result;
        });
        const widgetTypes = new Array<Widget>();
        let top = 0;
        const lastTop = [0, 0, 0];
        let col = 0;
        let column = 0;
        types.forEach((type) => {
          const widgetTypeInfo = toWidgetInfo(type);
          const sizeX = 8;
          const sizeY = Math.floor(widgetTypeInfo.sizeY);
          const widget: Widget = {
            typeId: type.id,
            isSystemType: isSystem,
            bundleAlias,
            typeAlias: widgetTypeInfo.alias,
            type: widgetTypeInfo.type,
            title: widgetTypeInfo.widgetName,
            sizeX,
            sizeY,
            row: top,
            col,
            config: JSON.parse(widgetTypeInfo.defaultConfig)
          };

          widget.config.title = widgetTypeInfo.widgetName;

          widgetTypes.push(widget);
          top += sizeY;
          if (top > lastTop[column] + 10) {
            lastTop[column] = top;
            column++;
            if (column > 2) {
              column = 0;
            }
            top = lastTop[column];
            col = column * 8;
          }
        });
        return widgetTypes;
      })
    );
  }

  public getWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                       config?: RequestConfig): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType?isSystem=${isSystem}&bundleAlias=${bundleAlias}&alias=${widgetTypeAlias}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetType(widgetInfo: WidgetInfo,
                        id: WidgetTypeId,
                        bundleAlias: string,
                        config?: RequestConfig): Observable<WidgetType> {
    const widgetTypeInstance = toWidgetType(widgetInfo, id, undefined, bundleAlias);
    return this.http.post<WidgetType>('/api/widgetType', widgetTypeInstance,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdatedSubject.next(savedWidgetType);
      }));
  }

  public saveImportedWidgetType(widgetTypeInstance: WidgetType,
                                config?: RequestConfig): Observable<WidgetType> {
    return this.http.post<WidgetType>('/api/widgetType', widgetTypeInstance,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdatedSubject.next(savedWidgetType);
      }));
  }

  public deleteWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                          config?: RequestConfig) {
    return this.getWidgetType(bundleAlias, widgetTypeAlias, isSystem, config).pipe(
      mergeMap((widgetTypeInstance) => {
          return this.http.delete(`/api/widgetType/${widgetTypeInstance.id.id}`,
            defaultHttpOptionsFromConfig(config)).pipe(
            tap(() => {
              this.widgetTypeUpdatedSubject.next(widgetTypeInstance);
            })
          );
        }
      ));
  }

  public getWidgetTypeById(widgetTypeId: string,
                           config?: RequestConfig): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType/${widgetTypeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getWidgetTemplate(widgetTypeParam: widgetType,
                           config?: RequestConfig): Observable<WidgetInfo> {
    const templateWidgetType = widgetTypesData.get(widgetTypeParam);
    return this.getWidgetType(templateWidgetType.template.bundleAlias, templateWidgetType.template.alias, true,
      config).pipe(
        map((result) => {
          const widgetInfo = toWidgetInfo(result);
          widgetInfo.alias = undefined;
          return widgetInfo;
        })
      );
  }

  public onWidgetTypeUpdated(): Observable<WidgetType> {
    return this.widgetTypeUpdatedSubject.asObservable();
  }

  public onWidgetBundleDeleted(): Observable<WidgetsBundle> {
    return this.widgetsBundleDeletedSubject.asObservable();
  }

  private loadWidgetsBundleCache(config?: RequestConfig): Observable<any> {
    if (!this.allWidgetsBundles) {
      if (!this.loadWidgetsBundleCacheSubject) {
        this.loadWidgetsBundleCacheSubject = new ReplaySubject();
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

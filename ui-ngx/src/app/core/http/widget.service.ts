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
import { Observable, Subject, of, ReplaySubject } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetType, widgetType, WidgetTypeData, widgetTypesData } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ResourcesService } from '../services/resources.service';
import { toWidgetInfo, WidgetInfo, toWidgetType } from '@app/modules/home/models/widget-component.models';
import { map, tap, mergeMap, filter } from 'rxjs/operators';
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

  public getAllWidgetsBundles(ignoreErrors: boolean = false,
                              ignoreLoading: boolean = false): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(ignoreErrors, ignoreLoading).pipe(
      map(() => this.allWidgetsBundles)
    );
  }

  public getSystemWidgetsBundles(ignoreErrors: boolean = false,
                                 ignoreLoading: boolean = false): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(ignoreErrors, ignoreLoading).pipe(
      map(() => this.systemWidgetsBundles)
    );
  }

  public getTenantWidgetsBundles(ignoreErrors: boolean = false,
                                 ignoreLoading: boolean = false): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(ignoreErrors, ignoreLoading).pipe(
      map(() => this.tenantWidgetsBundles)
    );
  }

  public getWidgetBundles(pageLink: PageLink, ignoreErrors: boolean = false,
                          ignoreLoading: boolean = false): Observable<PageData<WidgetsBundle>> {
    return this.http.get<PageData<WidgetsBundle>>(`/api/widgetsBundles${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getWidgetsBundle(widgetsBundleId: string,
                          ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetsBundle> {
    return this.http.get<WidgetsBundle>(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveWidgetsBundle(widgetsBundle: WidgetsBundle,
                           ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetsBundle> {
    return this.http.post<WidgetsBundle>('/api/widgetsBundle', widgetsBundle,
      defaultHttpOptions(ignoreLoading, ignoreErrors)).pipe(
      tap(() => {
        this.invalidateWidgetsBundleCache();
      })
    );
  }

  public deleteWidgetsBundle(widgetsBundleId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.getWidgetsBundle(widgetsBundleId, ignoreErrors, ignoreLoading).pipe(
      mergeMap((widgetsBundle) => {
        return this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`,
          defaultHttpOptions(ignoreLoading, ignoreErrors)).pipe(
          tap(() => {
            this.invalidateWidgetsBundleCache();
            this.widgetsBundleDeletedSubject.next(widgetsBundle);
          })
        );
      }
    ));
  }

  public getBundleWidgetTypes(bundleAlias: string, isSystem: boolean,
                              ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<WidgetType>> {
    return this.http.get<Array<WidgetType>>(`/api/widgetTypes?isSystem=${isSystem}&bundleAlias=${bundleAlias}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                       ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType?isSystem=${isSystem}&bundleAlias=${bundleAlias}&alias=${widgetTypeAlias}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveWidgetType(widgetInfo: WidgetInfo,
                        id: WidgetTypeId,
                        bundleAlias: string,
                        ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetType> {
    const widgetTypeInstance = toWidgetType(widgetInfo, id, undefined, bundleAlias);
    return this.http.post<WidgetType>('/api/widgetType', widgetTypeInstance,
      defaultHttpOptions(ignoreLoading, ignoreErrors)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdatedSubject.next(savedWidgetType);
      }));
  }

  public saveImportedWidgetType(widgetTypeInstance: WidgetType,
                                ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetType> {
    return this.http.post<WidgetType>('/api/widgetType', widgetTypeInstance,
      defaultHttpOptions(ignoreLoading, ignoreErrors)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdatedSubject.next(savedWidgetType);
      }));
  }

  public deleteWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                          ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.getWidgetType(bundleAlias, widgetTypeAlias, isSystem, ignoreErrors, ignoreLoading).pipe(
      mergeMap((widgetTypeInstance) => {
          return this.http.delete(`/api/widgetType/${widgetTypeInstance.id.id}`,
            defaultHttpOptions(ignoreLoading, ignoreErrors)).pipe(
            tap(() => {
              this.widgetTypeUpdatedSubject.next(widgetTypeInstance);
            })
          );
        }
      ));
  }

  public getWidgetTypeById(widgetTypeId: string,
                           ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType/${widgetTypeId}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getWidgetTemplate(widgetTypeParam: widgetType,
                           ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetInfo> {
    const templateWidgetType = widgetTypesData.get(widgetTypeParam);
    return this.getWidgetType(templateWidgetType.template.bundleAlias, templateWidgetType.template.alias, true,
      ignoreErrors, ignoreLoading).pipe(
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

  private loadWidgetsBundleCache(ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<any> {
    if (!this.allWidgetsBundles) {
      const loadWidgetsBundleCacheSubject = new ReplaySubject();
      this.http.get<Array<WidgetsBundle>>('/api/widgetsBundles',
        defaultHttpOptions(ignoreLoading, ignoreErrors)).subscribe(
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
          loadWidgetsBundleCacheSubject.next();
          loadWidgetsBundleCacheSubject.complete();
        },
        () => {
          loadWidgetsBundleCacheSubject.error(null);
        });
      return loadWidgetsBundleCacheSubject.asObservable();
    } else {
      return of(null);
    }
  }

  private invalidateWidgetsBundleCache() {
    this.allWidgetsBundles = undefined;
    this.systemWidgetsBundles = undefined;
    this.tenantWidgetsBundles = undefined;
  }

}

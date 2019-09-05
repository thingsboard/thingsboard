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

import {Injectable} from '@angular/core';
import {defaultHttpOptions} from './http-utils';
import { Observable, ReplaySubject, Subject, of, forkJoin, throwError } from 'rxjs/index';
import {HttpClient} from '@angular/common/http';
import {PageLink} from '@shared/models/page/page-link';
import {PageData} from '@shared/models/page/page-data';
import {WidgetsBundle} from '@shared/models/widgets-bundle.model';
import {
  WidgetControllerDescriptor,
  WidgetInfo,
  WidgetType,
  WidgetTypeInstance,
  widgetActionSources,
  MissingWidgetType, toWidgetInfo, ErrorWidgetType
} from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { isFunction, isUndefined } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { AuthPayload } from '@core/auth/auth.models';
import cssjs from '@core/css/css';
import { ResourcesService } from '../services/resources.service';
import { catchError, map, switchMap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class WidgetService {

  private cssParser = new cssjs();

  private widgetsInfoInMemoryCache = new Map<string, WidgetInfo>();

  private widgetsInfoFetchQueue = new Map<string, Array<Subject<WidgetInfo>>>();

  constructor(
    private http: HttpClient,
    private utils: UtilsService,
    private resources: ResourcesService,
    private translate: TranslateService
  ) {
    this.cssParser.testMode = false;
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
    return this.http.post<WidgetsBundle>('/api/widgetsBundle', widgetsBundle, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteWidgetsBundle(widgetsBundleId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
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

  public getWidgetInfo(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): Observable<WidgetInfo> {
    const widgetInfoSubject = new ReplaySubject<WidgetInfo>();
    const widgetInfo = this.getWidgetInfoFromCache(bundleAlias, widgetTypeAlias, isSystem);
    if (widgetInfo) {
      widgetInfoSubject.next(widgetInfo);
      widgetInfoSubject.complete();
    } else {
      if (this.utils.widgetEditMode) {
        // TODO:
      } else {
        const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        let fetchQueue = this.widgetsInfoFetchQueue.get(key);
        if (fetchQueue) {
          fetchQueue.push(widgetInfoSubject);
        } else {
          fetchQueue = new Array<Subject<WidgetInfo>>();
          this.widgetsInfoFetchQueue.set(key, fetchQueue);
          this.getWidgetType(bundleAlias, widgetTypeAlias, isSystem).subscribe(
            (widgetType) => {
              this.loadWidget(widgetType, bundleAlias, isSystem, widgetInfoSubject);
            },
            () => {
              widgetInfoSubject.next(MissingWidgetType);
              widgetInfoSubject.complete();
              this.resolveWidgetsInfoFetchQueue(key, MissingWidgetType);
            }
          );
        }
      }
    }
    return widgetInfoSubject.asObservable();
  }

  private loadWidget(widgetType: WidgetType, bundleAlias: string, isSystem: boolean, widgetInfoSubject: Subject<WidgetInfo>) {
    const widgetInfo = toWidgetInfo(widgetType);
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetInfo.alias, isSystem);
    this.loadWidgetResources(widgetInfo, bundleAlias, isSystem).subscribe(
      () => {
        let widgetControllerDescriptor: WidgetControllerDescriptor = null;
        try {
          widgetControllerDescriptor = this.createWidgetControllerDescriptor(widgetInfo, key);
        } catch (e) {
          const details = this.utils.parseException(e);
          const errorMessage = `Failed to compile widget script. \n Error: ${details.message}`;
          this.processWidgetLoadError([errorMessage], key, widgetInfoSubject);
        }
        if (widgetControllerDescriptor) {
          if (widgetControllerDescriptor.settingsSchema) {
            widgetInfo.typeSettingsSchema = widgetControllerDescriptor.settingsSchema;
          }
          if (widgetControllerDescriptor.dataKeySettingsSchema) {
            widgetInfo.typeDataKeySettingsSchema = widgetControllerDescriptor.dataKeySettingsSchema;
          }
          widgetInfo.typeParameters = widgetControllerDescriptor.typeParameters;
          widgetInfo.actionSources = widgetControllerDescriptor.actionSources;
          widgetInfo.widgetTypeFunction = widgetControllerDescriptor.widgetTypeFunction;
          this.putWidgetInfoToCache(widgetInfo, bundleAlias, widgetInfo.alias, isSystem);
          if (widgetInfoSubject) {
            widgetInfoSubject.next(widgetInfo);
            widgetInfoSubject.complete();
          }
          this.resolveWidgetsInfoFetchQueue(key, widgetInfo);
        }
      },
      (errorMessages: string[]) => {
        this.processWidgetLoadError(errorMessages, key, widgetInfoSubject);
      }
    );
  }

  private loadWidgetResources(widgetInfo: WidgetInfo, bundleAlias: string, isSystem: boolean): Observable<any> {
    const widgetNamespace = `widget-type-${(isSystem ? 'sys-' : '')}${bundleAlias}-${widgetInfo.alias}`;
    this.cssParser.cssPreviewNamespace = widgetNamespace;
    this.cssParser.createStyleElement(widgetNamespace, widgetInfo.templateCss);
    const resourceTasks: Observable<string>[] = [];
    if (widgetInfo.resources.length > 0) {
      widgetInfo.resources.forEach((resource) => {
        resourceTasks.push(
          this.resources.loadResource(resource.url).pipe(
            catchError(e => of(`Failed to load widget resource: '${resource.url}'`))
          )
        );
      });
      return forkJoin(resourceTasks).pipe(
        switchMap(msgs => {
          let errors: string[];
          if (msgs && msgs.length) {
            errors = msgs.filter(msg => msg && msg.length > 0);
          }
          if (errors && errors.length) {
            return throwError(errors);
          } else {
            return of(null);
          }
        }
      ));
    } else {
      return of(null);
    }
  }

  private createWidgetControllerDescriptor(widgetInfo: WidgetInfo, name: string): WidgetControllerDescriptor {
    let widgetTypeFunctionBody = `return function ${name} (ctx) {\n` +
      '    var self = this;\n' +
      '    self.ctx = ctx;\n\n'; /*+

         '    self.onInit = function() {\n\n' +

         '    }\n\n' +

         '    self.onDataUpdated = function() {\n\n' +

         '    }\n\n' +

         '    self.useCustomDatasources = function() {\n\n' +

         '    }\n\n' +

         '    self.typeParameters = function() {\n\n' +
                    return {
                                useCustomDatasources: false,
                                maxDatasources: -1, //unlimited
                                maxDataKeys: -1, //unlimited
                                dataKeysOptional: false,
                                stateData: false
                           };
         '    }\n\n' +

         '    self.actionSources = function() {\n\n' +
                    return {
                                'headerButton': {
                                   name: 'Header button',
                                   multiple: true
                                }
                            };
              }\n\n' +
         '    self.onResize = function() {\n\n' +

         '    }\n\n' +

         '    self.onEditModeChanged = function() {\n\n' +

         '    }\n\n' +

         '    self.onMobileModeChanged = function() {\n\n' +

         '    }\n\n' +

         '    self.getSettingsSchema = function() {\n\n' +

         '    }\n\n' +

         '    self.getDataKeySettingsSchema = function() {\n\n' +

         '    }\n\n' +

         '    self.onDestroy = function() {\n\n' +

         '    }\n\n' +
         '}';*/

    widgetTypeFunctionBody += widgetInfo.controllerScript;
    widgetTypeFunctionBody += '\n};\n';

    try {

      const widgetTypeFunction = new Function(widgetTypeFunctionBody);
      const widgetType = widgetTypeFunction.apply(this);
      const widgetTypeInstance: WidgetTypeInstance = new widgetType();
      const result: WidgetControllerDescriptor = {
        widgetTypeFunction: widgetType
      };
      if (isFunction(widgetTypeInstance.getSettingsSchema)) {
        result.settingsSchema = widgetTypeInstance.getSettingsSchema();
      }
      if (isFunction(widgetTypeInstance.getDataKeySettingsSchema)) {
        result.dataKeySettingsSchema = widgetTypeInstance.getDataKeySettingsSchema();
      }
      if (isFunction(widgetTypeInstance.typeParameters)) {
        result.typeParameters = widgetTypeInstance.typeParameters();
      } else {
        result.typeParameters = {};
      }
      if (isFunction(widgetTypeInstance.useCustomDatasources)) {
        result.typeParameters.useCustomDatasources = widgetTypeInstance.useCustomDatasources();
      } else {
        result.typeParameters.useCustomDatasources = false;
      }
      if (isUndefined(result.typeParameters.maxDatasources)) {
        result.typeParameters.maxDatasources = -1;
      }
      if (isUndefined(result.typeParameters.maxDataKeys)) {
        result.typeParameters.maxDataKeys = -1;
      }
      if (isUndefined(result.typeParameters.dataKeysOptional)) {
        result.typeParameters.dataKeysOptional = false;
      }
      if (isUndefined(result.typeParameters.stateData)) {
        result.typeParameters.stateData = false;
      }
      if (isFunction(widgetTypeInstance.actionSources)) {
        result.actionSources = widgetTypeInstance.actionSources();
      } else {
        result.actionSources = {};
      }
      for (const actionSourceId of Object.keys(widgetActionSources)) {
        result.actionSources[actionSourceId] = {...widgetActionSources[actionSourceId]};
        result.actionSources[actionSourceId].name = this.translate.instant(result.actionSources[actionSourceId].name);
      }
      return result;
    } catch (e) {
      this.utils.processWidgetException(e);
      throw e;
    }
  }

  private processWidgetLoadError(errorMessages: string[], cacheKey: string, widgetInfoSubject: Subject<WidgetInfo>) {
    const widgetInfo = {...ErrorWidgetType};
    errorMessages.forEach(error => {
      widgetInfo.templateHtml += `<div class="tb-widget-error-msg">${error}</div>`;
    });
    widgetInfo.templateHtml += '</div>';
    if (widgetInfoSubject) {
      widgetInfoSubject.next(widgetInfo);
      widgetInfoSubject.complete();
    }
    this.resolveWidgetsInfoFetchQueue(cacheKey, widgetInfo);
  }

  private resolveWidgetsInfoFetchQueue(key: string, widgetInfo: WidgetInfo) {
    const fetchQueue = this.widgetsInfoFetchQueue.get(key);
    if (fetchQueue) {
      fetchQueue.forEach(subject => {
        subject.next(widgetInfo);
        subject.complete();
      });
      this.widgetsInfoFetchQueue.delete(key);
    }
  }

  // Cache functions

  private createWidgetInfoCacheKey(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): string {
    return `${isSystem ? 'sys_' : ''}${bundleAlias}_${widgetTypeAlias}`;
  }

  private getWidgetInfoFromCache(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): WidgetInfo | undefined {
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
    return this.widgetsInfoInMemoryCache.get(key);
  }

  private putWidgetInfoToCache(widgetInfo: WidgetInfo, bundleAlias: string, widgetTypeAlias: string, isSystem: boolean) {
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
    this.widgetsInfoInMemoryCache.set(key, widgetInfo);
  }

}

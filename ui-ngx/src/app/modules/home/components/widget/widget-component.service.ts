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

import { Inject, Injectable } from '@angular/core';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { WidgetService } from '@core/http/widget.service';
import { forkJoin, Observable, of, ReplaySubject, Subject, throwError } from 'rxjs';
import { WidgetInfo, MissingWidgetType, toWidgetInfo, WidgetTypeInstance, ErrorWidgetType } from '@home/models/widget-component.models';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';
import { ResourcesService } from '@core/services/resources.service';
import {
  widgetActionSources,
  WidgetControllerDescriptor,
  WidgetType
} from '@shared/models/widget.models';
import { catchError, switchMap, map, mergeMap } from 'rxjs/operators';
import { isFunction, isUndefined } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { DynamicWidgetComponent } from '@home/components/widget/dynamic-widget.component';
import { SharedModule } from '@shared/shared.module';
import { WidgetComponentsModule } from '@home/components/widget/widget-components.module';
import { WINDOW } from '@core/services/window.service';

import * as tinycolor from 'tinycolor2';
import { TbFlot } from './lib/flot-widget';

// declare var jQuery: any;

@Injectable()
export class WidgetComponentService {

  private cssParser = new cssjs();

  private widgetsInfoInMemoryCache = new Map<string, WidgetInfo>();

  private widgetsInfoFetchQueue = new Map<string, Array<Subject<WidgetInfo>>>();

  private init$: Observable<any>;

  private missingWidgetType: WidgetInfo;
  private errorWidgetType: WidgetInfo;

  constructor(@Inject(WINDOW) private window: Window,
              private dynamicComponentFactoryService: DynamicComponentFactoryService,
              private widgetService: WidgetService,
              private utils: UtilsService,
              private resources: ResourcesService,
              private translate: TranslateService) {
    // @ts-ignore
    this.window.tinycolor = tinycolor;
    // @ts-ignore
    this.window.cssjs = cssjs;
    // @ts-ignore
    this.window.TbFlot = TbFlot;

    this.cssParser.testMode = false;
    this.init();
  }

  private init(): Observable<any> {
    if (this.init$) {
      return this.init$;
    } else {
      this.missingWidgetType = {...MissingWidgetType};
      this.errorWidgetType = {...ErrorWidgetType};
      const initSubject = new ReplaySubject();
      this.init$ = initSubject.asObservable();
      const loadDefaultWidgetInfoTasks = [
        this.loadWidgetResources(this.missingWidgetType, 'global-widget-missing-type'),
        this.loadWidgetResources(this.errorWidgetType, 'global-widget-error-type'),
      ];
      forkJoin(loadDefaultWidgetInfoTasks).subscribe(
        () => {
          initSubject.next();
        },
        () => {
          console.error('Failed to load default widget types!');
          initSubject.error('Failed to load default widget types!');
        }
      );
      return this.init$;
    }
  }

  public getWidgetInfo(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): Observable<WidgetInfo> {
    return this.init().pipe(
      mergeMap(() => this.getWidgetInfoInternal(bundleAlias, widgetTypeAlias, isSystem))
    );
  }

  private getWidgetInfoInternal(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): Observable<WidgetInfo> {
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
          this.widgetService.getWidgetType(bundleAlias, widgetTypeAlias, isSystem, true, false).subscribe(
            (widgetType) => {
              this.loadWidget(widgetType, bundleAlias, isSystem, widgetInfoSubject);
            },
            () => {
              widgetInfoSubject.next(this.missingWidgetType);
              widgetInfoSubject.complete();
              this.resolveWidgetsInfoFetchQueue(key, this.missingWidgetType);
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
    let widgetControllerDescriptor: WidgetControllerDescriptor = null;
    try {
      widgetControllerDescriptor = this.createWidgetControllerDescriptor(widgetInfo, key);
    } catch (e) {
      const details = this.utils.parseException(e);
      const errorMessage = `Failed to compile widget script. \n Error: ${details.message}`;
      this.processWidgetLoadError([errorMessage], key, widgetInfoSubject);
    }
    if (widgetControllerDescriptor) {
      const widgetNamespace = `widget-type-${(isSystem ? 'sys-' : '')}${bundleAlias}-${widgetInfo.alias}`;
      this.loadWidgetResources(widgetInfo, widgetNamespace).subscribe(
        () => {
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
        },
        (errorMessages: string[]) => {
          this.processWidgetLoadError(errorMessages, key, widgetInfoSubject);
        }
      );
    }
  }

  private loadWidgetResources(widgetInfo: WidgetInfo, widgetNamespace: string): Observable<any> {
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
    }
    resourceTasks.push(
      this.dynamicComponentFactoryService.createDynamicComponentFactory(
        class DynamicWidgetComponentInstance extends DynamicWidgetComponent {},
        widgetInfo.templateHtml,
        [SharedModule, WidgetComponentsModule]
      ).pipe(
        map((factory) => {
          widgetInfo.componentFactory = factory;
          return null;
        }),
        catchError(e => {
          const details = this.utils.parseException(e);
          const errorMessage = `Failed to compile widget html. \n Error: ${details.message}`;
          return of(errorMessage);
        })
      )
    );
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
    if (widgetInfoSubject) {
      widgetInfoSubject.error({
        widgetInfo: this.errorWidgetType,
        errorMessages
      });
    }
    this.resolveWidgetsInfoFetchQueue(cacheKey, this.errorWidgetType, errorMessages);
  }

  private resolveWidgetsInfoFetchQueue(key: string, widgetInfo: WidgetInfo, errorMessages?: string[]) {
    const fetchQueue = this.widgetsInfoFetchQueue.get(key);
    if (fetchQueue) {
      fetchQueue.forEach(subject => {
        if (!errorMessages) {
          subject.next(widgetInfo);
          subject.complete();
        } else {
          subject.error({
            widgetInfo,
            errorMessages
          });
        }
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

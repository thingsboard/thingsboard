///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Inject, Injectable, Optional, Type } from '@angular/core';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { WidgetService } from '@core/http/widget.service';
import { forkJoin, from, Observable, of, ReplaySubject, Subject, throwError } from 'rxjs';
import {
  ErrorWidgetType,
  MissingWidgetType,
  toWidgetInfo,
  toWidgetType,
  WidgetInfo,
  WidgetTypeInstance
} from '@home/models/widget-component.models';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';
import { ResourcesService } from '@core/services/resources.service';
import { Widget, widgetActionSources, WidgetControllerDescriptor, WidgetType } from '@shared/models/widget.models';
import { catchError, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { isFunction, isUndefined } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { DynamicWidgetComponent } from '@home/components/widget/dynamic-widget.component';
import { WidgetComponentsModule } from '@home/components/widget/widget-components.module';
import { WINDOW } from '@core/services/window.service';

import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetTypeId } from '@app/shared/models/id/widget-type-id';
import { TenantId } from '@app/shared/models/id/tenant-id';
import { SharedModule } from '@shared/shared.module';
import { MODULES_MAP } from '@shared/public-api';
import * as tinycolor_ from 'tinycolor2';
import moment from 'moment';

const tinycolor = tinycolor_;

// @dynamic
@Injectable()
export class WidgetComponentService {

  private cssParser = new cssjs();

  private widgetsInfoInMemoryCache = new Map<string, WidgetInfo>();

  private widgetsInfoFetchQueue = new Map<string, Array<Subject<WidgetInfo>>>();

  private init$: Observable<any>;

  private missingWidgetType: WidgetInfo;
  private errorWidgetType: WidgetInfo;
  private editingWidgetType: WidgetType;

  constructor(@Inject(WINDOW) private window: Window,
              @Optional() @Inject(MODULES_MAP) private modulesMap: {[key: string]: any},
              private dynamicComponentFactoryService: DynamicComponentFactoryService,
              private widgetService: WidgetService,
              private utils: UtilsService,
              private resources: ResourcesService,
              private translate: TranslateService) {

    this.cssParser.testMode = false;

    this.widgetService.onWidgetTypeUpdated().subscribe((widgetType) => {
      this.deleteWidgetInfoFromCache(widgetType.bundleAlias, widgetType.alias, widgetType.tenantId.id === NULL_UUID);
    });

    this.widgetService.onWidgetBundleDeleted().subscribe((widgetsBundle) => {
      this.deleteWidgetsBundleFromCache(widgetsBundle.alias, widgetsBundle.tenantId.id === NULL_UUID);
    });

    this.init();
  }

  private init(): Observable<any> {
    if (this.init$) {
      return this.init$;
    } else {
      this.missingWidgetType = {...MissingWidgetType};
      this.errorWidgetType = {...ErrorWidgetType};
      if (this.utils.widgetEditMode) {
        this.editingWidgetType = toWidgetType(
          {
            widgetName: this.utils.editWidgetInfo.widgetName,
            alias: 'customWidget',
            type: this.utils.editWidgetInfo.type,
            sizeX: this.utils.editWidgetInfo.sizeX,
            sizeY: this.utils.editWidgetInfo.sizeY,
            resources: this.utils.editWidgetInfo.resources,
            templateHtml: this.utils.editWidgetInfo.templateHtml,
            templateCss: this.utils.editWidgetInfo.templateCss,
            controllerScript: this.utils.editWidgetInfo.controllerScript,
            settingsSchema: this.utils.editWidgetInfo.settingsSchema,
            dataKeySettingsSchema: this.utils.editWidgetInfo.dataKeySettingsSchema,
            image: this.utils.editWidgetInfo.image,
            description: this.utils.editWidgetInfo.description,
            defaultConfig: this.utils.editWidgetInfo.defaultConfig
          }, new WidgetTypeId('1'), new TenantId( NULL_UUID ), 'customWidgetBundle', undefined
        );
      }
      const initSubject = new ReplaySubject();
      this.init$ = initSubject.asObservable();

      const w = (this.window as any);

      w.tinycolor = tinycolor;
      w.cssjs = cssjs;
      w.moment = moment;
      w.$ = $;
      w.jQuery = $;

      const widgetModulesTasks: Observable<any>[] = [];
      widgetModulesTasks.push(from(import('jquery.terminal')));

      widgetModulesTasks.push(from(import('flot/src/jquery.flot.js')).pipe(
        mergeMap(() => {
          const flotJsPluginsTasks: Observable<any>[] = [];
          flotJsPluginsTasks.push(from(import('flot/lib/jquery.colorhelpers.js')));
          flotJsPluginsTasks.push(from(import('flot/src/plugins/jquery.flot.time.js')));
          flotJsPluginsTasks.push(from(import('flot/src/plugins/jquery.flot.selection.js')));
          flotJsPluginsTasks.push(from(import('flot/src/plugins/jquery.flot.pie.js')));
          flotJsPluginsTasks.push(from(import('flot/src/plugins/jquery.flot.crosshair.js')));
          flotJsPluginsTasks.push(from(import('flot/src/plugins/jquery.flot.stack.js')));
          flotJsPluginsTasks.push(from(import('flot/src/plugins/jquery.flot.symbol.js')));
          flotJsPluginsTasks.push(from(import('flot.curvedlines/curvedLines.js')));
          return forkJoin(flotJsPluginsTasks);
        })
      ));

      widgetModulesTasks.push(from(import('@home/components/widget/lib/flot-widget')).pipe(
        tap((mod) => {
          (window as any).TbFlot = mod.TbFlot;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/lib/analogue-compass')).pipe(
        tap((mod) => {
          (window as any).TbAnalogueCompass = mod.TbAnalogueCompass;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/lib/analogue-radial-gauge')).pipe(
        tap((mod) => {
          (window as any).TbAnalogueRadialGauge = mod.TbAnalogueRadialGauge;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/lib/analogue-linear-gauge')).pipe(
        tap((mod) => {
          (window as any).TbAnalogueLinearGauge = mod.TbAnalogueLinearGauge;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/lib/digital-gauge')).pipe(
        tap((mod) => {
          (window as any).TbCanvasDigitalGauge = mod.TbCanvasDigitalGauge;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/lib/maps/map-widget2')).pipe(
        tap((mod) => {
          (window as any).TbMapWidgetV2 = mod.TbMapWidgetV2;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/trip-animation/trip-animation.component')).pipe(
        tap((mod) => {
          (window as any).TbTripAnimationWidget = mod.TbTripAnimationWidget;
        }))
      );

      forkJoin(widgetModulesTasks).subscribe(
        () => {
          const loadDefaultWidgetInfoTasks = [
            this.loadWidgetResources(this.missingWidgetType, 'global-widget-missing-type', [SharedModule, WidgetComponentsModule]),
            this.loadWidgetResources(this.errorWidgetType, 'global-widget-error-type', [SharedModule, WidgetComponentsModule]),
          ];
          forkJoin(loadDefaultWidgetInfoTasks).subscribe(
            () => {
              initSubject.next();
            },
            (e) => {
              let errorMessages = ['Failed to load default widget types!'];
              if (e && e.length) {
                errorMessages = errorMessages.concat(e);
              }
              console.error('Failed to load default widget types!');
              initSubject.error({
                widgetInfo: this.errorWidgetType,
                errorMessages
              });
            }
          );
        },
        (e) => {
          let errorMessages = ['Failed to load widget modules!'];
          if (e && e.length) {
            errorMessages = errorMessages.concat(e);
          }
          console.error('Failed to load widget modules!');
          initSubject.error({
            widgetInfo: this.errorWidgetType,
            errorMessages
          });
        }
      );
      return this.init$;
    }
  }

  public getInstantWidgetInfo(widget: Widget): WidgetInfo {
    const widgetInfo = this.getWidgetInfoFromCache(widget.bundleAlias, widget.typeAlias, widget.isSystemType);
    if (widgetInfo) {
      return widgetInfo;
    } else {
      return {} as WidgetInfo;
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
        this.loadWidget(this.editingWidgetType, bundleAlias, isSystem, widgetInfoSubject);
      } else {
        const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        let fetchQueue = this.widgetsInfoFetchQueue.get(key);
        if (fetchQueue) {
          fetchQueue.push(widgetInfoSubject);
        } else {
          fetchQueue = new Array<Subject<WidgetInfo>>();
          this.widgetsInfoFetchQueue.set(key, fetchQueue);
          this.widgetService.getWidgetType(bundleAlias, widgetTypeAlias, isSystem, {ignoreErrors: true}).subscribe(
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
      this.loadWidgetResources(widgetInfo, widgetNamespace, [SharedModule, WidgetComponentsModule]).subscribe(
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

  private loadWidgetResources(widgetInfo: WidgetInfo, widgetNamespace: string, modules?: Type<any>[]): Observable<any> {
    this.cssParser.cssPreviewNamespace = widgetNamespace;
    this.cssParser.createStyleElement(widgetNamespace, widgetInfo.templateCss);
    const resourceTasks: Observable<string>[] = [];
    const modulesTasks: Observable<Type<any>[] | string>[] = [];
    if (widgetInfo.resources.length > 0) {
      widgetInfo.resources.filter(r => r.isModule).forEach(
        (resource) => {
          modulesTasks.push(
            this.resources.loadModules(resource.url, this.modulesMap).pipe(
              catchError((e: Error) => of(e?.message ? e.message : `Failed to load widget resource module: '${resource.url}'`))
            )
          );
        }
      );
    }
    widgetInfo.resources.filter(r => !r.isModule).forEach(
      (resource) => {
        resourceTasks.push(
          this.resources.loadResource(resource.url).pipe(
            catchError(e => of(`Failed to load widget resource: '${resource.url}'`))
          )
        );
      }
    );

    let modulesObservable: Observable<string | Type<any>[]>;
    if (modulesTasks.length) {
      modulesObservable = forkJoin(modulesTasks).pipe(
        map(res => {
          const msg = res.find(r => typeof r === 'string');
          if (msg) {
            return msg as string;
          } else {
            let resModules = (res as Type<any>[][]).flat();
            if (modules && modules.length) {
              resModules = resModules.concat(modules);
            }
            return resModules;
          }
        })
      );
    } else {
      modulesObservable = modules && modules.length ? of(modules) : of([]);
    }

    resourceTasks.push(
      modulesObservable.pipe(
        mergeMap((resolvedModules) => {
          if (typeof resolvedModules === 'string') {
            return of(resolvedModules);
          } else {
            return this.dynamicComponentFactoryService.createDynamicComponentFactory(
              class DynamicWidgetComponentInstance extends DynamicWidgetComponent {},
              widgetInfo.templateHtml,
              resolvedModules
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
            );
          }
        }))
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
      if (isUndefined(result.typeParameters.hasDataPageLink)) {
        result.typeParameters.hasDataPageLink = false;
      }
      if (isUndefined(result.typeParameters.maxDatasources)) {
        result.typeParameters.maxDatasources = -1;
      }
      if (isUndefined(result.typeParameters.maxDataKeys)) {
        result.typeParameters.maxDataKeys = -1;
      }
      if (isUndefined(result.typeParameters.singleEntity)) {
        result.typeParameters.singleEntity = false;
      }
      if (isUndefined(result.typeParameters.warnOnPageDataOverflow)) {
        result.typeParameters.warnOnPageDataOverflow = true;
      }
      if (isUndefined(result.typeParameters.ignoreDataUpdateOnIntervalTick)) {
        result.typeParameters.ignoreDataUpdateOnIntervalTick = false;
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

  private deleteWidgetInfoFromCache(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean) {
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
    this.widgetsInfoInMemoryCache.delete(key);
  }

  private deleteWidgetsBundleFromCache(bundleAlias: string, isSystem: boolean) {
    const key = (isSystem ? 'sys_' : '') + bundleAlias;
    this.widgetsInfoInMemoryCache.forEach((widgetInfo, cacheKey) => {
      if (cacheKey.startsWith(key)) {
        this.widgetsInfoInMemoryCache.delete(cacheKey);
      }
    });
  }
}

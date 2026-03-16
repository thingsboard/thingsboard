///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import {
  componentTypeBySelector,
  flatModulesWithComponents,
  ModulesWithComponents,
  modulesWithComponentsToTypes,
  ResourcesService
} from '@core/services/resources.service';
import {
  IWidgetSettingsComponent,
  migrateWidgetTypeToDynamicForms,
  Widget,
  widgetActionSources,
  WidgetActionType,
  WidgetControllerDescriptor,
  WidgetType
} from '@shared/models/widget.models';
import { catchError, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { isDefinedAndNotNull, isFunction, isUndefined } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { DynamicWidgetComponent } from '@home/components/widget/dynamic-widget.component';
import { WidgetComponentsModule } from '@home/components/widget/widget-components.module';
import { WINDOW } from '@core/services/window.service';

import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetTypeId } from '@app/shared/models/id/widget-type-id';
import { TenantId } from '@app/shared/models/id/tenant-id';
import { SharedModule } from '@shared/shared.module';
import { MODULES_MAP } from '@shared/public-api';
import tinycolor from 'tinycolor2';
import moment from 'moment';
import { IModulesMap } from '@modules/common/modules-map.models';
import { HOME_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { IBasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { compileTbFunction, TbFunction } from '@shared/models/js-function.models';
import { HttpClient } from '@angular/common/http';
import { jsonFormSchemaToFormProperties } from '@shared/models/dynamic-form.models';

@Injectable()
export class WidgetComponentService {

  private cssParser = new cssjs();

  private widgetsInfoFetchQueue = new Map<string, Array<Subject<WidgetInfo>>>();

  private init$: Observable<any>;

  private missingWidgetType: WidgetInfo;
  private errorWidgetType: WidgetInfo;
  private editingWidgetType: WidgetType;

  constructor(@Inject(WINDOW) private window: Window,
              @Optional() @Inject(MODULES_MAP) private modulesMap: IModulesMap,
              @Inject(HOME_COMPONENTS_MODULE_TOKEN) private homeComponentsModule: Type<any>,
              private dynamicComponentFactoryService: DynamicComponentFactoryService,
              private widgetService: WidgetService,
              private utils: UtilsService,
              private resources: ResourcesService,
              private translate: TranslateService,
              private http: HttpClient) {

    this.cssParser.testMode = false;

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
            fullFqn: 'system.customWidget',
            deprecated: false,
            scada: false,
            type: this.utils.editWidgetInfo.type,
            sizeX: this.utils.editWidgetInfo.sizeX,
            sizeY: this.utils.editWidgetInfo.sizeY,
            resources: this.utils.editWidgetInfo.resources,
            templateHtml: this.utils.editWidgetInfo.templateHtml,
            templateCss: this.utils.editWidgetInfo.templateCss,
            controllerScript: this.utils.editWidgetInfo.controllerScript,
            settingsForm: this.utils.editWidgetInfo.settingsForm,
            dataKeySettingsForm: this.utils.editWidgetInfo.dataKeySettingsForm,
            latestDataKeySettingsForm: this.utils.editWidgetInfo.latestDataKeySettingsForm,
            settingsDirective: this.utils.editWidgetInfo.settingsDirective,
            dataKeySettingsDirective: this.utils.editWidgetInfo.dataKeySettingsDirective,
            latestDataKeySettingsDirective: this.utils.editWidgetInfo.latestDataKeySettingsDirective,
            hasBasicMode: this.utils.editWidgetInfo.hasBasicMode,
            basicModeDirective: this.utils.editWidgetInfo.basicModeDirective,
            defaultConfig: this.utils.editWidgetInfo.defaultConfig
          }, new WidgetTypeId('1'), new TenantId( NULL_UUID ), undefined, undefined
        );
      }
      const initSubject = new ReplaySubject<void>();
      this.init$ = initSubject.asObservable();

      const w = (this.window as any);

      w.tinycolor = tinycolor;
      w.cssjs = cssjs;
      w.moment = moment;

      const widgetModulesTasks: Observable<any>[] = [];
      widgetModulesTasks.push(from(import('jquery.terminal')).pipe(
        tap((mod) => {
          mod.default(window, $);
        })
      ));

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
      widgetModulesTasks.push(from(import('@home/components/widget/lib/chart/time-series-chart')).pipe(
        tap((mod) => {
          (window as any).TbTimeSeriesChart = mod.TbTimeSeriesChart;
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
      widgetModulesTasks.push(from(import('@home/components/widget/lib/maps-legacy/map-widget2')).pipe(
        tap((mod) => {
          (window as any).TbMapWidgetV2 = mod.TbMapWidgetV2;
        }))
      );
      widgetModulesTasks.push(from(import('@home/components/widget/lib/trip-animation/trip-animation.component')).pipe(
        tap((mod) => {
          (window as any).TbTripAnimationWidget = mod.TbTripAnimationWidget;
        }))
      );

      forkJoin(widgetModulesTasks).subscribe(
        () => {
          const loadDefaultWidgetInfoTasks = [
            this.loadWidgetResources(this.missingWidgetType, 'global-widget-missing-type',
              [SharedModule, WidgetComponentsModule, this.homeComponentsModule]),
            this.loadWidgetResources(this.errorWidgetType, 'global-widget-error-type',
              [SharedModule, WidgetComponentsModule, this.homeComponentsModule]),
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
    const widgetInfo = this.widgetService.getWidgetInfoFromCache(widget.typeFullFqn);
    if (widgetInfo) {
      return widgetInfo;
    } else {
      return {
        typeParameters: {
          hideDataTab: true
        }
      } as WidgetInfo;
    }
  }

  public getWidgetInfo(fullFqn: string): Observable<WidgetInfo> {
    return this.init().pipe(
      mergeMap(() => this.getWidgetInfoInternal(fullFqn))
    );
  }

  public clearWidgetInfo(widgetInfo: WidgetInfo): void {
    this.dynamicComponentFactoryService.destroyDynamicComponent(widgetInfo.componentType);
    this.widgetService.deleteWidgetInfoFromCache(widgetInfo.fullFqn);
  }

  private getWidgetInfoInternal(fullFqn: string): Observable<WidgetInfo> {
    const widgetInfoSubject = new ReplaySubject<WidgetInfo>();
    const widgetInfo = this.widgetService.getWidgetInfoFromCache(fullFqn);
    if (widgetInfo) {
      widgetInfoSubject.next(widgetInfo);
      widgetInfoSubject.complete();
    } else {
      if (this.utils.widgetEditMode) {
        this.loadWidget(this.editingWidgetType, widgetInfoSubject);
      } else {
        let fetchQueue = this.widgetsInfoFetchQueue.get(fullFqn);
        if (fetchQueue) {
          fetchQueue.push(widgetInfoSubject);
        } else {
          fetchQueue = new Array<Subject<WidgetInfo>>();
          this.widgetsInfoFetchQueue.set(fullFqn, fetchQueue);
          this.widgetService.getWidgetType(fullFqn, {ignoreErrors: true}).subscribe(
            (widgetType) => {
              widgetType = migrateWidgetTypeToDynamicForms(widgetType);
              this.loadWidget(widgetType, widgetInfoSubject);
            },
            () => {
              widgetInfoSubject.next(this.missingWidgetType);
              widgetInfoSubject.complete();
              this.resolveWidgetsInfoFetchQueue(fullFqn, this.missingWidgetType);
            }
          );
        }
      }
    }
    return widgetInfoSubject.asObservable();
  }

  private loadWidget(widgetType: WidgetType, widgetInfoSubject: Subject<WidgetInfo>) {
    const widgetInfo = toWidgetInfo(widgetType);
    this.createWidgetControllerDescriptor(widgetInfo).subscribe(
      {
        next: widgetControllerDescriptor => {
          const widgetNamespace = `widget-type-${widgetInfo.fullFqn.replace(/\./g, '-')}`;
          this.loadWidgetResources(widgetInfo, widgetNamespace, [SharedModule, WidgetComponentsModule, this.homeComponentsModule]).subscribe(
            {
              next: () => {
                if (widgetControllerDescriptor.settingsForm) {
                  widgetInfo.typeSettingsForm = widgetControllerDescriptor.settingsForm;
                }
                if (widgetControllerDescriptor.dataKeySettingsForm) {
                  widgetInfo.typeDataKeySettingsForm = widgetControllerDescriptor.dataKeySettingsForm;
                }
                if (widgetControllerDescriptor.latestDataKeySettingsForm) {
                  widgetInfo.typeLatestDataKeySettingsForm = widgetControllerDescriptor.latestDataKeySettingsForm;
                }
                widgetInfo.typeParameters = widgetControllerDescriptor.typeParameters;
                widgetInfo.actionSources = widgetControllerDescriptor.actionSources;
                widgetInfo.widgetTypeFunction = widgetControllerDescriptor.widgetTypeFunction;
                this.widgetService.putWidgetInfoToCache(widgetInfo);
                if (widgetInfoSubject) {
                  widgetInfoSubject.next(widgetInfo);
                  widgetInfoSubject.complete();
                }
                this.resolveWidgetsInfoFetchQueue(widgetInfo.fullFqn, widgetInfo);
              },
              error: (errorMessages: string[]) => {
                this.processWidgetLoadError(errorMessages, widgetInfo.fullFqn, widgetInfoSubject);
              }
            }
          );
        },
        error: e => {
          const details = this.utils.parseException(e);
          const errorMessage = `Failed to compile widget script. \n Error: ${details.message}`;
          this.processWidgetLoadError([errorMessage], widgetInfo.fullFqn, widgetInfoSubject);
        }
      }
    );
  }

  private loadWidgetResources(widgetInfo: WidgetInfo, widgetNamespace: string, modules?: Type<any>[]): Observable<any> {
    this.cssParser.cssPreviewNamespace = widgetNamespace;
    this.cssParser.createStyleElement(widgetNamespace, widgetInfo.templateCss);
    const resourceTasks: Observable<string>[] = [];
    const modulesTasks: Observable<ModulesWithComponents | string>[] = [];
    if (widgetInfo.resources.length > 0) {
      widgetInfo.resources.filter(r => r.isModule).forEach(
        (resource) => {
          modulesTasks.push(
            this.resources.loadModulesWithComponents(resource.url, this.modulesMap).pipe(
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
            catchError(() => of(`Failed to load widget resource: '${resource.url}'`))
          )
        );
      }
    );

    let modulesObservable: Observable<string | ModulesWithComponents>;
    if (modulesTasks.length) {
      modulesObservable = forkJoin(modulesTasks).pipe(
        map(res => {
          const msg = res.find(r => typeof r === 'string');
          if (msg) {
            return msg as string;
          } else {
            const modulesWithComponentsList = res as ModulesWithComponents[];
            return flatModulesWithComponents(modulesWithComponentsList);
          }
        })
      );
    } else {
      modulesObservable = of({modules: [], standaloneComponents: []});
    }

    resourceTasks.push(
      modulesObservable.pipe(
        mergeMap((resolvedModules) => {
          if (typeof resolvedModules === 'string') {
            return of(resolvedModules);
          } else {
            this.registerWidgetSettingsForms(widgetInfo, resolvedModules);
            let imports = modulesWithComponentsToTypes(resolvedModules);
            if (modules && modules.length) {
              imports = imports.concat(modules);
            }
            return this.dynamicComponentFactoryService.createDynamicComponent(
              class DynamicWidgetComponentInstance extends DynamicWidgetComponent {},
              widgetInfo.templateHtml,
              imports
            ).pipe(
              map((componentType) => {
                widgetInfo.componentType = componentType;
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
            return throwError(() => errors);
          } else {
            return of(null);
          }
        }
    ));
  }

  private registerWidgetSettingsForms(widgetInfo: WidgetInfo, modulesWithComponents: ModulesWithComponents) {
    const directives: string[] = [];
    const basicDirectives: string[] = [];
    if (widgetInfo.settingsDirective && widgetInfo.settingsDirective.length) {
      directives.push(widgetInfo.settingsDirective);
    }
    if (widgetInfo.dataKeySettingsDirective && widgetInfo.dataKeySettingsDirective.length) {
      directives.push(widgetInfo.dataKeySettingsDirective);
    }
    if (widgetInfo.latestDataKeySettingsDirective && widgetInfo.latestDataKeySettingsDirective.length) {
      directives.push(widgetInfo.latestDataKeySettingsDirective);
    }
    if (widgetInfo.basicModeDirective && widgetInfo.basicModeDirective.length) {
      basicDirectives.push(widgetInfo.basicModeDirective);
    }

    this.expandSettingComponentMap(this.widgetService.putWidgetSettingsComponentToMap.bind(this.widgetService), directives, modulesWithComponents);
    this.expandSettingComponentMap(this.widgetService.putBasicWidgetSettingsComponentToMap.bind(this.widgetService), basicDirectives, modulesWithComponents);
  }

  private expandSettingComponentMap(putComponentToMap: (selector: string, comp: Type<IWidgetSettingsComponent | IBasicWidgetConfigComponent>) => void,
                                    directives: string[], modulesWithComponents: ModulesWithComponents): void {
    if (directives.length) {
      directives.forEach(selector => {
        const compType = componentTypeBySelector(modulesWithComponents, selector);
        if (compType) {
          putComponentToMap(selector, compType);
        }
      });
    }
  }

  private createWidgetControllerDescriptor(widgetInfo: WidgetInfo): Observable<WidgetControllerDescriptor> {
    let controllerBody: string;
    let modules: {[alias: string]: string} = null;
    if (typeof widgetInfo.controllerScript === 'string') {
      controllerBody = widgetInfo.controllerScript;
    } else {
      controllerBody = widgetInfo.controllerScript.body;
      modules = widgetInfo.controllerScript.modules;
    }
    let widgetTypeFunctionBody = `return function _${widgetInfo.fullFqn.replace(/\./g, '_')} (ctx) {\n` +
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

         '    self.getSettingsForm = function() {\n\n' +
                return [
                  {
                    'id': 'testProp',
                    'name': 'Test property',
                    'type': 'text',
                    'default': 'Default value'
                  }
                ];
         '    }\n\n' +

         '    self.getDataKeySettingsForm = function() {\n\n' +
                return [];
         '    }\n\n' +

         '    self.getLatestDataKeySettingsForm = function() {\n\n' +
                return [];
         '    }\n\n' +

         '    self.onDestroy = function() {\n\n' +

         '    }\n\n' +
         '}';*/

    widgetTypeFunctionBody += controllerBody;
    widgetTypeFunctionBody += '\n};\n';

    let tbWidgetTypeFunction: TbFunction;
    if (modules && Object.keys(modules).length) {
      tbWidgetTypeFunction = {
        body: widgetTypeFunctionBody,
        modules
      }
    } else {
      tbWidgetTypeFunction = widgetTypeFunctionBody;
    }

    return compileTbFunction(this.http, tbWidgetTypeFunction).pipe(
      map((compiled) => {
        const widgetType = compiled.apply(this);
        const widgetTypeInstance: WidgetTypeInstance = new widgetType();
        const result: WidgetControllerDescriptor = {
          widgetTypeFunction: widgetType
        };
        if (isFunction(widgetTypeInstance.getSettingsForm)) {
          result.settingsForm = widgetTypeInstance.getSettingsForm();
        }
        if (isFunction(widgetTypeInstance.getDataKeySettingsForm)) {
          result.dataKeySettingsForm = widgetTypeInstance.getDataKeySettingsForm();
        }
        if (isFunction(widgetTypeInstance.getLatestDataKeySettingsForm)) {
          result.latestDataKeySettingsForm = widgetTypeInstance.getLatestDataKeySettingsForm();
        }

        /** Start migrate from old JSON Schema Form **/

        if (isFunction((widgetTypeInstance as any).getSettingsSchema) && !result.settingsForm?.length) {
          const settingsSchema = (widgetTypeInstance as any).getSettingsSchema();
          result.settingsForm = jsonFormSchemaToFormProperties(settingsSchema);
        }
        if (isFunction((widgetTypeInstance as any).getDataKeySettingsSchema) && !result.dataKeySettingsForm?.length) {
          const dataKeySettingsSchema = (widgetTypeInstance as any).getDataKeySettingsSchema();
          result.dataKeySettingsForm = jsonFormSchemaToFormProperties(dataKeySettingsSchema);
        }
        if (isFunction((widgetTypeInstance as any).getLatestDataKeySettingsSchema) && !result.latestDataKeySettingsForm?.length) {
          const latestDataKeySettingsSchema = (widgetTypeInstance as any).getLatestDataKeySettingsSchema();
          result.latestDataKeySettingsForm = jsonFormSchemaToFormProperties(latestDataKeySettingsSchema);
        }

        /** End migrate from old JSON Schema Form **/

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
        if (isUndefined(result.typeParameters.hasAdditionalLatestDataKeys)) {
          result.typeParameters.hasAdditionalLatestDataKeys = false;
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
        if (isUndefined(result.typeParameters.datasourcesOptional)) {
          result.typeParameters.datasourcesOptional = false;
        }
        if (isUndefined(result.typeParameters.stateData)) {
          result.typeParameters.stateData = false;
        }
        if (isUndefined(result.typeParameters.processNoDataByWidget)) {
          result.typeParameters.processNoDataByWidget = false;
        }
        if (isUndefined(result.typeParameters.previewWidth)) {
          result.typeParameters.previewWidth = '100%';
        }
        if (isUndefined(result.typeParameters.previewHeight)) {
          result.typeParameters.previewHeight = '70%';
        }
        if (isUndefined(result.typeParameters.embedTitlePanel)) {
          result.typeParameters.embedTitlePanel = false;
        }
        if (isUndefined(result.typeParameters.embedActionsPanel)) {
          result.typeParameters.embedActionsPanel = false;
        }
        if (isUndefined(result.typeParameters.overflowVisible)) {
          result.typeParameters.overflowVisible = false;
        }
        if (isUndefined(result.typeParameters.hideDataTab)) {
          result.typeParameters.hideDataTab = false;
        }
        if (isUndefined(result.typeParameters.hideDataSettings)) {
          result.typeParameters.hideDataSettings = false;
        }
        if (!isFunction(result.typeParameters.defaultDataKeysFunction)) {
          result.typeParameters.defaultDataKeysFunction = null;
        }
        if (!isFunction(result.typeParameters.defaultLatestDataKeysFunction)) {
          result.typeParameters.defaultLatestDataKeysFunction = null;
        }
        if (!isFunction(result.typeParameters.dataKeySettingsFunction)) {
          result.typeParameters.dataKeySettingsFunction = null;
        }
        if (isUndefined(result.typeParameters.displayRpcMessageToast)) {
          result.typeParameters.displayRpcMessageToast = true;
        }
        if (isUndefined(result.typeParameters.targetDeviceOptional)) {
          result.typeParameters.targetDeviceOptional = false;
        }
        if (isUndefined(result.typeParameters.supportsUnitConversion)) {
          result.typeParameters.supportsUnitConversion = false;
        }
        if (isDefinedAndNotNull(result.typeParameters.additionalWidgetActionTypes)) {
          if (Array.isArray(result.typeParameters.additionalWidgetActionTypes)) {
            result.typeParameters.additionalWidgetActionTypes = result.typeParameters.additionalWidgetActionTypes.filter(type => WidgetActionType[type]);
          } else {
            result.typeParameters.additionalWidgetActionTypes = null;
          }
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
      }),
      catchError((e) => {
        this.utils.processWidgetException(e);
        return throwError(() => e);
      })
    );
  }

  private processWidgetLoadError(errorMessages: string[], fullFqn: string, widgetInfoSubject: Subject<WidgetInfo>) {
    if (widgetInfoSubject) {
      widgetInfoSubject.error({
        widgetInfo: this.errorWidgetType,
        errorMessages
      });
    }
    this.resolveWidgetsInfoFetchQueue(fullFqn, this.errorWidgetType, errorMessages);
  }

  private resolveWidgetsInfoFetchQueue(fullFqn: string, widgetInfo: WidgetInfo, errorMessages?: string[]) {
    const fetchQueue = this.widgetsInfoFetchQueue.get(fullFqn);
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
      this.widgetsInfoFetchQueue.delete(fullFqn);
    }
  }
}

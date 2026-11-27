// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Inject, Injectable, Optional, Type } from '@angular/core';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { WidgetService } from '@core/http/widget.service';
import { defer, forkJoin, Observable, of, ReplaySubject, Subject, throwError } from 'rxjs';
import {
  ErrorWidgetType, isWidgetWithInfo,
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
  WidgetConfig,
  WidgetActionType,
  WidgetControllerDescriptor,
  WidgetType
} from '@shared/models/widget.models';
import { catchError, map, mergeMap, shareReplay, switchMap } from 'rxjs/operators';
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
import { DashboardReportService } from '@core/http/dashboard-report.service';
import { IBasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { compileTbFunction, TbFunction } from '@shared/models/js-function.models';
import { HttpClient } from '@angular/common/http';
import { jsonFormSchemaToFormProperties } from '@shared/models/dynamic-form.models';

// Libraries that legacy widgets use as window globals, loaded for a widget whose controller script or template matches `test`.
interface WidgetGlobalModule {
  globals: string[];
  test: RegExp;
  load: () => Promise<{[name: string]: unknown}>;
}

const loadFlot = async (): Promise<{[name: string]: unknown}> => {
  await import('flot/src/jquery.flot.js');
  await Promise.all([
    import('flot/lib/jquery.colorhelpers.js'),
    import('flot/src/plugins/jquery.flot.time.js'),
    import('flot/src/plugins/jquery.flot.selection.js'),
    import('flot/src/plugins/jquery.flot.pie.js'),
    import('flot/src/plugins/jquery.flot.crosshair.js'),
    import('flot/src/plugins/jquery.flot.stack.js'),
    import('flot/src/plugins/jquery.flot.symbol.js'),
    import('flot.curvedlines/curvedLines.js')
  ]);
  const mod = await import('@home/components/widget/lib/flot-widget');
  return {TbFlot: mod.TbFlot};
};

// A computed access to the global scope, which can reach any of the libraries without naming it.
const DYNAMIC_GLOBAL_ACCESS = /\b(?:window|globalThis)\s*\[|\beval\s*\(|\bFunction\s*\(/;

const WIDGET_GLOBAL_MODULES: WidgetGlobalModule[] = [
  {
    globals: [],
    test: /\.terminal\b/,
    load: () => import('jquery.terminal').then(mod => {
      mod.default(window, $);
      return {};
    })
  },
  {
    // tb-flot-widget is an Angular component that calls $.plot, so it needs the jQuery plugins as well.
    globals: ['TbFlot'],
    test: /\bTbFlot\b|\.plot\b|tb-flot-widget/,
    load: loadFlot
  },
  {
    globals: ['TbTimeSeriesChart'],
    test: /\bTbTimeSeriesChart\b/,
    load: () => import('@home/components/widget/lib/chart/time-series-chart')
      .then(m => ({TbTimeSeriesChart: m.TbTimeSeriesChart}))
  },
  {
    globals: ['TbAnalogueCompass'],
    test: /\bTbAnalogueCompass\b/,
    load: () => import('@home/components/widget/lib/analogue-compass')
      .then(m => ({TbAnalogueCompass: m.TbAnalogueCompass}))
  },
  {
    globals: ['TbAnalogueRadialGauge'],
    test: /\bTbAnalogueRadialGauge\b/,
    load: () => import('@home/components/widget/lib/analogue-radial-gauge')
      .then(m => ({TbAnalogueRadialGauge: m.TbAnalogueRadialGauge}))
  },
  {
    globals: ['TbAnalogueLinearGauge'],
    test: /\bTbAnalogueLinearGauge\b/,
    load: () => import('@home/components/widget/lib/analogue-linear-gauge')
      .then(m => ({TbAnalogueLinearGauge: m.TbAnalogueLinearGauge}))
  },
  {
    globals: ['TbCanvasDigitalGauge'],
    test: /\bTbCanvasDigitalGauge\b/,
    load: () => import('@home/components/widget/lib/digital-gauge')
      .then(m => ({TbCanvasDigitalGauge: m.TbCanvasDigitalGauge}))
  },
  {
    // Leaflet sets the global `L` itself when it is imported, and the legacy map imports it with the plugins that code
    // using `L` got before. `L` gets no placeholder: Leaflet reads `window.L` on import, also when the new map loads it.
    globals: ['TbMapWidgetV2'],
    test: /\bTbMapWidgetV2\b|(?<![\w$.])L\s*[.[]/,
    load: () => import('@home/components/widget/lib/maps-legacy/map-widget2')
      .then(m => ({TbMapWidgetV2: m.TbMapWidgetV2}))
  },
  {
    globals: ['TbTripAnimationWidget'],
    test: /\bTbTripAnimationWidget\b/,
    load: () => import('@home/components/widget/lib/trip-animation/trip-animation.component')
      .then(m => ({TbTripAnimationWidget: m.TbTripAnimationWidget}))
  }
];

// Collects the strings of a widget config into `sources` and tells whether it has a function with JS modules,
// whose code cannot be scanned.
const collectConfigCode = (value: unknown, sources: string[]): boolean => {
  if (typeof value === 'string') {
    sources.push(value);
    return false;
  }
  if (!value || typeof value !== 'object') {
    return false;
  }
  let withModules = false;
  if (!Array.isArray(value)) {
    const modules = (value as {modules?: unknown}).modules;
    withModules = typeof (value as {body?: unknown}).body === 'string'
      && !!modules && typeof modules === 'object' && Object.keys(modules).length > 0;
  }
  for (const item of Object.values(value)) {
    withModules = collectConfigCode(item, sources) || withModules;
  }
  return withModules;
};

@Injectable()
export class WidgetComponentService {

  private cssParser = new cssjs();

  private widgetsInfoFetchQueue = new Map<string, Array<Subject<WidgetInfo>>>();

  private init$: Observable<any>;

  private widgetGlobalModulesLoad = new Map<WidgetGlobalModule, Observable<void>>();

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
              private http: HttpClient,
              private reportService: DashboardReportService) {

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

      this.installWidgetGlobalPlaceholders();

      const loadDefaultWidgetInfoTasks = [
        this.loadWidgetResources(this.missingWidgetType, 'global-widget-missing-type',
          [SharedModule, WidgetComponentsModule, this.homeComponentsModule]),
        this.loadWidgetResources(this.errorWidgetType, 'global-widget-error-type',
          [SharedModule, WidgetComponentsModule, this.homeComponentsModule]),
      ];
      forkJoin(loadDefaultWidgetInfoTasks).subscribe(
        () => {
          if (this.reportService.reportView) {
            this.reportService.openReportSubject.subscribe(() => this.widgetService.clearWidgetInfoInMemoryCache());
          }
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
      return this.init$;
    }
  }

  public getInstantWidgetInfo(widget: Widget): WidgetInfo {
    if (isWidgetWithInfo(widget)) {
      return widget.widgetInfo;
    }
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
    this.loadWidgetGlobalModules(widgetInfo).subscribe({
      next: () => this.compileWidget(widgetInfo, widgetInfoSubject),
      error: e => {
        const details = this.utils.parseException(e);
        const errorMessage = `Failed to load widget modules. \n Error: ${details.message}`;
        this.processWidgetLoadError([errorMessage], widgetInfo.fullFqn, widgetInfoSubject);
      }
    });
  }

  private loadWidgetGlobalModules(widgetInfo: WidgetInfo): Observable<void> {
    const controllerScript = widgetInfo.controllerScript;
    const source = (typeof controllerScript === 'string' ? controllerScript : controllerScript?.body ?? '') +
      '\n' + (widgetInfo.templateHtml ?? '');
    // Widget resources cannot be scanned, so such widgets get every library.
    return this.loadGlobalModulesForSource(source, !!widgetInfo.resources?.length);
  }

  // The code of a widget on a dashboard (actions, settings functions, cell functions and the like) is in its config,
  // not in its type: it is scanned the same way before the widget is created.
  public loadWidgetConfigGlobalModules(config: WidgetConfig): Observable<void> {
    const sources: string[] = [];
    const withModules = collectConfigCode(config, sources);
    return this.loadGlobalModulesForSource(sources.join('\n'), withModules).pipe(
      catchError(e => {
        const details = this.utils.parseException(e);
        return throwError(() => ({
          widgetInfo: this.errorWidgetType,
          errorMessages: [`Failed to load widget modules. \n Error: ${details.message}`]
        }));
      })
    );
  }

  private loadGlobalModulesForSource(source: string, loadAll: boolean): Observable<void> {
    // A script that reaches globals by a computed name cannot be matched, so it gets every library as well.
    const modules = loadAll || DYNAMIC_GLOBAL_ACCESS.test(source)
      ? WIDGET_GLOBAL_MODULES : WIDGET_GLOBAL_MODULES.filter(m => m.test.test(source));
    const tasks = modules.map(m => this.loadWidgetGlobalModule(m));
    return tasks.length ? forkJoin(tasks).pipe(map(() => undefined)) : of(undefined);
  }

  private loadWidgetGlobalModule(module: WidgetGlobalModule): Observable<void> {
    let load$ = this.widgetGlobalModulesLoad.get(module);
    if (!load$) {
      load$ = defer(() => module.load()).pipe(
        map(globals => {
          Object.keys(globals).forEach(name => this.setWidgetGlobal(name, globals[name]));
        }),
        catchError(e => {
          this.widgetGlobalModulesLoad.delete(module);
          return throwError(() => e);
        }),
        shareReplay(1)
      );
      this.widgetGlobalModulesLoad.set(module, load$);
    }
    return load$;
  }

  private setWidgetGlobal(name: string, value: unknown) {
    Object.defineProperty(this.window, name, {value, writable: true, configurable: true, enumerable: true});
  }

  // Last resort for code the scans do not see (e.g. an action added in edit mode before the dashboard is reloaded):
  // the first access starts loading the library, so widgets created after it has loaded can use it.
  private installWidgetGlobalPlaceholders() {
    WIDGET_GLOBAL_MODULES.forEach(module => module.globals.forEach(name => {
      if (name in this.window) {
        return;
      }
      Object.defineProperty(this.window, name, {
        configurable: true,
        enumerable: false,
        get: () => {
          // A library may read its own global while it loads; that is not a use the scans missed.
          if (!this.widgetGlobalModulesLoad.has(module)) {
            console.warn(`${name} was used before it was loaded: the code that uses it was not scanned for it. ` +
              'It is being loaded now and will be available to widgets created after it has loaded.');
            this.loadWidgetGlobalModule(module).subscribe({error: () => {}});
          }
          return undefined;
        },
        set: (value: unknown) => this.setWidgetGlobal(name, value)
      });
    }));
  }

  private compileWidget(widgetInfo: WidgetInfo, widgetInfoSubject: Subject<WidgetInfo>) {
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

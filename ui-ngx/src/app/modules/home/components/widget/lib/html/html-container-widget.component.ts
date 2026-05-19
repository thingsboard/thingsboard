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

import {
  Component,
  ElementRef,
  Inject,
  Injector,
  Input,
  OnInit,
  Optional,
  Type,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  htmlContainerDefaultSettings,
  HtmlContainerWidgetSettings,
  HtmlContainerWidgetType,
  WidgetContainerAngularFunction,
  WidgetContainerPlainFunction
} from '@home/components/widget/lib/html/html-container-widget.models';
import { hashCode, isNotEmptyStr, parseTbFunction } from '@core/utils';
import { CompiledTbFunction, isNotEmptyTbFunction } from '@shared/models/js-function.models';
import { catchError, forkJoin, map, Observable, of, switchMap, throwError } from 'rxjs';
import cssjs from '@core/css/css';
import { SHARED_MODULE_TOKEN } from '@shared/components/tokens';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { HOME_COMPONENTS_MODULE_TOKEN, WIDGET_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { ExceptionData } from '@shared/models/error.models';
import { UtilsService } from '@core/services/utils.service';
import {
  flatModulesWithComponents,
  ModulesWithComponents,
  modulesWithComponentsToTypes,
  ResourcesService
} from '@core/services/resources.service';
import { MODULES_MAP } from '@shared/models/constants';
import { IModulesMap } from '@modules/common/modules-map.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';

@Component({
  selector: 'tb-html-container-widget',
  template: '<div #container class="tb-absolute-fill"><tb-anchor #angularContainer></tb-anchor></div>' +
    '@if (widgetErrorData) { <div class="tb-absolute-fill tb-widget-error">\n' +
    '  <span [innerHtml]="(\'Widget Error:<br/><br/>\' + widgetErrorData.message) | safe:\'html\'"></span>\n' +
    '</div> }',
  styles: '.tb-widget-error {\n' +
    '    display: flex;\n' +
    '    align-items: center;\n' +
    '    justify-content: center;\n' +
    '    background: rgba(255, 255, 255, .5);\n' +
    '\n' +
    '    span {\n' +
    '      color: #f00;\n' +
    '    }\n' +
    '  }',
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class HtmlContainerWidgetComponent implements OnInit {

  @ViewChild('container', {static: true})
  containerElmRef: ElementRef<HTMLElement>;

  @ViewChild('angularContainer', {static: true})
  angularContainer: TbAnchorComponent;

  @Input()
  ctx: WidgetContext;

  private containerInstanceComponentType: Type<any>;

  private settings: HtmlContainerWidgetSettings;

  widgetErrorData: ExceptionData;

  constructor(private elementRef: ElementRef<HTMLElement>,
              @Optional() @Inject(MODULES_MAP) private modulesMap: IModulesMap,
              @Inject(SHARED_MODULE_TOKEN) private sharedModule: Type<any>,
              @Inject(WIDGET_COMPONENTS_MODULE_TOKEN) private widgetComponentsModule: Type<any>,
              @Inject(HOME_COMPONENTS_MODULE_TOKEN) private homeComponentsModule: Type<any>,
              private dynamicComponentFactoryService: DynamicComponentFactoryService,
              private utils: UtilsService,
              private resources: ResourcesService) {}

  ngOnInit(): void {
    this.settings = {...htmlContainerDefaultSettings, ...(this.ctx.settings || {})};
    this.loadWidgetResources().subscribe(
      {
        next: () => {
          if (this.settings.type === HtmlContainerWidgetType.PLAIN) {
            this.initPlain();
          } else if (this.settings.type === HtmlContainerWidgetType.ANGULAR) {
            this.initAngular();
          }
        },
        error: (e) => {
          this.handleWidgetException(e);
        }
      }
    );
  }

  private initPlain(): void {
    try {
      if (isNotEmptyStr(this.settings.css)) {
        const cssParser = new cssjs();
        cssParser.testMode = false;
        const namespace = 'html-container-' + hashCode(this.settings.css);
        cssParser.cssPreviewNamespace = namespace;
        cssParser.createStyleElement(namespace, this.settings.css);
        $(this.elementRef.nativeElement).addClass(namespace);
      }
      if (isNotEmptyStr(this.settings.html)) {
        $(this.containerElmRef.nativeElement).html(this.settings.html);
      }
      this.compileAndExecutePlainFunction();
    } catch (e) {
      this.handleWidgetException(e);
    }
  }

  private compileAndExecutePlainFunction(): void {
    if (isNotEmptyTbFunction(this.settings.js)) {
      const jsFunction: Observable<CompiledTbFunction<WidgetContainerPlainFunction>> = parseTbFunction(this.ctx.http, this.settings.js, ['ctx', 'container']);
      jsFunction.subscribe({
        next: (containerFunction) => {
          try {
            containerFunction.execute(this.ctx, this.containerElmRef.nativeElement);
          } catch (e) {
            this.handleWidgetException(e);
          }
        },
        error: (e) => {
          this.handleWidgetException(e);
        }
      });
    }
  }

  private initAngular(): void {
    this.loadAngularModules().subscribe(
      {
        next: (imports) => {
          this.compileAngularFunction().subscribe(
            {
              next: (containerFunction) => {
                try {
                  this.initAngularComponent(imports, containerFunction);
                } catch (e) {
                  this.handleWidgetException(e);
                }
              },
              error: (e) => {
                this.handleWidgetException(e);
              }
            }
          );
        },
        error: (e) => {
          this.handleWidgetException(e);
        }
      }
    );
  }

  private compileAngularFunction(): Observable<CompiledTbFunction<WidgetContainerAngularFunction>> {
    if (isNotEmptyTbFunction(this.settings.js)) {
        return parseTbFunction(this.ctx.http, this.settings.js, ['ctx']);
    } else {
      return of(null);
    }
  }

  private initAngularComponent(imports?: Type<any>[], containerFunction?: CompiledTbFunction<WidgetContainerAngularFunction>): void {
    this.angularContainer.viewContainerRef.clear();
    const destroyContainerInstanceResources = this.destroyContainerInstanceResources.bind(this);
    const template = this.settings.html || '';
    const styles: string[] = [];
    if (isNotEmptyStr(this.settings.css)) {
      styles.push(this.settings.css);
    }
    let compileModules = [this.sharedModule, this.widgetComponentsModule, this.homeComponentsModule];
    if (imports && imports.length) {
      compileModules = compileModules.concat(imports);
    }
    const self = () => this;
    this.dynamicComponentFactoryService.createDynamicComponent(
      class TbContainerInstance {
        ngOnInit(): void {
          if (containerFunction) {
            const instance = self();
            try {
              containerFunction.apply(this, [instance.ctx]);
            } catch (e) {
              instance.handleWidgetException(e);
            }
          }
        }
        ngOnDestroy(): void {
          destroyContainerInstanceResources();
        }
      },
      template,
      compileModules,
      true, styles
    ).subscribe({
      next: (componentType) => {
        this.containerInstanceComponentType = componentType;
        const injector: Injector = Injector.create({providers: [], parent: this.angularContainer.viewContainerRef.injector});
        try {
          this.angularContainer.viewContainerRef.createComponent(this.containerInstanceComponentType,
              {index: 0, injector});

        } catch (error) {
          this.handleWidgetException(error);
        }
      },
      error: (e) => {
        this.handleWidgetException(e);
      }
    });
  }

  private destroyContainerInstanceResources() {
    if (this.containerInstanceComponentType) {
      this.dynamicComponentFactoryService.destroyDynamicComponent(this.containerInstanceComponentType);
      this.containerInstanceComponentType = null;
    }
  }

  private handleWidgetException(e: any) {
    console.error(e);
    this.widgetErrorData = this.utils.processWidgetException(e);
    this.ctx.detectChanges();
  }

  private loadWidgetResources(): Observable<any> {
    const resourceTasks: Observable<string>[] = [];
    this.settings.resources.filter(r => !r.isModule).forEach(
      (resource) => {
        resourceTasks.push(
          this.resources.loadResource(resource.url).pipe(
            catchError(() => of(`Failed to load widget resource: '${resource.url}'`))
          )
        );
      }
    );
    if (resourceTasks.length) {
      return forkJoin(resourceTasks).pipe(
        switchMap(msgs => {
            let errors: string[];
            if (msgs && msgs.length) {
              errors = msgs.filter(msg => msg && msg.length > 0);
            }
            if (errors && errors.length) {
              return throwError(() => new Error(errors.join('<br/>')));
            } else {
              return of(null);
            }
          }
        ));
    } else {
      return of(null);
    }
  }

  private loadAngularModules(): Observable<Type<any>[]> {
    const modulesTasks: Observable<ModulesWithComponents | string>[] = [];
    this.settings.resources.filter(r => r.isModule).forEach(
      (resource) => {
        modulesTasks.push(
          this.resources.loadModulesWithComponents(resource.url, this.modulesMap).pipe(
            catchError((e: Error) => of(e?.message ? e.message : `Failed to load widget resource module: '${resource.url}'`))
          )
        );
      }
    );
    if (modulesTasks.length) {
      return forkJoin(modulesTasks).pipe(
        map(res => {
          const msg = res.find(r => typeof r === 'string');
          if (msg) {
            return msg as string;
          } else {
            const modulesWithComponentsList = res as ModulesWithComponents[];
            return flatModulesWithComponents(modulesWithComponentsList);
          }
        }),
        switchMap(modulesWithComponentsList => {
          if (typeof modulesWithComponentsList === 'string') {
            return throwError(() => new Error(modulesWithComponentsList));
          } else {
            const modules = modulesWithComponentsToTypes(modulesWithComponentsList);
            return of(modules);
          }
        })
      );
    } else {
      return of(null);
    }
  }
}

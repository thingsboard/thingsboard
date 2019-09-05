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

import {
  AfterViewInit,
  Component,
  ComponentFactory,
  ComponentFactoryResolver,
  ComponentRef,
  ElementRef,
  Injector,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { DashboardWidget, IDashboardComponent } from '@home/models/dashboard-component.models';
import {
  LegendConfig,
  LegendData,
  LegendPosition,
  Widget,
  WidgetActionDescriptor,
  WidgetActionType,
  WidgetInfo, WidgetResource,
  widgetType,
  WidgetTypeInstance,
  widgetActionSources
} from '@shared/models/widget.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { UtilsService } from '@core/services/utils.service';
import { DynamicWidgetComponent } from '@home/components/widget/dynamic-widget.component';
import { forkJoin, Observable, of, ReplaySubject, throwError } from 'rxjs';
import { DynamicWidgetComponentFactoryService } from '@home/components/widget/dynamic-widget-component-factory.service';
import { isDefined, objToBase64 } from '@core/utils';
import * as $ from 'jquery';
import { WidgetContext, WidgetHeaderAction } from '@home/models/widget-component.models';
import {
  EntityInfo,
  IWidgetSubscription,
  SubscriptionInfo,
  WidgetSubscriptionOptions,
  StateObject,
  StateParams,
  WidgetSubscriptionContext
} from '@core/api/widget-api.models';
import { EntityId } from '@shared/models/id/entity-id';
import { ActivatedRoute, Router, UrlSegment } from '@angular/router';
import cssjs from '@core/css/css';
import { ResourcesService } from '@core/services/resources.service';
import { catchError, switchMap } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TimeService } from '@core/services/time.service';
import { DeviceService } from '@app/core/http/device.service';
import { AlarmService } from '@app/core/http/alarm.service';
import { ExceptionData } from '@shared/models/error.models';

@Component({
  selector: 'tb-widget',
  templateUrl: './widget.component.html',
  styleUrls: ['./widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WidgetComponent extends PageComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @Input()
  isEdit: boolean;

  @Input()
  isMobile: boolean;

  @Input()
  dashboard: IDashboardComponent;

  @Input()
  dashboardWidget: DashboardWidget;

  @ViewChild('widgetContent', {read: ViewContainerRef, static: true}) widgetContentContainer: ViewContainerRef;

  widget: Widget;
  widgetInfo: WidgetInfo;
  widgetContext: WidgetContext;
  widgetType: any;
  widgetTypeInstance: WidgetTypeInstance;
  widgetErrorData: ExceptionData;

  dynamicWidgetComponentFactory: ComponentFactory<DynamicWidgetComponent>;
  dynamicWidgetComponentRef: ComponentRef<DynamicWidgetComponent>;
  dynamicWidgetComponent: DynamicWidgetComponent;

  subscriptionContext: WidgetSubscriptionContext;

  subscriptionInited = false;
  widgetSizeDetected = false;

  onResizeListener = this.onResize.bind(this);

  private cssParser = new cssjs();

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private dynamicWidgetComponentFactoryService: DynamicWidgetComponentFactoryService,
              private componentFactoryResolver: ComponentFactoryResolver,
              private elementRef: ElementRef,
              private injector: Injector,
              private widgetService: WidgetService,
              private resources: ResourcesService,
              private timeService: TimeService,
              private deviceService: DeviceService,
              private alarmService: AlarmService,
              private utils: UtilsService) {
    super(store);
  }

  ngOnInit(): void {
    this.widget = this.dashboardWidget.widget;

    const actionDescriptorsBySourceId: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = {};
    if (this.widget.config.actions) {
      for (const actionSourceId of Object.keys(this.widget.config.actions)) {
        const descriptors = this.widget.config.actions[actionSourceId];
        const actionDescriptors: Array<WidgetActionDescriptor> = [];
        descriptors.forEach((descriptor) => {
          const actionDescriptor: WidgetActionDescriptor = {...descriptor};
          actionDescriptor.displayName = this.utils.customTranslation(descriptor.name, descriptor.name);
          actionDescriptors.push(actionDescriptor);
        });
        actionDescriptorsBySourceId[actionSourceId] = actionDescriptors;
      }
    }

    this.widgetContext = this.dashboardWidget.widgetContext;
    this.widgetContext.inited = false;
    this.widgetContext.hideTitlePanel = false;
    this.widgetContext.isEdit = this.isEdit;
    this.widgetContext.isMobile = this.isMobile;
    this.widgetContext.dashboard = this.dashboard;
    this.widgetContext.widgetConfig = this.widget.config;
    this.widgetContext.settings = this.widget.config.settings;
    this.widgetContext.units = this.widget.config.units || '';
    this.widgetContext.decimals = isDefined(this.widget.config.decimals) ? this.widget.config.decimals : 2;
    this.widgetContext.subscriptions = {};
    this.widgetContext.defaultSubscription = null;
    this.widgetContext.dashboardTimewindow = this.dashboard.dashboardTimewindow;
    this.widgetContext.timewindowFunctions = {
      onUpdateTimewindow: (startTimeMs, endTimeMs, interval) => {
        if (this.widgetContext.defaultSubscription) {
          this.widgetContext.defaultSubscription.onUpdateTimewindow(startTimeMs, endTimeMs, interval);
        }
      },
      onResetTimewindow: () => {
        if (this.widgetContext.defaultSubscription) {
          this.widgetContext.defaultSubscription.onResetTimewindow();
        }
      }
    };
    this.widgetContext.subscriptionApi = {
      createSubscription: this.createSubscription.bind(this),
      createSubscriptionFromInfo: this.createSubscriptionFromInfo.bind(this),
      removeSubscription: (id) => {
        const subscription = this.widgetContext.subscriptions[id];
        if (subscription) {
          subscription.destroy();
          delete this.widgetContext.subscriptions[id];
        }
      }
    };
    this.widgetContext.controlApi = {
      sendOneWayCommand: (method, params, timeout) => {
        if (this.widgetContext.defaultSubscription) {
            return this.widgetContext.defaultSubscription.sendOneWayCommand(method, params, timeout);
        } else {
          return of(null);
        }
      },
      sendTwoWayCommand: (method, params, timeout) => {
        if (this.widgetContext.defaultSubscription) {
          return this.widgetContext.defaultSubscription.sendTwoWayCommand(method, params, timeout);
        } else {
          return of(null);
        }
      }
    };
    this.widgetContext.utils = {
      formatValue: this.formatValue
    };
    this.widgetContext.actionsApi = {
      actionDescriptorsBySourceId,
      getActionDescriptors: this.getActionDescriptors.bind(this),
      handleWidgetAction: this.handleWidgetAction.bind(this),
      elementClick: this.elementClick.bind(this)
    };
    this.widgetContext.stateController = this.dashboard.stateController;
    this.widgetContext.aliasController = this.dashboard.aliasController;

    this.widgetContext.customHeaderActions = [];
    const headerActionsDescriptors = this.getActionDescriptors(widgetActionSources.headerButton.value);
    headerActionsDescriptors.forEach((descriptor) => {
      const headerAction: WidgetHeaderAction = {
        name: descriptor.name,
        displayName: descriptor.displayName,
        icon: descriptor.icon,
        descriptor,
        onAction: $event => {
          const entityInfo = this.getActiveEntityInfo();
          const entityId = entityInfo ? entityInfo.entityId : null;
          const entityName = entityInfo ? entityInfo.entityName : null;
          this.handleWidgetAction($event, descriptor, entityId, entityName);
        }
      };
      this.widgetContext.customHeaderActions.push(headerAction);
    });


    this.subscriptionContext = {
      timeService: this.timeService,
      deviceService: this.deviceService,
      alarmService: this.alarmService,
      utils: this.utils,
      widgetUtils: this.widgetContext.utils,
      dashboardTimewindowApi: null, // TODO:
      getServerTimeDiff: null, // TODO:
      aliasController: this.dashboard.aliasController
    };

    this.widgetService.getWidgetInfo(this.widget.bundleAlias, this.widget.typeAlias, this.widget.isSystemType).subscribe(
      (widgetInfo) => {
        this.widgetInfo = widgetInfo;
        this.loadFromWidgetInfo();
      }
    );

  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {

    for (const id of Object.keys(this.widgetContext.subscriptions)) {
      const subscription = this.widgetContext.subscriptions[id];
      subscription.destroy();
    }
    this.subscriptionInited = false;
    this.widgetContext.subscriptions = {};
    if (this.widgetContext.inited) {
      this.widgetContext.inited = false;
      // TODO:
      try {
        this.widgetTypeInstance.onDestroy();
      } catch (e) {
        this.handleWidgetException(e);
      }
    }
    this.destroyDynamicWidgetComponent();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'isEdit') {
          console.log(`isEdit changed: ${this.isEdit}`);
          this.onEditModeChanged();
        } else if (propName === 'isMobile') {
          console.log(`isMobile changed: ${this.isMobile}`);
          this.onMobileModeChanged();
        }
      }
    }
  }

  private onEditModeChanged() {
    if (this.widgetContext.isEdit !== this.isEdit) {
      this.widgetContext.isEdit = this.isEdit;
      if (this.widgetContext.inited) {
        // TODO:
      }
    }
  }

  private onMobileModeChanged() {
    if (this.widgetContext.isMobile !== this.isMobile) {
      this.widgetContext.isMobile = this.isMobile;
      if (this.widgetContext.inited) {
        // TODO:
      }
    }
  }

  private onResize() {
    if (this.checkSize()) {
      if (this.widgetContext.inited) {
        // TODO:
      }
    }
  }

  private loadFromWidgetInfo() {
    const widgetNamespace = `widget-type-${(this.widget.isSystemType ? 'sys-' : '')}${this.widget.bundleAlias}-${this.widget.typeAlias}`;
    const elem = this.elementRef.nativeElement;
    elem.classList.add('tb-widget');
    elem.classList.add(widgetNamespace);
    this.widgetType = this.widgetInfo.widgetTypeFunction;

    if (!this.widgetType) {
      this.widgetTypeInstance = {};
    } else {
      try {
        this.widgetTypeInstance = new this.widgetType(this.widgetContext);
      } catch (e) {
        this.handleWidgetException(e);
        this.widgetTypeInstance = {};
      }
    }
    if (!this.widgetTypeInstance.onInit) {
      this.widgetTypeInstance.onInit = () => {};
    }
    if (!this.widgetTypeInstance.onDataUpdated) {
      this.widgetTypeInstance.onDataUpdated = () => {};
    }
    if (!this.widgetTypeInstance.onResize) {
      this.widgetTypeInstance.onResize = () => {};
    }
    if (!this.widgetTypeInstance.onEditModeChanged) {
      this.widgetTypeInstance.onEditModeChanged = () => {};
    }
    if (!this.widgetTypeInstance.onMobileModeChanged) {
      this.widgetTypeInstance.onMobileModeChanged = () => {};
    }
    if (!this.widgetTypeInstance.onDestroy) {
      this.widgetTypeInstance.onDestroy = () => {};
    }

    this.initialize();
  }

  private reInit() {
    this.ngOnDestroy();
    this.initialize();
    // TODO:
  }

  private initialize() {
    this.configureDynamicWidgetComponent().subscribe(
      () => {
        this.dynamicWidgetComponent.loadingData = false;
      },
      (error) => {
        // TODO:
      }
    );
  }

  private destroyDynamicWidgetComponent() {
    if (this.widgetContext.$containerParent) {
      // @ts-ignore
      removeResizeListener(this.widgetContext.$containerParent[0], this.onResizeListener);
    }
    if (this.dynamicWidgetComponentRef) {
      this.dynamicWidgetComponentRef.destroy();
    }
    if (this.dynamicWidgetComponentFactory) {
      this.dynamicWidgetComponentFactoryService.destroyDynamicWidgetComponentFactory(this.dynamicWidgetComponentFactory);
    }
  }

  private handleWidgetException(e) {
    console.error(e);
    this.widgetErrorData = this.utils.processWidgetException(e);
    if (this.dynamicWidgetComponent) {
      this.dynamicWidgetComponent.widgetErrorData = this.widgetErrorData;
    }
  }

  private configureDynamicWidgetComponent(): Observable<any> {

    const dynamicWidgetComponentSubject = new ReplaySubject();

    let html = '<div class="tb-absolute-fill tb-widget-error" *ngIf="widgetErrorData">' +
      '<span>Widget Error: {{ widgetErrorData.name + ": " + widgetErrorData.message}}</span>' +
      '</div>' +
      '<div class="tb-absolute-fill tb-widget-loading" [fxShow]="loadingData" fxLayout="column" fxLayoutAlign="center center">' +
      '<mat-spinner color="accent" md-mode="indeterminate" diameter="40"></mat-spinner>' +
      '</div>';

    let containerHtml = `<div id="container">${this.widgetInfo.templateHtml}</div>`;

    const displayLegend = isDefined(this.widget.config.showLegend) ? this.widget.config.showLegend
      : this.widget.type === widgetType.timeseries;

    let legendConfig: LegendConfig;
    let legendData: LegendData;
    if (displayLegend) {
      legendConfig = this.widget.config.legendConfig ||
        {
          position: LegendPosition.bottom,
          showMin: false,
          showMax: false,
          showAvg: this.widget.type === widgetType.timeseries,
          showTotal: false
        };
      legendData = {
        keys: [],
        data: []
      };
      let layoutType;
      if (legendConfig.position === LegendPosition.top ||
        legendConfig.position === LegendPosition.bottom) {
        layoutType = 'column';
      } else {
        layoutType = 'row';
      }
      let legendStyle;
      switch (legendConfig.position) {
        case LegendPosition.top:
          legendStyle = 'padding-bottom: 8px; max-height: 50%; overflow-y: auto;';
          break;
        case LegendPosition.bottom:
          legendStyle = 'padding-top: 8px; max-height: 50%; overflow-y: auto;';
          break;
        case LegendPosition.left:
          legendStyle = 'padding-right: 0px; max-width: 50%; overflow-y: auto;';
          break;
        case LegendPosition.right:
          legendStyle = 'padding-left: 0px; max-width: 50%; overflow-y: auto;';
          break;
      }

      const legendHtml = `<tb-legend style="${legendStyle}" [legendConfig]="legendConfig" [legendData]="legendData"></tb-legend>`;
      containerHtml = `<div fxFlex id="widget-container">${containerHtml}</div>`;
      html += `<div class="tb-absolute-fill" fxLayout="${layoutType}">`;
      if (legendConfig.position === LegendPosition.top ||
        legendConfig.position === LegendPosition.left) {
        html += legendHtml;
        html += containerHtml;
      } else {
        html += containerHtml;
        html += legendHtml;
      }
      html += '</div>';
    } else {
      html += containerHtml;
    }

    this.dynamicWidgetComponentFactoryService.createDynamicWidgetComponentFactory(html).subscribe(
      (componentFactory) => {
        this.dynamicWidgetComponentFactory = componentFactory;
        this.widgetContentContainer.clear();
        this.dynamicWidgetComponentRef = this.widgetContentContainer.createComponent(this.dynamicWidgetComponentFactory);
        this.dynamicWidgetComponent = this.dynamicWidgetComponentRef.instance;

        this.dynamicWidgetComponent.loadingData = true;
        this.dynamicWidgetComponent.widgetContext = this.widgetContext;
        this.dynamicWidgetComponent.widgetErrorData = this.widgetErrorData;
        this.dynamicWidgetComponent.displayLegend = displayLegend;
        this.dynamicWidgetComponent.legendConfig = legendConfig;
        this.dynamicWidgetComponent.legendData = legendData;

        this.widgetContext.$scope = this.dynamicWidgetComponent;

        const containerElement = displayLegend ? $(this.elementRef.nativeElement.querySelector('#widget-container'))
          : $(this.elementRef.nativeElement);

        this.widgetContext.$container = $('#container', containerElement);
        this.widgetContext.$containerParent = $(containerElement);

        if (this.widgetSizeDetected) {
          this.widgetContext.$container.css('height', this.widgetContext.height + 'px');
          this.widgetContext.$container.css('width', this.widgetContext.width + 'px');
        }

        // @ts-ignore
        addResizeListener(this.widgetContext.$containerParent[0], this.onResizeListener);

        dynamicWidgetComponentSubject.next();
        dynamicWidgetComponentSubject.complete();
      },
      (e) => {
        dynamicWidgetComponentSubject.error(e);
      }
    );
    return dynamicWidgetComponentSubject.asObservable();
  }

  private createSubscription(options: WidgetSubscriptionOptions, subscribe: boolean): Observable<IWidgetSubscription> {
    // TODO:
    return of(null);
  }

  private createSubscriptionFromInfo(type: widgetType, subscriptionsInfo: Array<SubscriptionInfo>,
                                     options: WidgetSubscriptionOptions, useDefaultComponents: boolean,
                                     subscribe: boolean): Observable<IWidgetSubscription> {
    // TODO:
    return of(null);
  }

  private isNumeric(value: any): boolean {
    return (value - parseFloat( value ) + 1) >= 0;
  }

  private formatValue(value: any, dec?: number, units?: string, showZeroDecimals?: boolean): string | undefined {
    if (isDefined(value) &&
      value != null && this.isNumeric(value)) {
      let formatted: string | number = Number(value);
      if (isDefined(dec)) {
        formatted = formatted.toFixed(dec);
      }
      if (!showZeroDecimals) {
        formatted = (Number(formatted) * 1);
      }
      formatted = formatted.toString();
      if (isDefined(units) && units.length > 0) {
        formatted += ' ' + units;
      }
      return formatted;
    } else {
      return value;
    }
  }

  private getActionDescriptors(actionSourceId: string): Array<WidgetActionDescriptor> {
    let result = this.widgetContext.actionsApi.actionDescriptorsBySourceId[actionSourceId];
    if (!result) {
      result = [];
    }
    return result;
  }

  private handleWidgetAction($event: Event, descriptor: WidgetActionDescriptor,
                             entityId?: EntityId, entityName?: string, additionalParams?: any): void {
    const type = descriptor.type;
    const targetEntityParamName = descriptor.stateEntityParamName;
    let targetEntityId: EntityId;
    if (descriptor.setEntityId) {
      targetEntityId = entityId;
    }
    switch (type) {
      case WidgetActionType.openDashboardState:
      case WidgetActionType.updateDashboardState:
        let targetDashboardStateId = descriptor.targetDashboardStateId;
        const params = {...this.widgetContext.stateController.getStateParams()};
        this.updateEntityParams(params, targetEntityParamName, targetEntityId, entityName);
        if (type === WidgetActionType.openDashboardState) {
          this.widgetContext.stateController.openState(targetDashboardStateId, params, descriptor.openRightLayout);
        } else {
          this.widgetContext.stateController.updateState(targetDashboardStateId, params, descriptor.openRightLayout);
        }
        break;
      case WidgetActionType.openDashboard:
        const targetDashboardId = descriptor.targetDashboardId;
        targetDashboardStateId = descriptor.targetDashboardStateId;
        const stateObject: StateObject = {};
        stateObject.params = {};
        this.updateEntityParams(stateObject.params, targetEntityParamName, targetEntityId, entityName);
        if (targetDashboardStateId) {
          stateObject.id = targetDashboardStateId;
        }
        const stateParams = {
          dashboardId: targetDashboardId,
          state: objToBase64([ stateObject ])
        };
        const state = objToBase64([ stateObject ]);
        const currentUrl = this.route.snapshot.url;
        let url;
        if (currentUrl.length > 1) {
          if (currentUrl[currentUrl.length - 2].path === 'dashboard') {
            url = `/dashboard/${targetDashboardId}?state=${state}`;
          } else {
            url = `/dashboards/${targetDashboardId}?state=${state}`;
          }
        }
        if (url) {
          const urlTree = this.router.parseUrl(url);
          this.router.navigateByUrl(url);
        }
        break;
      case WidgetActionType.custom:
        const customFunction = descriptor.customFunction;
        if (isDefined(customFunction) && customFunction.length > 0) {
          try {
            if (!additionalParams) {
              additionalParams = {};
            }
            const customActionFunction = new Function('$event', 'widgetContext', 'entityId',
              'entityName', 'additionalParams', customFunction);
            customActionFunction($event, this.widgetContext, entityId, entityName, additionalParams);
          } catch (e) {
            //
          }
        }
        break;
      case WidgetActionType.customPretty:
        const customPrettyFunction = descriptor.customFunction;
        const customHtml = descriptor.customHtml;
        const customCss = descriptor.customCss;
        const customResources = descriptor.customResources;
        const actionNamespace = `custom-action-pretty-${descriptor.name.toLowerCase()}`;
        let htmlTemplate = '';
        if (isDefined(customHtml) && customHtml.length > 0) {
          htmlTemplate = customHtml;
        }
        this.loadCustomActionResources(actionNamespace, customCss, customResources).subscribe(
          () => {
            if (isDefined(customPrettyFunction) && customPrettyFunction.length > 0) {
              try {
                if (!additionalParams) {
                  additionalParams = {};
                }
                const customActionPrettyFunction = new Function('$event', 'widgetContext', 'entityId',
                  'entityName', 'htmlTemplate', 'additionalParams', customPrettyFunction);
                customActionPrettyFunction($event, this.widgetContext, entityId, entityName, htmlTemplate, additionalParams);
              } catch (e) {
                //
              }
            }
          },
          (errorMessages: string[]) => {
            this.processResourcesLoadErrors(errorMessages);
          }
        );
        break;
    }
  }

  private elementClick($event: Event) {
    $event.stopPropagation();
    const e = ($event.target || $event.srcElement) as Element;
    if (e.id) {
      const descriptors = this.getActionDescriptors('elementClick');
      if (descriptors.length) {
        descriptors.forEach((descriptor) => {
          if (descriptor.name === e.id) {
            const entityInfo = this.getActiveEntityInfo();
            const entityId = entityInfo ? entityInfo.entityId : null;
            const entityName = entityInfo ? entityInfo.entityName : null;
            this.handleWidgetAction(event, descriptor, entityId, entityName);
          }
        });
      }
    }
  }

  private updateEntityParams(params: StateParams, targetEntityParamName?: string, targetEntityId?: EntityId, entityName?: string) {
    if (targetEntityId) {
      let targetEntityParams: StateParams;
      if (targetEntityParamName && targetEntityParamName.length) {
        targetEntityParams = params[targetEntityParamName];
        if (!targetEntityParams) {
          targetEntityParams = {};
          params[targetEntityParamName] = targetEntityParams;
          params.targetEntityParamName = targetEntityParamName;
        }
      } else {
        targetEntityParams = params;
      }
      targetEntityParams.entityId = targetEntityId;
      if (entityName) {
        targetEntityParams.entityName = entityName;
      }
    }
  }

  private loadCustomActionResources(actionNamespace: string, customCss: string, customResources: Array<WidgetResource>): Observable<any> {
    if (isDefined(customCss) && customCss.length > 0) {
      this.cssParser.cssPreviewNamespace = actionNamespace;
      this.cssParser.createStyleElement(actionNamespace, customCss, 'nonamespace');
    }
    const resourceTasks: Observable<string>[] = [];
    if (customResources.length > 0) {
      customResources.forEach((resource) => {
        resourceTasks.push(
          this.resources.loadResource(resource.url).pipe(
            catchError(e => of(`Failed to load custom action resource: '${resource.url}'`))
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

  private processResourcesLoadErrors(errorMessages: string[]) {
    let messageToShow = '';
    errorMessages.forEach(error => {
      messageToShow += `<div>${error}</div>`;
    });
    this.store.dispatch(new ActionNotificationShow({message: messageToShow, type: 'error'}));
  }

  private getActiveEntityInfo(): EntityInfo {
    let entityInfo = this.widgetContext.activeEntityInfo;
    if (!entityInfo) {
      for (const id of Object.keys(this.widgetContext.subscriptions)) {
        const subscription = this.widgetContext.subscriptions[id];
        entityInfo = subscription.getFirstEntityInfo();
        if (entityInfo) {
          break;
        }
      }
    }
    return entityInfo;
  }

  private checkSize(): boolean {
    const width = this.widgetContext.$containerParent.width();
    const height = this.widgetContext.$containerParent.height();
    let sizeChanged = false;

    if (!this.widgetContext.width || this.widgetContext.width !== width ||
      !this.widgetContext.height || this.widgetContext.height !== height) {
      if (width > 0 && height > 0) {
        this.widgetContext.$container.css('height', height + 'px');
        this.widgetContext.$container.css('width', width + 'px');
        this.widgetContext.width = width;
        this.widgetContext.height = height;
        sizeChanged = true;
        this.widgetSizeDetected = true;
      }
    }
    return sizeChanged;
  }

}

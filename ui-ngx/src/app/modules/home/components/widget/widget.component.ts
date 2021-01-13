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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentFactoryResolver,
  ComponentRef,
  ElementRef,
  Injector,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { DashboardWidget } from '@home/models/dashboard-component.models';
import {
  defaultLegendConfig,
  LegendConfig,
  LegendData,
  LegendPosition,
  Widget,
  WidgetActionDescriptor,
  widgetActionSources,
  WidgetActionType,
  WidgetComparisonSettings,
  WidgetResource,
  widgetType,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { UtilsService } from '@core/services/utils.service';
import { forkJoin, Observable, of, ReplaySubject, Subscription, throwError } from 'rxjs';
import { deepClone, isDefined, objToBase64URI } from '@core/utils';
import {
  IDynamicWidgetComponent,
  WidgetContext,
  WidgetHeaderAction,
  WidgetInfo,
  WidgetTypeInstance
} from '@home/models/widget-component.models';
import {
  IWidgetSubscription,
  StateObject,
  StateParams,
  SubscriptionEntityInfo,
  SubscriptionInfo,
  SubscriptionMessage,
  WidgetSubscriptionContext,
  WidgetSubscriptionOptions
} from '@core/api/widget-api.models';
import { EntityId } from '@shared/models/id/entity-id';
import { ActivatedRoute, Router } from '@angular/router';
import cssjs from '@core/css/css';
import { ResourcesService } from '@core/services/resources.service';
import { catchError, switchMap } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TimeService } from '@core/services/time.service';
import { DeviceService } from '@app/core/http/device.service';
import { ExceptionData } from '@shared/models/error.models';
import { WidgetComponentService } from './widget-component.service';
import { Timewindow } from '@shared/models/time/time.models';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { DashboardService } from '@core/http/dashboard.service';
import { WidgetSubscription } from '@core/api/widget-subscription';
import { EntityService } from '@core/http/entity.service';
import { ServicesMap } from '@home/models/services.map';
import { ResizeObserver } from '@juggle/resize-observer';
import { EntityDataService } from '@core/api/entity-data.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationType } from '@core/notification/notification.models';
import { AlarmDataService } from '@core/api/alarm-data.service';

@Component({
  selector: 'tb-widget',
  templateUrl: './widget.component.html',
  styleUrls: ['./widget.component.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetComponent extends PageComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @Input()
  isEdit: boolean;

  @Input()
  isMobile: boolean;

  @Input()
  dashboardWidget: DashboardWidget;

  @ViewChild('widgetContent', {read: ViewContainerRef, static: true}) widgetContentContainer: ViewContainerRef;

  widget: Widget;
  widgetInfo: WidgetInfo;
  errorMessages: string[];
  widgetContext: WidgetContext;
  widgetType: any;
  typeParameters: WidgetTypeParameters;
  widgetTypeInstance: WidgetTypeInstance;
  widgetErrorData: ExceptionData;
  loadingData: boolean;
  displayNoData = false;

  displayLegend: boolean;
  legendConfig: LegendConfig;
  legendData: LegendData;
  isLegendFirst: boolean;
  legendContainerLayoutType: string;
  legendStyle: {[klass: string]: any};

  dynamicWidgetComponentRef: ComponentRef<IDynamicWidgetComponent>;
  dynamicWidgetComponent: IDynamicWidgetComponent;

  subscriptionContext: WidgetSubscriptionContext;

  subscriptionInited = false;
  destroyed = false;
  widgetSizeDetected = false;
  widgetInstanceInited = false;
  dataUpdatePending = false;
  pendingMessage: SubscriptionMessage;

  cafs: {[cafId: string]: CancelAnimationFrame} = {};

  toastTargetId = 'widget-messages-' + this.utils.guid();

  private widgetResize$: ResizeObserver;

  private cssParser = new cssjs();

  private rxSubscriptions = new Array<Subscription>();

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private widgetComponentService: WidgetComponentService,
              private componentFactoryResolver: ComponentFactoryResolver,
              private elementRef: ElementRef,
              private injector: Injector,
              private widgetService: WidgetService,
              private resources: ResourcesService,
              private timeService: TimeService,
              private deviceService: DeviceService,
              private entityService: EntityService,
              private dashboardService: DashboardService,
              private entityDataService: EntityDataService,
              private alarmDataService: AlarmDataService,
              private translate: TranslateService,
              private utils: UtilsService,
              private raf: RafService,
              private ngZone: NgZone,
              private cd: ChangeDetectorRef) {
    super(store);
    this.cssParser.testMode = false;
  }

  ngOnInit(): void {

    this.loadingData = true;

    this.widget = this.dashboardWidget.widget;

    this.displayLegend = isDefined(this.widget.config.showLegend) ? this.widget.config.showLegend
      : this.widget.type === widgetType.timeseries;

    this.legendContainerLayoutType = 'column';

    if (this.displayLegend) {
      this.legendConfig = this.widget.config.legendConfig || defaultLegendConfig(this.widget.type);
      this.legendData = {
        keys: [],
        data: []
      };
      if (this.legendConfig.position === LegendPosition.top ||
        this.legendConfig.position === LegendPosition.bottom) {
        this.legendContainerLayoutType = 'column';
        this.isLegendFirst = this.legendConfig.position === LegendPosition.top;
      } else {
        this.legendContainerLayoutType = 'row';
        this.isLegendFirst = this.legendConfig.position === LegendPosition.left;
      }
      switch (this.legendConfig.position) {
        case LegendPosition.top:
          this.legendStyle = {
            paddingBottom: '8px',
            maxHeight: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.bottom:
          this.legendStyle = {
            paddingTop: '8px',
            maxHeight: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.left:
          this.legendStyle = {
            paddingRight: '0px',
            maxWidth: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.right:
          this.legendStyle = {
            paddingLeft: '0px',
            maxWidth: '50%',
            overflowY: 'auto'
          };
          break;
      }
    }

    const actionDescriptorsBySourceId: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = {};
    if (this.widget.config.actions) {
      for (const actionSourceId of Object.keys(this.widget.config.actions)) {
        const descriptors = this.widget.config.actions[actionSourceId];
        const actionDescriptors: Array<WidgetActionDescriptor> = [];
        descriptors.forEach((descriptor) => {
          const actionDescriptor: WidgetActionDescriptor = deepClone(descriptor);
          actionDescriptor.displayName = this.utils.customTranslation(descriptor.name, descriptor.name);
          actionDescriptors.push(actionDescriptor);
        });
        actionDescriptorsBySourceId[actionSourceId] = actionDescriptors;
      }
    }

    this.widgetContext = this.dashboardWidget.widgetContext;
    this.widgetContext.changeDetector = this.cd;
    this.widgetContext.ngZone = this.ngZone;
    this.widgetContext.store = this.store;
    this.widgetContext.servicesMap = ServicesMap;
    this.widgetContext.isEdit = this.isEdit;
    this.widgetContext.isMobile = this.isMobile;

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

    this.widgetContext.actionsApi = {
      actionDescriptorsBySourceId,
      getActionDescriptors: this.getActionDescriptors.bind(this),
      handleWidgetAction: this.handleWidgetAction.bind(this),
      elementClick: this.elementClick.bind(this),
      getActiveEntityInfo: this.getActiveEntityInfo.bind(this)
    };

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
          const entityLabel = entityInfo ? entityInfo.entityLabel : null;
          this.handleWidgetAction($event, descriptor, entityId, entityName, null, entityLabel);
        }
      };
      this.widgetContext.customHeaderActions.push(headerAction);
    });

    this.subscriptionContext = new WidgetSubscriptionContext(this.widgetContext.dashboard);
    this.subscriptionContext.timeService = this.timeService;
    this.subscriptionContext.deviceService = this.deviceService;
    this.subscriptionContext.translate = this.translate;
    this.subscriptionContext.entityDataService = this.entityDataService;
    this.subscriptionContext.alarmDataService = this.alarmDataService;
    this.subscriptionContext.utils = this.utils;
    this.subscriptionContext.raf = this.raf;
    this.subscriptionContext.widgetUtils = this.widgetContext.utils;
    this.subscriptionContext.getServerTimeDiff = this.dashboardService.getServerTimeDiff.bind(this.dashboardService);

    this.widgetComponentService.getWidgetInfo(this.widget.bundleAlias, this.widget.typeAlias, this.widget.isSystemType).subscribe(
      (widgetInfo) => {
        this.widgetInfo = widgetInfo;
        this.loadFromWidgetInfo();
      },
      (errorData) => {
        this.widgetInfo = errorData.widgetInfo;
        this.errorMessages = errorData.errorMessages;
        this.loadFromWidgetInfo();
      }
    );
    setTimeout(() => {
      this.dashboardWidget.updateWidgetParams();
    }, 0);
  }

  ngAfterViewInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'isEdit') {
          this.onEditModeChanged();
        } else if (propName === 'isMobile') {
          this.onMobileModeChanged();
        }
      }
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
    this.onDestroy();
  }

  private displayWidgetInstance(): boolean {
    if (this.widget.type !== widgetType.static) {
      for (const id of Object.keys(this.widgetContext.subscriptions)) {
        const subscription = this.widgetContext.subscriptions[id];
        if (subscription.isDataResolved()) {
          return true;
        }
      }
      return false;
    } else {
      return true;
    }
  }

  private onDestroy() {
    if (this.widgetContext) {
      const shouldDestroyWidgetInstance = this.displayWidgetInstance();
      for (const id of Object.keys(this.widgetContext.subscriptions)) {
        const subscription = this.widgetContext.subscriptions[id];
        subscription.destroy();
      }
      this.subscriptionInited = false;
      this.dataUpdatePending = false;
      this.pendingMessage = null;
      this.widgetContext.subscriptions = {};
      if (this.widgetContext.inited) {
        this.widgetContext.inited = false;
        for (const cafId of Object.keys(this.cafs)) {
          if (this.cafs[cafId]) {
            this.cafs[cafId]();
            this.cafs[cafId] = null;
          }
        }
        try {
          if (shouldDestroyWidgetInstance) {
            this.widgetTypeInstance.onDestroy();
            this.widgetInstanceInited = false;
          }
        } catch (e) {
          this.handleWidgetException(e);
        }
      }
      this.widgetContext.destroyed = true;
      this.destroyDynamicWidgetComponent();
    }
  }

  public onTimewindowChanged(timewindow: Timewindow) {
    for (const id of Object.keys(this.widgetContext.subscriptions)) {
      const subscription = this.widgetContext.subscriptions[id];
      if (!subscription.useDashboardTimewindow) {
        subscription.updateTimewindowConfig(timewindow);
      }
    }
  }

  public onLegendKeyHiddenChange(index: number) {
    for (const id of Object.keys(this.widgetContext.subscriptions)) {
      const subscription = this.widgetContext.subscriptions[id];
      subscription.updateDataVisibility(index);
    }
  }

  private loadFromWidgetInfo() {
    this.widgetContext.widgetNamespace = `widget-type-${(this.widget.isSystemType ? 'sys-' : '')}${this.widget.bundleAlias}-${this.widget.typeAlias}`;
    const elem = this.elementRef.nativeElement;
    elem.classList.add('tb-widget');
    elem.classList.add(this.widgetContext.widgetNamespace);
    this.widgetType = this.widgetInfo.widgetTypeFunction;
    this.typeParameters = this.widgetInfo.typeParameters;

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

    this.initialize().subscribe(
      () => {
        this.onInit();
      },
      (err) => {
        // console.log(err);
      }
    );
  }

  private detectChanges() {
    if (!this.destroyed) {
      try {
        this.cd.detectChanges();
      } catch (e) {
        // console.log(e);
      }
    }
  }

  private isReady(): boolean {
    return this.subscriptionInited && this.widgetSizeDetected;
  }

  private onInit(skipSizeCheck?: boolean) {
    if (!this.widgetContext.$containerParent || this.destroyed) {
      return;
    }
    if (!skipSizeCheck) {
      this.checkSize();
    }
    if (!this.widgetContext.inited && this.isReady()) {
      this.widgetContext.inited = true;
      if (this.cafs.init) {
        this.cafs.init();
        this.cafs.init = null;
      }
      this.cafs.init = this.raf.raf(() => {
        try {
          if (this.displayWidgetInstance()) {
            this.widgetTypeInstance.onInit();
            this.widgetInstanceInited = true;
            if (this.dataUpdatePending) {
              this.widgetTypeInstance.onDataUpdated();
              this.dataUpdatePending = false;
            }
            if (this.pendingMessage) {
              this.displayMessage(this.pendingMessage.severity, this.pendingMessage.message);
              this.pendingMessage = null;
            }
          } else {
            this.loadingData = false;
            this.displayNoData = true;
          }
          this.detectChanges();
        } catch (e) {
          this.handleWidgetException(e);
        }
      });
      if (!this.typeParameters.useCustomDatasources && this.widgetContext.defaultSubscription) {
        this.widgetContext.defaultSubscription.subscribe();
      }
    }
  }

  private onResize() {
    if (this.checkSize()) {
      if (this.widgetContext.inited) {
        if (this.cafs.resize) {
          this.cafs.resize();
          this.cafs.resize = null;
        }
        this.cafs.resize = this.raf.raf(() => {
          try {
            if (this.displayWidgetInstance()) {
              this.widgetTypeInstance.onResize();
            }
          } catch (e) {
            this.handleWidgetException(e);
          }
        });
      } else {
        this.onInit(true);
      }
    }
  }

  private onEditModeChanged() {
    if (this.widgetContext.isEdit !== this.isEdit) {
      this.widgetContext.isEdit = this.isEdit;
      if (this.widgetContext.inited) {
        if (this.cafs.editMode) {
          this.cafs.editMode();
          this.cafs.editMode = null;
        }
        this.cafs.editMode = this.raf.raf(() => {
          try {
            if (this.displayWidgetInstance()) {
              this.widgetTypeInstance.onEditModeChanged();
            }
          } catch (e) {
            this.handleWidgetException(e);
          }
        });
      }
    }
  }

  private onMobileModeChanged() {
    if (this.widgetContext.isMobile !== this.isMobile) {
      this.widgetContext.isMobile = this.isMobile;
      if (this.widgetContext.inited) {
        if (this.cafs.mobileMode) {
          this.cafs.mobileMode();
          this.cafs.mobileMode = null;
        }
        this.cafs.mobileMode = this.raf.raf(() => {
          try {
            if (this.displayWidgetInstance()) {
              this.widgetTypeInstance.onMobileModeChanged();
            }
          } catch (e) {
            this.handleWidgetException(e);
          }
        });
      }
    }
  }

  private reInit() {
    if (this.cafs.reinit) {
      this.cafs.reinit();
      this.cafs.reinit = null;
    }
    this.cafs.reinit = this.raf.raf(() => {
      this.ngZone.run(() => {
        this.reInitImpl();
      });
    });
  }

  private reInitImpl() {
    this.onDestroy();
    if (!this.typeParameters.useCustomDatasources) {
      this.createDefaultSubscription().subscribe(
        () => {
          if (this.destroyed) {
            this.onDestroy();
          } else {
            this.widgetContext.reset();
            this.subscriptionInited = true;
            this.configureDynamicWidgetComponent();
            this.onInit();
          }
        },
        () => {
          if (this.destroyed) {
            this.onDestroy();
          } else {
            this.widgetContext.reset();
            this.subscriptionInited = true;
            this.onInit();
          }
        }
      );
    } else {
      this.widgetContext.reset();
      this.subscriptionInited = true;
      this.configureDynamicWidgetComponent();
      this.onInit();
    }
  }

  private initialize(): Observable<any> {

    const initSubject = new ReplaySubject();

    this.rxSubscriptions.push(this.widgetContext.aliasController.entityAliasesChanged.subscribe(
      (aliasIds) => {
        let subscriptionChanged = false;
        for (const id of Object.keys(this.widgetContext.subscriptions)) {
          const subscription = this.widgetContext.subscriptions[id];
          subscriptionChanged = subscriptionChanged || subscription.onAliasesChanged(aliasIds);
        }
        if (subscriptionChanged && !this.typeParameters.useCustomDatasources) {
          this.displayNoData = false;
          this.reInit();
        }
      }
    ));

    this.rxSubscriptions.push(this.widgetContext.aliasController.filtersChanged.subscribe(
      (filterIds) => {
        let subscriptionChanged = false;
        for (const id of Object.keys(this.widgetContext.subscriptions)) {
          const subscription = this.widgetContext.subscriptions[id];
          subscriptionChanged = subscriptionChanged || subscription.onFiltersChanged(filterIds);
        }
        if (subscriptionChanged && !this.typeParameters.useCustomDatasources) {
          this.displayNoData = false;
          this.reInit();
        }
      }
    ));

    this.rxSubscriptions.push(this.widgetContext.dashboard.dashboardTimewindowChanged.subscribe(
      (dashboardTimewindow) => {
        for (const id of Object.keys(this.widgetContext.subscriptions)) {
          const subscription = this.widgetContext.subscriptions[id];
          subscription.onDashboardTimewindowChanged(dashboardTimewindow);
        }
      }
    ));
    if (!this.typeParameters.useCustomDatasources) {
      this.createDefaultSubscription().subscribe(
        () => {
          this.subscriptionInited = true;
          this.configureDynamicWidgetComponent();
          initSubject.next();
          initSubject.complete();
        },
        (err) => {
          this.subscriptionInited = true;
          initSubject.error(err);
        }
      );
    } else {
      this.loadingData = false;
      this.subscriptionInited = true;
      this.configureDynamicWidgetComponent();
      initSubject.next();
      initSubject.complete();
    }
    return initSubject.asObservable();
  }

  private destroyDynamicWidgetComponent() {
    if (this.widgetContext.$containerParent && this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    if (this.dynamicWidgetComponentRef) {
      this.dynamicWidgetComponentRef.destroy();
      this.dynamicWidgetComponentRef = null;
    }
  }

  private handleWidgetException(e) {
    console.error(e);
    this.widgetErrorData = this.utils.processWidgetException(e);
    this.detectChanges();
  }

  private displayMessage(type: NotificationType, message: string, duration?: number) {
    this.widgetContext.showToast(type, message, duration, 'bottom', 'right', this.toastTargetId);
  }

  private clearMessage() {
    this.widgetContext.hideToast(this.toastTargetId);
  }

  private configureDynamicWidgetComponent() {
      this.widgetContentContainer.clear();
      const injector: Injector = Injector.create(
        {
          providers: [
            {
              provide: 'widgetContext',
              useValue: this.widgetContext
            },
            {
              provide: 'errorMessages',
              useValue: this.errorMessages
            }
          ],
          parent: this.injector
        }
      );

      const containerElement = $(this.elementRef.nativeElement.querySelector('#widget-container'));
      this.widgetContext.$containerParent = $(containerElement);

      try {
        this.dynamicWidgetComponentRef = this.widgetContentContainer.createComponent(this.widgetInfo.componentFactory, 0, injector);
        this.cd.detectChanges();
      } catch (e) {
        console.error(e);
        if (this.dynamicWidgetComponentRef) {
          this.dynamicWidgetComponentRef.destroy();
          this.dynamicWidgetComponentRef = null;
        }
        this.widgetContentContainer.clear();
      }

      if (this.dynamicWidgetComponentRef) {
        this.dynamicWidgetComponent = this.dynamicWidgetComponentRef.instance;
        this.widgetContext.$container = $(this.dynamicWidgetComponentRef.location.nativeElement);
        this.widgetContext.$container.css('display', 'block');
        this.widgetContext.$container.css('user-select', 'none');
        this.widgetContext.$container.attr('id', 'container');
        if (this.widgetSizeDetected) {
          this.widgetContext.$container.css('height', this.widgetContext.height + 'px');
          this.widgetContext.$container.css('width', this.widgetContext.width + 'px');
        }
      }

      this.widgetResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.widgetResize$.observe(this.widgetContext.$containerParent[0]);
  }

  private createSubscription(options: WidgetSubscriptionOptions, subscribe?: boolean): Observable<IWidgetSubscription> {
    const createSubscriptionSubject = new ReplaySubject<IWidgetSubscription>();
    options.dashboardTimewindow = this.widgetContext.dashboardTimewindow;
    const subscription: IWidgetSubscription = new WidgetSubscription(this.subscriptionContext, options);
    subscription.init$.subscribe(
      () => {
        this.widgetContext.subscriptions[subscription.id] = subscription;
        if (subscribe) {
          subscription.subscribe();
        }
        createSubscriptionSubject.next(subscription);
        createSubscriptionSubject.complete();
      },
      (err) => {
        createSubscriptionSubject.error(err);
      }
    );
    return createSubscriptionSubject.asObservable();
  }

  private createSubscriptionFromInfo(type: widgetType, subscriptionsInfo: Array<SubscriptionInfo>,
                                     options: WidgetSubscriptionOptions, useDefaultComponents: boolean,
                                     subscribe: boolean): Observable<IWidgetSubscription> {
    const createSubscriptionSubject = new ReplaySubject<IWidgetSubscription>();
    options.type = type;

    if (useDefaultComponents) {
      this.defaultComponentsOptions(options);
    } else {
      if (!options.timeWindowConfig) {
        options.useDashboardTimewindow = true;
      }
    }
    if (options.type === widgetType.alarm) {
      options.alarmSource = this.entityService.createAlarmSourceFromSubscriptionInfo(subscriptionsInfo[0]);
    } else {
      options.datasources = this.entityService.createDatasourcesFromSubscriptionsInfo(subscriptionsInfo);
    }
    this.createSubscription(options, subscribe).subscribe(
      (subscription) => {
        if (useDefaultComponents) {
          this.defaultSubscriptionOptions(subscription, options);
        }
        createSubscriptionSubject.next(subscription);
        createSubscriptionSubject.complete();
      },
      (err) => {
        createSubscriptionSubject.error(err);
      }
    );
    return createSubscriptionSubject.asObservable();
  }

  private defaultComponentsOptions(options: WidgetSubscriptionOptions) {
    options.useDashboardTimewindow = isDefined(this.widget.config.useDashboardTimewindow)
          ? this.widget.config.useDashboardTimewindow : true;
    options.displayTimewindow = isDefined(this.widget.config.displayTimewindow)
      ? this.widget.config.displayTimewindow : !options.useDashboardTimewindow;
    options.timeWindowConfig = options.useDashboardTimewindow ? this.widgetContext.dashboardTimewindow : this.widget.config.timewindow;
    options.legendConfig = null;
    if (this.displayLegend) {
      options.legendConfig = this.legendConfig;
    }
    options.decimals = this.widgetContext.decimals;
    options.units = this.widgetContext.units;
    options.callbacks = {
      onDataUpdated: () => {
        try {
          if (this.displayWidgetInstance()) {
            if (this.widgetInstanceInited) {
              this.widgetTypeInstance.onDataUpdated();
            } else {
              this.dataUpdatePending = true;
            }
          }
        } catch (e){}
      },
      onDataUpdateError: (subscription, e) => {
        this.handleWidgetException(e);
      },
      onSubscriptionMessage: (subscription, message) => {
        if (this.displayWidgetInstance()) {
          if (this.widgetInstanceInited) {
            this.displayMessage(message.severity, message.message);
          } else {
            this.pendingMessage = message;
          }
        }
      },
      onInitialPageDataChanged: (subscription, nextPageData) => {
        this.reInit();
      },
      dataLoading: (subscription) => {
        if (this.loadingData !== subscription.loadingData) {
          this.loadingData = subscription.loadingData;
          this.detectChanges();
        }
      },
      legendDataUpdated: (subscription, detectChanges) => {
        if (detectChanges) {
          this.detectChanges();
        }
      },
      timeWindowUpdated: (subscription, timeWindowConfig) => {
        this.ngZone.run(() => {
          this.widget.config.timewindow = timeWindowConfig;
          this.detectChanges();
        });
      }
    };

  }

  private defaultSubscriptionOptions(subscription: IWidgetSubscription, options: WidgetSubscriptionOptions) {
    if (this.displayLegend) {
      this.legendData = subscription.legendData;
    }
  }

  private createDefaultSubscription(): Observable<any> {
    const createSubscriptionSubject = new ReplaySubject();
    let options: WidgetSubscriptionOptions;
    if (this.widget.type !== widgetType.rpc && this.widget.type !== widgetType.static) {
      const comparisonSettings: WidgetComparisonSettings = this.widgetContext.settings;
      options = {
        type: this.widget.type,
        stateData: this.typeParameters.stateData,
        hasDataPageLink: this.typeParameters.hasDataPageLink,
        singleEntity: this.typeParameters.singleEntity,
        warnOnPageDataOverflow: this.typeParameters.warnOnPageDataOverflow,
        comparisonEnabled: comparisonSettings.comparisonEnabled,
        timeForComparison: comparisonSettings.timeForComparison
      };
      if (this.widget.type === widgetType.alarm) {
        options.alarmSource = deepClone(this.widget.config.alarmSource);
      } else {
        options.datasources = deepClone(this.widget.config.datasources);
      }

      this.defaultComponentsOptions(options);

      this.createSubscription(options).subscribe(
        (subscription) => {
          this.defaultSubscriptionOptions(subscription, options);

          // backward compatibility
          this.widgetContext.datasources = subscription.datasources;
          this.widgetContext.data = subscription.data;
          this.widgetContext.hiddenData = subscription.hiddenData;
          this.widgetContext.timeWindow = subscription.timeWindow;
          this.widgetContext.defaultSubscription = subscription;
          createSubscriptionSubject.next();
          createSubscriptionSubject.complete();
        },
        (err) => {
          createSubscriptionSubject.error(err);
        }
      );
    } else if (this.widget.type === widgetType.rpc) {
      this.loadingData = false;
      options = {
        type: this.widget.type,
        targetDeviceAliasIds: this.widget.config.targetDeviceAliasIds
      };
      options.callbacks = {
        rpcStateChanged: (subscription) => {
          if (this.dynamicWidgetComponent) {
            this.dynamicWidgetComponent.rpcEnabled = subscription.rpcEnabled;
            this.dynamicWidgetComponent.executingRpcRequest = subscription.executingRpcRequest;
            this.detectChanges();
          }
        },
        onRpcSuccess: (subscription) => {
          if (this.dynamicWidgetComponent) {
            this.dynamicWidgetComponent.executingRpcRequest = subscription.executingRpcRequest;
            this.dynamicWidgetComponent.rpcErrorText = subscription.rpcErrorText;
            this.dynamicWidgetComponent.rpcRejection = subscription.rpcRejection;
            this.clearMessage();
            this.detectChanges();
          }
        },
        onRpcFailed: (subscription) => {
          if (this.dynamicWidgetComponent) {
            this.dynamicWidgetComponent.executingRpcRequest = subscription.executingRpcRequest;
            this.dynamicWidgetComponent.rpcErrorText = subscription.rpcErrorText;
            this.dynamicWidgetComponent.rpcRejection = subscription.rpcRejection;
            if (subscription.rpcErrorText) {
              this.displayMessage('error', subscription.rpcErrorText);
            }
            this.detectChanges();
          }
        },
        onRpcErrorCleared: (subscription) => {
          if (this.dynamicWidgetComponent) {
            this.dynamicWidgetComponent.rpcErrorText = null;
            this.dynamicWidgetComponent.rpcRejection = null;
            this.clearMessage();
            this.detectChanges();
          }
        }
      };
      this.createSubscription(options).subscribe(
        (subscription) => {
          this.widgetContext.defaultSubscription = subscription;
          createSubscriptionSubject.next();
          createSubscriptionSubject.complete();
        },
        (err) => {
          createSubscriptionSubject.error(err);
        }
      );
      this.detectChanges();
    } else if (this.widget.type === widgetType.static) {
      this.loadingData = false;
      createSubscriptionSubject.next();
      createSubscriptionSubject.complete();
      this.detectChanges();
    } else {
      createSubscriptionSubject.next();
      createSubscriptionSubject.complete();
      this.detectChanges();
    }
    return createSubscriptionSubject.asObservable();
  }

  private getActionDescriptors(actionSourceId: string): Array<WidgetActionDescriptor> {
    let result = this.widgetContext.actionsApi.actionDescriptorsBySourceId[actionSourceId];
    if (!result) {
      result = [];
    }
    return result;
  }

  private handleWidgetAction($event: Event, descriptor: WidgetActionDescriptor,
                             entityId?: EntityId, entityName?: string, additionalParams?: any, entityLabel?: string): void {
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
        const params = deepClone(this.widgetContext.stateController.getStateParams());
        this.updateEntityParams(params, targetEntityParamName, targetEntityId, entityName, entityLabel);
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
        this.updateEntityParams(stateObject.params, targetEntityParamName, targetEntityId, entityName, entityLabel);
        if (targetDashboardStateId) {
          stateObject.id = targetDashboardStateId;
        }
        const state = objToBase64URI([ stateObject ]);
        const isSinglePage = this.route.snapshot.data.singlePageMode;
        let url;
        if (isSinglePage) {
          url = `/dashboard/${targetDashboardId}?state=${state}`;
        } else {
          url = `/dashboards/${targetDashboardId}?state=${state}`;
        }
        this.router.navigateByUrl(url);
        break;
      case WidgetActionType.custom:
        const customFunction = descriptor.customFunction;
        if (customFunction && customFunction.length > 0) {
          try {
            if (!additionalParams) {
              additionalParams = {};
            }
            const customActionFunction = new Function('$event', 'widgetContext', 'entityId',
              'entityName', 'additionalParams', 'entityLabel', customFunction);
            customActionFunction($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
          } catch (e) {
            console.error(e);
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
                  'entityName', 'htmlTemplate', 'additionalParams', 'entityLabel', customPrettyFunction);
                customActionPrettyFunction($event, this.widgetContext, entityId, entityName, htmlTemplate, additionalParams, entityLabel);
              } catch (e) {
                console.error(e);
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
    const e = ($event.target || $event.srcElement) as Element;
    if (e.id) {
      const descriptors = this.getActionDescriptors('elementClick');
      if (descriptors.length) {
        descriptors.forEach((descriptor) => {
          if (descriptor.name === e.id) {
            $event.stopPropagation();
            const entityInfo = this.getActiveEntityInfo();
            const entityId = entityInfo ? entityInfo.entityId : null;
            const entityName = entityInfo ? entityInfo.entityName : null;
            const entityLabel = entityInfo && entityInfo.entityLabel ? entityInfo.entityLabel : null;
            this.handleWidgetAction($event, descriptor, entityId, entityName, null, entityLabel);
          }
        });
      }
    }
  }

  private updateEntityParams(params: StateParams, targetEntityParamName?: string, targetEntityId?: EntityId,
                             entityName?: string, entityLabel?: string) {
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
      if (entityLabel) {
        targetEntityParams.entityLabel = entityLabel;
      }
    }
  }

  private loadCustomActionResources(actionNamespace: string, customCss: string, customResources: Array<WidgetResource>): Observable<any> {
    if (isDefined(customCss) && customCss.length > 0) {
      this.cssParser.cssPreviewNamespace = actionNamespace;
      this.cssParser.createStyleElement(actionNamespace, customCss, 'nonamespace');
    }
    const resourceTasks: Observable<string>[] = [];
    if (isDefined(customResources) && customResources.length > 0) {
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

  private getActiveEntityInfo(): SubscriptionEntityInfo {
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
        if (this.widgetContext.$container) {
          this.widgetContext.$container.css('height', height + 'px');
          this.widgetContext.$container.css('width', width + 'px');
        }
        this.widgetContext.width = width;
        this.widgetContext.height = height;
        sizeChanged = true;
        this.widgetSizeDetected = true;
      }
    }
    return sizeChanged;
  }

}

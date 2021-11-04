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
  Inject,
  Injector,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit, Renderer2,
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
  LegendPosition, MobileActionResult,
  Widget,
  WidgetActionDescriptor,
  widgetActionSources,
  WidgetActionType,
  WidgetComparisonSettings, WidgetMobileActionDescriptor, WidgetMobileActionType,
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
import {
  deepClone,
  insertVariable,
  isDefined,
  isNotEmptyStr,
  objToBase64,
  objToBase64URI,
  validateEntityId
} from '@core/utils';
import {
  IDynamicWidgetComponent, ShowWidgetHeaderActionFunction,
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
import { catchError, map, switchMap } from 'rxjs/operators';
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
import { MatDialog } from '@angular/material/dialog';
import { ComponentType } from '@angular/cdk/portal';
import { EMBED_DASHBOARD_DIALOG_TOKEN } from '@home/components/widget/dialog/embed-dashboard-dialog-token';
import { MobileService } from '@core/services/mobile.service';
import { DialogService } from '@core/services/dialog.service';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { PopoverPlacement } from '@shared/components/popover.models';
import { TbPopoverService } from '@shared/components/popover.service';

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
  noDataDisplayMessageText: string;

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
              private dialog: MatDialog,
              private renderer: Renderer2,
              private popoverService: TbPopoverService,
              @Inject(EMBED_DASHBOARD_DIALOG_TOKEN) private embedDashboardDialogComponent: ComponentType<any>,
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
              private mobileService: MobileService,
              private dialogs: DialogService,
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
    this.widgetContext.toastTargetId = this.toastTargetId;

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
      getActiveEntityInfo: this.getActiveEntityInfo.bind(this),
      openDashboardStateInSeparateDialog: this.openDashboardStateInSeparateDialog.bind(this),
      openDashboardStateInPopover: this.openDashboardStateInPopover.bind(this)
    };

    this.widgetContext.customHeaderActions = [];
    const headerActionsDescriptors = this.getActionDescriptors(widgetActionSources.headerButton.value);
    headerActionsDescriptors.forEach((descriptor) =>
    {
      let useShowWidgetHeaderActionFunction = descriptor.useShowWidgetActionFunction || false;
      let showWidgetHeaderActionFunction: ShowWidgetHeaderActionFunction = null;
      if (useShowWidgetHeaderActionFunction && isNotEmptyStr(descriptor.showWidgetActionFunction)) {
        try {
          showWidgetHeaderActionFunction =
            new Function('widgetContext', 'data', descriptor.showWidgetActionFunction) as ShowWidgetHeaderActionFunction;
        } catch (e) {
          useShowWidgetHeaderActionFunction = false;
        }
      }
      const headerAction: WidgetHeaderAction = {
        name: descriptor.name,
        displayName: descriptor.displayName,
        icon: descriptor.icon,
        descriptor,
        useShowWidgetHeaderActionFunction,
        showWidgetHeaderActionFunction,
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

    const noDataDisplayMessage = this.widget.config.noDataDisplayMessage;
    if (isNotEmptyStr(noDataDisplayMessage)) {
      this.noDataDisplayMessageText = this.utils.customTranslation(noDataDisplayMessage, noDataDisplayMessage);
    } else {
      this.noDataDisplayMessageText = this.translate.instant('widget.no-data');
    }
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

  private detectChanges(detectContainerChanges = false) {
    if (!this.destroyed) {
      try {
        this.cd.detectChanges();
        if (detectContainerChanges) {
          this.widgetContext.detectContainerChanges();
        }
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
      this.widgetContext.detectContainerChanges();
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
              setTimeout(() => {
                this.dashboardWidget.updateCustomHeaderActions(true);
              }, 0);
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
              setTimeout(() => {
                this.dashboardWidget.updateCustomHeaderActions(true);
              }, 0);
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
      forceReInit: () => {
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
          this.detectChanges(true);
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
        datasourcesOptional: this.typeParameters.datasourcesOptional,
        hasDataPageLink: this.typeParameters.hasDataPageLink,
        singleEntity: this.typeParameters.singleEntity,
        warnOnPageDataOverflow: this.typeParameters.warnOnPageDataOverflow,
        ignoreDataUpdateOnIntervalTick: this.typeParameters.ignoreDataUpdateOnIntervalTick,
        comparisonEnabled: comparisonSettings.comparisonEnabled,
        timeForComparison: comparisonSettings.timeForComparison,
        comparisonCustomIntervalValue: comparisonSettings.comparisonCustomIntervalValue
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
    if (descriptor.setEntityId && validateEntityId(entityId)) {
      targetEntityId = entityId;
    }
    switch (type) {
      case WidgetActionType.openDashboardState:
      case WidgetActionType.updateDashboardState:
        let targetDashboardStateId = descriptor.targetDashboardStateId;
        const params = deepClone(this.widgetContext.stateController.getStateParams());
        this.updateEntityParams(params, targetEntityParamName, targetEntityId, entityName, entityLabel);
        if (type === WidgetActionType.openDashboardState) {
          if (descriptor.openInPopover) {
            this.openDashboardStateInPopover($event, descriptor.targetDashboardStateId, params,
              descriptor.popoverHideDashboardToolbar, descriptor.popoverPreferredPlacement,
              descriptor.popoverHideOnClickOutside, descriptor.popoverWidth, descriptor.popoverHeight, descriptor.popoverStyle);
          } else if (descriptor.openInSeparateDialog && !this.mobileService.isMobileApp()) {
            this.openDashboardStateInSeparateDialog(descriptor.targetDashboardStateId, params, descriptor.dialogTitle,
              descriptor.dialogHideDashboardToolbar, descriptor.dialogWidth, descriptor.dialogHeight);
          } else {
            this.widgetContext.stateController.openState(targetDashboardStateId, params, descriptor.openRightLayout);
          }
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
        if (descriptor.openNewBrowserTab) {
          window.open(url, '_blank');
        } else {
          this.router.navigateByUrl(url);
        }
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
      case WidgetActionType.mobileAction:
        const mobileAction = descriptor.mobileAction;
        this.handleMobileAction($event, mobileAction, entityId, entityName, additionalParams, entityLabel);
        break;
    }
  }

  private handleMobileAction($event: Event, mobileAction: WidgetMobileActionDescriptor,
                             entityId?: EntityId, entityName?: string, additionalParams?: any, entityLabel?: string) {
    const type = mobileAction.type;
    let argsObservable: Observable<any[]>;
    switch (type) {
      case WidgetMobileActionType.takePictureFromGallery:
      case WidgetMobileActionType.takePhoto:
      case WidgetMobileActionType.scanQrCode:
      case WidgetMobileActionType.getLocation:
      case WidgetMobileActionType.takeScreenshot:
        argsObservable = of([]);
        break;
      case WidgetMobileActionType.mapDirection:
      case WidgetMobileActionType.mapLocation:
        const getLocationFunctionString = mobileAction.getLocationFunction;
        const getLocationFunction = new Function('$event', 'widgetContext', 'entityId',
          'entityName', 'additionalParams', 'entityLabel', getLocationFunctionString);
        const locationArgs = getLocationFunction($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
        if (locationArgs && locationArgs instanceof Observable) {
          argsObservable = locationArgs;
        } else {
          argsObservable = of(locationArgs);
        }
        argsObservable = argsObservable.pipe(map(latLng => {
          let valid = false;
          if (Array.isArray(latLng) && latLng.length === 2) {
            if (typeof latLng[0] === 'number' && typeof latLng[1] === 'number') {
              valid = true;
            }
          }
          if (valid) {
            return latLng;
          } else {
            throw new Error('Location function did not return valid array of latitude/longitude!');
          }
        }));
        break;
      case WidgetMobileActionType.makePhoneCall:
        const getPhoneNumberFunctionString = mobileAction.getPhoneNumberFunction;
        const getPhoneNumberFunction = new Function('$event', 'widgetContext', 'entityId',
          'entityName', 'additionalParams', 'entityLabel', getPhoneNumberFunctionString);
        const phoneNumberArg = getPhoneNumberFunction($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
        if (phoneNumberArg && phoneNumberArg instanceof Observable) {
          argsObservable = phoneNumberArg.pipe(map(phoneNumber => [phoneNumber]));
        } else {
          argsObservable = of([phoneNumberArg]);
        }
        argsObservable = argsObservable.pipe(map(phoneNumberArr => {
          let valid = false;
          if (Array.isArray(phoneNumberArr) && phoneNumberArr.length === 1) {
            if (phoneNumberArr[0] !== null) {
              valid = true;
            }
          }
          if (valid) {
            return phoneNumberArr;
          } else {
            throw new Error('Phone number function did not return valid number!');
          }
        }));
        break;
    }
    argsObservable.subscribe((args) => {
      this.mobileService.handleWidgetMobileAction(type, ...args).subscribe(
        (result) => {
          if (result) {
            if (result.hasError) {
              this.handleWidgetMobileActionError(result.error, $event, mobileAction, entityId, entityName, additionalParams, entityLabel);
            } else if (result.hasResult) {
              const actionResult = result.result;
              switch (type) {
                case WidgetMobileActionType.takePictureFromGallery:
                case WidgetMobileActionType.takePhoto:
                case WidgetMobileActionType.takeScreenshot:
                  const imageUrl = actionResult.imageUrl;
                  if (mobileAction.processImageFunction && mobileAction.processImageFunction.length) {
                    try {
                      const processImageFunction = new Function('imageUrl', '$event', 'widgetContext', 'entityId',
                        'entityName', 'additionalParams', 'entityLabel', mobileAction.processImageFunction);
                      processImageFunction(imageUrl, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                    } catch (e) {
                      console.error(e);
                    }
                  }
                  break;
                case WidgetMobileActionType.scanQrCode:
                  const code = actionResult.code;
                  const format = actionResult.format;
                  if (mobileAction.processQrCodeFunction && mobileAction.processQrCodeFunction.length) {
                    try {
                      const processQrCodeFunction = new Function('code', 'format', '$event', 'widgetContext', 'entityId',
                        'entityName', 'additionalParams', 'entityLabel', mobileAction.processQrCodeFunction);
                      processQrCodeFunction(code, format, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                    } catch (e) {
                      console.error(e);
                    }
                  }
                  break;
                case WidgetMobileActionType.getLocation:
                  const latitude = actionResult.latitude;
                  const longitude = actionResult.longitude;
                  if (mobileAction.processLocationFunction && mobileAction.processLocationFunction.length) {
                    try {
                      const processLocationFunction = new Function('latitude', 'longitude', '$event', 'widgetContext', 'entityId',
                        'entityName', 'additionalParams', 'entityLabel', mobileAction.processLocationFunction);
                      processLocationFunction(latitude, longitude, $event, this.widgetContext,
                        entityId, entityName, additionalParams, entityLabel);
                    } catch (e) {
                      console.error(e);
                    }
                  }
                  break;
                case WidgetMobileActionType.mapDirection:
                case WidgetMobileActionType.mapLocation:
                case WidgetMobileActionType.makePhoneCall:
                  const launched = actionResult.launched;
                  if (mobileAction.processLaunchResultFunction && mobileAction.processLaunchResultFunction.length) {
                    try {
                      const processLaunchResultFunction = new Function('launched', '$event', 'widgetContext', 'entityId',
                        'entityName', 'additionalParams', 'entityLabel', mobileAction.processLaunchResultFunction);
                      processLaunchResultFunction(launched, $event, this.widgetContext,
                        entityId, entityName, additionalParams, entityLabel);
                    } catch (e) {
                      console.error(e);
                    }
                  }
                  break;
              }
            } else {
              if (mobileAction.handleEmptyResultFunction && mobileAction.handleEmptyResultFunction.length) {
                try {
                  const handleEmptyResultFunction = new Function('$event', 'widgetContext', 'entityId',
                    'entityName', 'additionalParams', 'entityLabel', mobileAction.handleEmptyResultFunction);
                  handleEmptyResultFunction($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                } catch (e) {
                  console.error(e);
                }
              }
            }
          }
        }
      );
    },
    (err) => {
      let errorMessage;
      if (err && typeof err === 'string') {
        errorMessage = err;
      } else if (err && err.message) {
        errorMessage = err.message;
      }
      errorMessage = `Failed to get mobile action arguments${errorMessage ? `: ${errorMessage}` : '!'}`;
      this.handleWidgetMobileActionError(errorMessage, $event, mobileAction, entityId, entityName, additionalParams, entityLabel);
    });
  }

  private handleWidgetMobileActionError(error: string, $event: Event, mobileAction: WidgetMobileActionDescriptor,
                                        entityId?: EntityId, entityName?: string, additionalParams?: any, entityLabel?: string) {
    if (mobileAction.handleErrorFunction && mobileAction.handleErrorFunction.length) {
      try {
        const handleErrorFunction = new Function('error', '$event', 'widgetContext', 'entityId',
          'entityName', 'additionalParams', 'entityLabel', mobileAction.handleErrorFunction);
        handleErrorFunction(error, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
      } catch (e) {
        console.error(e);
      }
    }
  }

  private openDashboardStateInPopover($event: Event,
                                      targetDashboardStateId: string,
                                      params?: StateParams,
                                      hideDashboardToolbar = true,
                                      preferredPlacement: PopoverPlacement = 'top',
                                      hideOnClickOutside = true,
                                      popoverWidth = '25vw',
                                      popoverHeight = '25vh',
                                      popoverStyle: { [klass: string]: any } = {}) {
    const trigger = ($event.target || $event.srcElement || $event.currentTarget) as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const dashboard = deepClone(this.widgetContext.stateController.dashboardCtrl.dashboardCtx.getDashboard());
      const stateObject: StateObject = {};
      stateObject.params = params;
      if (targetDashboardStateId) {
        stateObject.id = targetDashboardStateId;
      }
      const injector = Injector.create({
        parent: this.widgetContentContainer.injector, providers: [
          {
            provide: 'embeddedValue',
            useValue: true
          }
        ]
      });
      const component = this.popoverService.displayPopover(trigger, this.renderer,
        this.widgetContentContainer, DashboardPageComponent, preferredPlacement, hideOnClickOutside,
        injector,
        {
          embed: true,
          syncStateWithQueryParam: false,
          hideToolbar: hideDashboardToolbar,
          currentState: objToBase64([stateObject]),
          dashboard,
          parentDashboard: this.widgetContext.parentDashboard ?
            this.widgetContext.parentDashboard : this.widgetContext.dashboard
        },
        {width: popoverWidth, height: popoverHeight},
        popoverStyle,
        {}
      );
      this.widgetContext.registerPopoverComponent(component);
    }
  }

  private openDashboardStateInSeparateDialog(targetDashboardStateId: string, params?: StateParams, dialogTitle?: string,
                                             hideDashboardToolbar = true, dialogWidth?: number, dialogHeight?: number) {
    const dashboard = deepClone(this.widgetContext.stateController.dashboardCtrl.dashboardCtx.getDashboard());
    const stateObject: StateObject = {};
    stateObject.params = params;
    if (targetDashboardStateId) {
      stateObject.id = targetDashboardStateId;
    }
    let title = dialogTitle;
    if (!title) {
      if (targetDashboardStateId && dashboard.configuration.states) {
        const dashboardState = dashboard.configuration.states[targetDashboardStateId];
        if (dashboardState) {
          title = dashboardState.name;
        }
      }
    }
    if (!title) {
      title = dashboard.title;
    }
    title = this.utils.customTranslation(title, title);
    const paramsEntityName = params && params.entityName ? params.entityName : '';
    const paramsEntityLabel = params && params.entityLabel ? params.entityLabel : '';
    title = insertVariable(title, 'entityName', paramsEntityName);
    title = insertVariable(title, 'entityLabel', paramsEntityLabel);
    for (const prop of Object.keys(params)) {
      if (params[prop] && params[prop].entityName) {
        title = insertVariable(title, prop + ':entityName', params[prop].entityName);
      }
      if (params[prop] && params[prop].entityLabel) {
        title = insertVariable(title, prop + ':entityLabel', params[prop].entityLabel);
      }
    }
    this.dialog.open(this.embedDashboardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      viewContainerRef: this.widgetContentContainer,
      data: {
        dashboard,
        state: objToBase64([ stateObject ]),
        title,
        hideToolbar: hideDashboardToolbar,
        width: dialogWidth,
        height: dialogHeight
      }
    });
  }

  private elementClick($event: Event) {
    const elementClicked = ($event.target || $event.srcElement) as Element;
    const descriptors = this.getActionDescriptors('elementClick');
    if (descriptors.length) {
      const idsList = descriptors.map(descriptor => `#${descriptor.name}`).join(',');
      const targetElement = $(elementClicked).closest(idsList, this.widgetContext.$container[0]);
      if (targetElement.length && targetElement[0].id) {
        $event.stopPropagation();
        const descriptor = descriptors.find(descriptorInfo => descriptorInfo.name === targetElement[0].id);
        const entityInfo = this.getActiveEntityInfo();
        const entityId = entityInfo ? entityInfo.entityId : null;
        const entityName = entityInfo ? entityInfo.entityName : null;
        const entityLabel = entityInfo && entityInfo.entityLabel ? entityInfo.entityLabel : null;
        this.handleWidgetAction($event, descriptor, entityId, entityName, null, entityLabel);
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

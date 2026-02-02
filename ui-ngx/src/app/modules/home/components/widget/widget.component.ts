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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentRef,
  ElementRef,
  Inject,
  Injector,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Optional,
  Renderer2,
  SimpleChanges,
  TemplateRef,
  Type,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { DashboardWidget } from '@home/models/dashboard-component.models';
import {
  Widget,
  WidgetAction,
  WidgetActionDescriptor,
  widgetActionSources,
  WidgetActionType,
  WidgetComparisonSettings,
  WidgetHeaderActionButtonType,
  WidgetMobileActionDescriptor,
  WidgetMobileActionType,
  WidgetResource,
  widgetType,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { forkJoin, Observable, of, ReplaySubject, Subscription, throwError } from 'rxjs';
import {
  deepClone,
  guid,
  insertVariable,
  isDefined,
  isNotEmptyStr,
  objToBase64,
  objToBase64URI,
  validateEntityId
} from '@core/utils';
import {
  IDynamicWidgetComponent,
  ShowWidgetHeaderActionFunction,
  updateEntityParams,
  WidgetContext,
  widgetContextToken,
  widgetErrorMessagesToken,
  WidgetHeaderAction,
  WidgetInfo,
  widgetTitlePanelToken,
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
import {
  flatModulesWithComponents,
  ModulesWithComponents,
  modulesWithComponentsToTypes,
  ResourcesService
} from '@core/services/resources.service';
import { catchError, map, switchMap } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TimeService } from '@core/services/time.service';
import { DeviceService } from '@app/core/http/device.service';
import { ExceptionData } from '@shared/models/error.models';
import { WidgetComponentService } from './widget-component.service';
import { Timewindow } from '@shared/models/time/time.models';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { UnitService } from '@core/services/unit.service';
import { DashboardService } from '@core/http/dashboard.service';
import { WidgetSubscription } from '@core/api/widget-subscription';
import { EntityService } from '@core/http/entity.service';
import { ServicesMap } from '@home/models/services.map';
import { EntityDataService } from '@core/api/entity-data.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationType } from '@core/notification/notification.models';
import { AlarmDataService } from '@core/api/alarm-data.service';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { ComponentType } from '@angular/cdk/portal';
import { EMBED_DASHBOARD_DIALOG_TOKEN } from '@home/components/widget/dialog/embed-dashboard-dialog-token';
import { MobileService } from '@core/services/mobile.service';
import { PopoverPlacement } from '@shared/components/popover.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { DASHBOARD_PAGE_COMPONENT_TOKEN } from '@home/components/tokens';
import { MODULES_MAP } from '@shared/models/constants';
import { IModulesMap } from '@modules/common/modules-map.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { CompiledTbFunction, compileTbFunction, isNotEmptyTbFunction } from '@shared/models/js-function.models';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'tb-widget',
  templateUrl: './widget.component.html',
  styleUrls: ['./widget.component.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetComponent extends PageComponent implements OnInit, OnChanges, OnDestroy {

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  @Input()
  widgetHeaderActionsPanel: TemplateRef<any>;

  @Input()
  isEdit: boolean;

  @Input()
  isPreview: boolean;

  @Input()
  isMobile: boolean;

  @Input()
  dashboardWidget: DashboardWidget;

  @Input()
  widget: Widget;

  @ViewChild('widgetContent', {read: ViewContainerRef, static: true}) widgetContentContainer: ViewContainerRef;

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

  dynamicWidgetComponentRef: ComponentRef<IDynamicWidgetComponent>;
  dynamicWidgetComponent: IDynamicWidgetComponent;

  subscriptionContext: WidgetSubscriptionContext;

  subscriptionInited = false;
  destroyed = false;
  widgetSizeDetected = false;
  widgetInstanceInited = false;
  dataUpdatePending = false;
  latestDataUpdatePending = false;
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
              private elementRef: ElementRef,
              private injector: Injector,
              private dialog: MatDialog,
              private renderer: Renderer2,
              private popoverService: TbPopoverService,
              @Inject(EMBED_DASHBOARD_DIALOG_TOKEN) private embedDashboardDialogComponent: ComponentType<any>,
              @Inject(DASHBOARD_PAGE_COMPONENT_TOKEN) private dashboardPageComponent: ComponentType<any>,
              @Optional() @Inject(MODULES_MAP) private modulesMap: IModulesMap,
              private resources: ResourcesService,
              private timeService: TimeService,
              private deviceService: DeviceService,
              private entityService: EntityService,
              private dashboardService: DashboardService,
              private entityDataService: EntityDataService,
              private alarmDataService: AlarmDataService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private mobileService: MobileService,
              private raf: RafService,
              private unitService: UnitService,
              private ngZone: NgZone,
              private cd: ChangeDetectorRef,
              private http: HttpClient) {
    super(store);
    this.cssParser.testMode = false;
  }

  ngOnInit(): void {

    this.loadingData = true;

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
    this.widgetContext.isPreview = this.isPreview;
    this.widgetContext.isMobile = this.isMobile;
    this.widgetContext.toastTargetId = this.toastTargetId;
    this.widgetContext.renderer = this.renderer;
    this.widgetContext.widgetContentContainer = this.widgetContentContainer;

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
      onWidgetAction: this.onWidgetAction.bind(this),
      elementClick: this.elementClick.bind(this),
      cardClick: this.cardClick.bind(this),
      click: this.click.bind(this),
      getActiveEntityInfo: this.getActiveEntityInfo.bind(this),
      openDashboardStateInSeparateDialog: this.openDashboardStateInSeparateDialog.bind(this),
      openDashboardStateInPopover: this.openDashboardStateInPopover.bind(this),
      placeMapItem: () => {}
    };

    this.widgetContext.customHeaderActions = [];

    const headerActionsDescriptors = this.getActionDescriptors(widgetActionSources.headerButton.value);

    const customHeaderActions$ = headerActionsDescriptors.map((descriptor) => {
      let useShowWidgetHeaderActionFunction = descriptor.useShowWidgetActionFunction || false;
      let showWidgetHeaderActionFunction$: Observable<CompiledTbFunction<ShowWidgetHeaderActionFunction>>;
      if (useShowWidgetHeaderActionFunction && isNotEmptyTbFunction(descriptor.showWidgetActionFunction)) {
        showWidgetHeaderActionFunction$ = compileTbFunction(this.http, descriptor.showWidgetActionFunction, 'widgetContext', 'data');
      } else {
        showWidgetHeaderActionFunction$ = of(null);
      }
      return showWidgetHeaderActionFunction$.pipe(
        catchError(() => { return of(null) }),
        map(showWidgetHeaderActionFunction => {
          if (!showWidgetHeaderActionFunction) {
            useShowWidgetHeaderActionFunction = false;
          }
          const headerAction: WidgetHeaderAction = {
            name: descriptor.name,
            displayName: descriptor.displayName,
            buttonType: descriptor.buttonType,
            showIcon: descriptor.showIcon,
            icon: descriptor.icon,
            customButtonStyle: this.headerButtonStyle(
              descriptor.buttonType,
              descriptor.customButtonStyle,
              descriptor.buttonColor,
              descriptor.buttonFillColor,
              descriptor.buttonBorderColor
            ),
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
          return headerAction;
        })
      );
    });

    if (customHeaderActions$.length) {
      forkJoin(customHeaderActions$).subscribe((customHeaderActions) => {
        this.widgetContext.customHeaderActions.push(...customHeaderActions);
      });
    }

    this.subscriptionContext = new WidgetSubscriptionContext(this.widgetContext.dashboard);
    this.subscriptionContext.timeService = this.timeService;
    this.subscriptionContext.deviceService = this.deviceService;
    this.subscriptionContext.translate = this.translate;
    this.subscriptionContext.entityDataService = this.entityDataService;
    this.subscriptionContext.alarmDataService = this.alarmDataService;
    this.subscriptionContext.utils = this.utils;
    this.subscriptionContext.dashboardUtils = this.dashboardUtils;
    this.subscriptionContext.raf = this.raf;
    this.subscriptionContext.unitService = this.unitService;
    this.subscriptionContext.widgetUtils = this.widgetContext.utils;
    this.subscriptionContext.getServerTimeDiff = this.dashboardService.getServerTimeDiff.bind(this.dashboardService);

    this.widgetComponentService.getWidgetInfo(this.widget.typeFullFqn).subscribe({
      next: (widgetInfo) => {
        this.widgetInfo = widgetInfo;
        this.loadFromWidgetInfo();
      },
      error: (errorData) => {
        this.widgetInfo = errorData.widgetInfo;
        this.errorMessages = errorData.errorMessages;
        this.loadFromWidgetInfo();
      }
    });

    const noDataDisplayMessage = this.widget.config.noDataDisplayMessage;
    if (isNotEmptyStr(noDataDisplayMessage)) {
      this.noDataDisplayMessageText = this.utils.customTranslation(noDataDisplayMessage, noDataDisplayMessage);
    } else {
      this.noDataDisplayMessageText = this.translate.instant('widget.no-data');
    }
  }

  headerButtonStyle(buttonType: WidgetHeaderActionButtonType = WidgetHeaderActionButtonType.icon,
                    customButtonStyle:{[key: string]: string},
                    buttonColor: string = this.widget.config.color,
                    backgroundColor: string,
                    borderColor: string) {
    const buttonStyle = {};
    switch (buttonType) {
      case WidgetHeaderActionButtonType.basic:
        buttonStyle['--mdc-text-button-label-text-color'] = buttonColor;
        break;
      case WidgetHeaderActionButtonType.raised:
        buttonStyle['--mdc-protected-button-label-text-color'] = buttonColor;
        buttonStyle['--mdc-protected-button-container-color'] = backgroundColor;
        break;
      case WidgetHeaderActionButtonType.stroked:
        buttonStyle['--mdc-outlined-button-label-text-color'] = buttonColor;
        buttonStyle['--mdc-outlined-button-outline-color'] = borderColor;
        break;
      case WidgetHeaderActionButtonType.flat:
        buttonStyle['--mdc-filled-button-label-text-color'] = buttonColor;
        buttonStyle['--mdc-filled-button-container-color'] = backgroundColor;
        break;
      case WidgetHeaderActionButtonType.miniFab:
        buttonStyle['--mat-fab-small-foreground-color'] = buttonColor;
        buttonStyle['--mdc-fab-small-container-color'] = backgroundColor;
        break;
      default:
        buttonStyle['--mat-icon-color'] = buttonColor;
        break;
    }
    return {...buttonStyle, ...customButtonStyle};
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
    if (this.widget.type === widgetType.static || this.typeParameters?.processNoDataByWidget) {
      return true;
    }

    for (const id of Object.keys(this.widgetContext.subscriptions)) {
      const subscription = this.widgetContext.subscriptions[id];
      if (subscription.isDataResolved()) {
        return true;
      }
    }
    return false;
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
      this.widgetContext.destroy();
      this.destroyDynamicWidgetComponent();
    }
  }

  public onTimewindowChanged(timewindow: Timewindow) {
    for (const id of Object.keys(this.widgetContext.subscriptions)) {
      const subscription = this.widgetContext.subscriptions[id];
      if (!subscription.useDashboardTimewindow) {
        subscription.updateTimewindowConfig(subscription.onTimewindowChangeFunction(timewindow));
      }
    }
  }

  private loadFromWidgetInfo() {
    this.widgetContext.widgetNamespace =
      `widget-type-${this.widget.typeFullFqn.replace(/\./g, '-')}`;
    const elem = this.elementRef.nativeElement;
    elem.classList.add('tb-widget');
    elem.classList.add(this.widgetContext.widgetNamespace);
    this.widgetType = this.widgetInfo.widgetTypeFunction;
    this.typeParameters = this.widgetInfo.typeParameters;
    this.widgetContext.embedTitlePanel = this.typeParameters.embedTitlePanel;
    this.widgetContext.embedActionsPanel = this.typeParameters.embedActionsPanel;
    this.widgetContext.overflowVisible = this.typeParameters.overflowVisible;

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
    if (!this.widgetTypeInstance.onLatestDataUpdated) {
      this.widgetTypeInstance.onLatestDataUpdated = () => {};
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

    this.initialize().subscribe({
      next: () => {
        this.onInit();
      },
      error: () => {
        this.widgetContext.inited = true;
        // console.log(err);
      }
    });
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
      this.widgetContext.destroyed = false;
      this.dashboardWidget.updateWidgetParams();
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
                this.dashboardWidget.updateParamsFromData(true);
              }, 0);
              this.dataUpdatePending = false;
            }
            if (this.latestDataUpdatePending) {
              this.widgetTypeInstance.onLatestDataUpdated();
              this.latestDataUpdatePending = false;
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
        this.ngZone.run(() => {
          this.onInit(true);
        });
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
      this.createDefaultSubscription().subscribe({
        next: () => {
          if (this.destroyed) {
            this.onDestroy();
          } else {
            this.widgetContext.reset();
            this.subscriptionInited = true;
            this.configureDynamicWidgetComponent();
            this.onInit();
          }
        },
        error: () => {
          if (this.destroyed) {
            this.onDestroy();
          } else {
            this.widgetContext.reset();
            this.subscriptionInited = true;
            this.onInit();
          }
        }
      });
    } else {
      this.widgetContext.reset();
      this.subscriptionInited = true;
      this.configureDynamicWidgetComponent();
      this.onInit();
    }
  }

  private initialize(): Observable<any> {

    const initSubject = new ReplaySubject<void>();

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
      this.createDefaultSubscription().subscribe({
        next: () => {
          this.subscriptionInited = true;
          try {
            this.configureDynamicWidgetComponent();
            initSubject.next();
            initSubject.complete();
          } catch (err) {
            initSubject.error(err);
          }
        },
        error: (err) => {
          this.subscriptionInited = true;
          initSubject.error(err);
        }
      });
    } else {
      this.loadingData = false;
      this.subscriptionInited = true;
      try {
        this.configureDynamicWidgetComponent();
        initSubject.next();
        initSubject.complete();
      }  catch (err) {
        initSubject.error(err);
      }
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

  private handleWidgetException(e: any) {
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
              provide: widgetContextToken,
              useValue: this.widgetContext
            },
            {
              provide: widgetErrorMessagesToken,
              useValue: this.errorMessages
            },
            {
              provide: widgetTitlePanelToken,
              useValue: this.widgetTitlePanel
            }
          ],
          parent: this.injector
        }
      );

      const containerElement = $(this.elementRef.nativeElement.querySelector('#widget-container'));
      this.widgetContext.$containerParent = $(containerElement);

      try {
        this.dynamicWidgetComponentRef = this.widgetContentContainer.createComponent(this.widgetInfo.componentType,
          {index: 0, injector});
        this.cd.detectChanges();
      } catch (e) {
        if (this.dynamicWidgetComponentRef) {
          this.dynamicWidgetComponentRef.destroy();
          this.dynamicWidgetComponentRef = null;
        }
        this.widgetContentContainer.clear();
        this.handleWidgetException(e);
        this.widgetComponentService.clearWidgetInfo(this.widgetInfo);
        throw e;
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
    subscription.init$.subscribe({
      next: () => {
        this.widgetContext.subscriptions[subscription.id] = subscription;
        if (subscribe) {
          subscription.subscribe();
        }
        createSubscriptionSubject.next(subscription);
        createSubscriptionSubject.complete();
      },
      error: (err) => {
        createSubscriptionSubject.error(err);
      }
    });
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
    this.createSubscription(options, subscribe).subscribe({
      next: (subscription) => {
        createSubscriptionSubject.next(subscription);
        createSubscriptionSubject.complete();
      },
      error: (err) => {
        createSubscriptionSubject.error(err);
      }
    });
    return createSubscriptionSubject.asObservable();
  }

  private defaultComponentsOptions(options: WidgetSubscriptionOptions) {
    options.useDashboardTimewindow = isDefined(this.widget.config.useDashboardTimewindow)
          ? this.widget.config.useDashboardTimewindow : true;
    options.displayTimewindow = isDefined(this.widget.config.displayTimewindow)
      ? this.widget.config.displayTimewindow : !options.useDashboardTimewindow;
    options.timeWindowConfig = options.useDashboardTimewindow ? this.widgetContext.dashboardTimewindow : this.widget.config.timewindow;
    options.legendConfig = null;
    if (this.widget.config.settings.showLegend === true) {
      options.legendConfig = this.widget.config.settings.legendConfig;
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
                this.dashboardWidget.updateParamsFromData(true);
              }, 0);
            } else {
              this.dataUpdatePending = true;
            }
          }
        } catch (e){/**/}
      },
      onLatestDataUpdated: () => {
        try {
          if (this.displayWidgetInstance()) {
            if (this.widgetInstanceInited) {
              this.widgetTypeInstance.onLatestDataUpdated();
            } else {
              this.latestDataUpdatePending = true;
            }
          }
        } catch (e){/**/}
      },
      onDataUpdateError: (_subscription, e) => {
        this.handleWidgetException(e);
      },
      onLatestDataUpdateError: (_subscription, e) => {
        this.handleWidgetException(e);
      },
      onSubscriptionMessage: (_subscription, message) => {
        if (this.displayWidgetInstance()) {
          if (this.widgetInstanceInited) {
            this.displayMessage(message.severity, message.message);
          } else {
            this.pendingMessage = message;
          }
        }
      },
      onInitialPageDataChanged: (_subscription, _nextPageData) => {
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
      legendDataUpdated: (_subscription, detectChanges) => {
        if (detectChanges) {
          this.detectChanges();
        }
      },
      timeWindowUpdated: (_subscription, timeWindowConfig) => {
        this.ngZone.run(() => {
          this.widget.config.timewindow = timeWindowConfig;
          this.detectChanges(true);
        });
      }
    };
  }

  private createDefaultSubscription(): Observable<any> {
    const createSubscriptionSubject = new ReplaySubject<void>();
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
        comparisonCustomIntervalValue: comparisonSettings.comparisonCustomIntervalValue,
        pageSize: this.widget.config.pageSize
      };
      if (this.widget.type === widgetType.alarm) {
        options.alarmSource = deepClone(this.widget.config.alarmSource);
      } else {
        options.datasources = deepClone(this.widget.config.datasources);
      }

      this.defaultComponentsOptions(options);

      this.createSubscription(options).subscribe({
        next: (subscription) => {

          // backward compatibility
          this.widgetContext.datasources = subscription.datasources;
          this.widgetContext.data = subscription.data;
          this.widgetContext.latestData = subscription.latestData;
          this.widgetContext.hiddenData = subscription.hiddenData;
          this.widgetContext.timeWindow = subscription.timeWindow;
          this.widgetContext.defaultSubscription = subscription;
          this.ngZone.run(() => {
            createSubscriptionSubject.next();
            createSubscriptionSubject.complete();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            createSubscriptionSubject.error(err);
          });
        }
      });
    } else if (this.widget.type === widgetType.rpc) {
      this.loadingData = false;
      options = {
        type: this.widget.type,
        targetDevice: this.widget.config.targetDevice
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
            if (this.typeParameters.displayRpcMessageToast) {
              this.clearMessage();
            }
            this.detectChanges();
          }
        },
        onRpcFailed: (subscription) => {
          if (this.dynamicWidgetComponent) {
            this.dynamicWidgetComponent.executingRpcRequest = subscription.executingRpcRequest;
            this.dynamicWidgetComponent.rpcErrorText = subscription.rpcErrorText;
            this.dynamicWidgetComponent.rpcRejection = subscription.rpcRejection;
            if (subscription.rpcErrorText && this.typeParameters.displayRpcMessageToast) {
              this.displayMessage('error', subscription.rpcErrorText);
            }
            this.detectChanges();
          }
        },
        onRpcErrorCleared: (_subscription) => {
          if (this.dynamicWidgetComponent) {
            this.dynamicWidgetComponent.rpcErrorText = null;
            this.dynamicWidgetComponent.rpcRejection = null;
            if (this.typeParameters.displayRpcMessageToast) {
              this.clearMessage();
            }
            this.detectChanges();
          }
        }
      };
      this.createSubscription(options).subscribe({
        next: (subscription) => {
          this.widgetContext.defaultSubscription = subscription;
          this.ngZone.run(() => {
            createSubscriptionSubject.next();
            createSubscriptionSubject.complete();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            createSubscriptionSubject.error(err);
          });
        }
      });
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

  private handleWidgetAction($event: Event, descriptor: WidgetAction,
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
        const params = deepClone(this.widgetContext.stateController.getStateParams());
        updateEntityParams(params, targetEntityParamName, targetEntityId, entityName, entityLabel);
        if (type === WidgetActionType.openDashboardState) {
          if (descriptor.openInPopover) {
            this.openDashboardStateInPopover($event, descriptor.targetDashboardStateId, params,
              descriptor.popoverHideDashboardToolbar, descriptor.popoverPreferredPlacement,
              descriptor.popoverHideOnClickOutside, descriptor.popoverWidth, descriptor.popoverHeight, descriptor.popoverStyle);
          } else if (descriptor.openInSeparateDialog && !this.mobileService.isMobileApp()) {
            this.openDashboardStateInSeparateDialog(descriptor.targetDashboardStateId, params, descriptor.dialogTitle,
              descriptor.dialogHideDashboardToolbar, descriptor.dialogWidth, descriptor.dialogHeight);
          } else {
            this.widgetContext.stateController.openState(descriptor.targetDashboardStateId, params, descriptor.openRightLayout);
          }
        } else {
          this.widgetContext.stateController.updateState(descriptor.targetDashboardStateId, params, descriptor.openRightLayout);
        }
        break;
      case WidgetActionType.openDashboard:
        const targetDashboardId = descriptor.targetDashboardId;
        const stateObject: StateObject = {};
        stateObject.params = {};
        updateEntityParams(stateObject.params, targetEntityParamName, targetEntityId, entityName, entityLabel);
        if (descriptor.targetDashboardStateId) {
          stateObject.id = descriptor.targetDashboardStateId;
        }
        const state = objToBase64URI([ stateObject ]);
        const isSinglePage = this.route.snapshot.data.singlePageMode;
        let url: string;
        if (isSinglePage) {
          url = `/dashboard/${targetDashboardId}?state=${state}`;
        } else {
          url = `/dashboards/${targetDashboardId}?state=${state}`;
        }
        if (descriptor.openNewBrowserTab) {
          window.open(url, '_blank');
        } else {
          this.router.navigateByUrl(url).then(() => {});
        }
        break;
      case WidgetActionType.openURL:
        window.open(descriptor.url, descriptor.openNewBrowserTab ? '_blank' : '_self');
        break;
      case WidgetActionType.custom:
        const customFunction = descriptor.customFunction;
        if (isNotEmptyTbFunction(customFunction)) {
          compileTbFunction(this.http, customFunction, '$event', 'widgetContext', 'entityId',
            'entityName', 'additionalParams', 'entityLabel').subscribe(
            {
              next: (compiled) => {
                try {
                  if (!additionalParams) {
                    additionalParams = {};
                  }
                  compiled.execute($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                } catch (e) {
                  console.error(e);
                }
              },
              error: (err) => {
                console.error(err);
              }
            }
          )
        }
        break;
      case WidgetActionType.placeMapItem:
        this.widgetContext.actionsApi.placeMapItem({
          action: descriptor,
          afterPlaceItemCallback: this.executeCustomPrettyAction.bind(this),
          additionalParams: additionalParams
        });
        break;
      case WidgetActionType.customPretty:
        this.executeCustomPrettyAction($event, descriptor, entityId, entityName, additionalParams, entityLabel);
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
      case WidgetMobileActionType.deviceProvision:
        argsObservable = of([]);
        break;
      case WidgetMobileActionType.mapDirection:
      case WidgetMobileActionType.mapLocation:
        argsObservable = compileTbFunction(this.http, mobileAction.getLocationFunction, '$event', 'widgetContext', 'entityId',
          'entityName', 'additionalParams', 'entityLabel').pipe(
          switchMap(getLocationFunction => {
            const locationArgs = getLocationFunction.execute($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
            if (locationArgs && locationArgs instanceof Observable) {
              return locationArgs;
            } else {
              return of(locationArgs);
            }
          }),
          map(latLng => {
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
          })
        );
        break;
      case WidgetMobileActionType.makePhoneCall:
        argsObservable = compileTbFunction(this.http, mobileAction.getPhoneNumberFunction, '$event', 'widgetContext', 'entityId',
          'entityName', 'additionalParams', 'entityLabel').pipe(
          switchMap(getPhoneNumberFunction => {
            const phoneNumberArg = getPhoneNumberFunction.execute($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
            if (phoneNumberArg && phoneNumberArg instanceof Observable) {
              return phoneNumberArg.pipe(map(phoneNumber => [phoneNumber]));
            } else {
              return of([phoneNumberArg]);
            }
          }),
          map(phoneNumberArr => {
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
          })
        );
        break;
    }
    argsObservable.subscribe(
      {
        next: (args) => {
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
                      if (isNotEmptyTbFunction(mobileAction.processImageFunction)) {
                        compileTbFunction(this.http, mobileAction.processImageFunction, 'imageUrl', '$event', 'widgetContext', 'entityId',
                          'entityName', 'additionalParams', 'entityLabel').subscribe(
                          {
                            next: (compiled) => {
                              try {
                                compiled.execute(imageUrl, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                              } catch (e) {
                                console.error(e);
                              }
                            },
                            error: (err) => {
                              console.error(err);
                            }
                          }
                        );
                      }
                      break;
                    case WidgetMobileActionType.deviceProvision:
                      const deviceName = actionResult.deviceName;
                      if (isNotEmptyTbFunction(mobileAction.handleProvisionSuccessFunction)) {
                        compileTbFunction(this.http, mobileAction.handleProvisionSuccessFunction, 'deviceName', '$event', 'widgetContext', 'entityId',
                          'entityName', 'additionalParams', 'entityLabel').subscribe(
                          {
                            next: (compiled) => {
                              try {
                                compiled.execute(deviceName, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                              } catch (e) {
                                console.error(e);
                              }
                            },
                            error: (err) => {
                              console.error(err);
                            }
                          }
                        );
                      }
                      break;
                    case WidgetMobileActionType.scanQrCode:
                      const code = actionResult.code;
                      const format = actionResult.format;
                      if (isNotEmptyTbFunction(mobileAction.processQrCodeFunction)) {
                        compileTbFunction(this.http, mobileAction.processQrCodeFunction, 'code', 'format', '$event', 'widgetContext', 'entityId',
                          'entityName', 'additionalParams', 'entityLabel').subscribe(
                          {
                            next: (compiled) => {
                              try {
                                compiled.execute(code, format, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                              } catch (e) {
                                console.error(e);
                              }
                            },
                            error: (err) => {
                              console.error(err);
                            }
                          }
                        );
                      }
                      break;
                    case WidgetMobileActionType.getLocation:
                      const latitude = actionResult.latitude;
                      const longitude = actionResult.longitude;
                      if (isNotEmptyTbFunction(mobileAction.processLocationFunction)) {
                        compileTbFunction(this.http, mobileAction.processLocationFunction, 'latitude', 'longitude', '$event', 'widgetContext', 'entityId',
                          'entityName', 'additionalParams', 'entityLabel').subscribe(
                          {
                            next: (compiled) => {
                              try {
                                compiled.execute(latitude, longitude, $event, this.widgetContext,
                                  entityId, entityName, additionalParams, entityLabel);
                              } catch (e) {
                                console.error(e);
                              }
                            },
                            error: (err) => {
                              console.error(err);
                            }
                          }
                        );
                      }
                      break;
                    case WidgetMobileActionType.mapDirection:
                    case WidgetMobileActionType.mapLocation:
                    case WidgetMobileActionType.makePhoneCall:
                      const launched = actionResult.launched;
                      if (isNotEmptyTbFunction(mobileAction.processLaunchResultFunction)) {
                        compileTbFunction(this.http, mobileAction.processLaunchResultFunction, 'launched', '$event', 'widgetContext', 'entityId',
                          'entityName', 'additionalParams', 'entityLabel').subscribe(
                          {
                            next: (compiled) => {
                              try {
                                compiled.execute(launched, $event, this.widgetContext,
                                  entityId, entityName, additionalParams, entityLabel);
                              } catch (e) {
                                console.error(e);
                              }
                            },
                            error: (err) => {
                              console.error(err);
                            }
                          }
                        );
                      }
                      break;
                  }
                } else {
                  if (isNotEmptyTbFunction(mobileAction.handleEmptyResultFunction)) {
                    compileTbFunction(this.http, mobileAction.handleEmptyResultFunction, '$event', 'widgetContext', 'entityId',
                      'entityName', 'additionalParams', 'entityLabel').subscribe(
                      {
                        next: (compiled) => {
                          try {
                            compiled.execute($event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
                          } catch (e) {
                            console.error(e);
                          }
                        },
                        error: (err) => {
                          console.error(err);
                        }
                      }
                    );
                  }
                }
              }
            }
          );
        },
        error: err => {
          let errorMessage: string;
          if (err && typeof err === 'string') {
            errorMessage = err;
          } else if (err && err.message) {
            errorMessage = err.message;
          }
          errorMessage = `Failed to get mobile action arguments${errorMessage ? `: ${errorMessage}` : '!'}`;
          this.handleWidgetMobileActionError(errorMessage, $event, mobileAction, entityId, entityName, additionalParams, entityLabel);
        }
      });
  }

  private handleWidgetMobileActionError(error: string, $event: Event, mobileAction: WidgetMobileActionDescriptor,
                                        entityId?: EntityId, entityName?: string, additionalParams?: any, entityLabel?: string) {
    if (isNotEmptyTbFunction(mobileAction.handleErrorFunction)) {
      compileTbFunction(this.http, mobileAction.handleErrorFunction, 'error', '$event', 'widgetContext', 'entityId',
        'entityName', 'additionalParams', 'entityLabel').subscribe(
        {
          next: (compiled) => {
            try {
              compiled.execute(error, $event, this.widgetContext, entityId, entityName, additionalParams, entityLabel);
            } catch (e) {
              console.error(e);
            }
          },
          error: (err) => {
            console.error(err);
          }
        }
      );
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
    const trigger = ($event.target || $event.currentTarget) as Element;
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
      const componentRef = this.popoverService.createPopoverRef(this.widgetContentContainer);
      const component = this.popoverService.displayPopoverWithComponentRef(componentRef, trigger, this.renderer,
        this.dashboardPageComponent, preferredPlacement, hideOnClickOutside,
        injector,
        {
          embedded: true,
          syncStateWithQueryParam: false,
          hideToolbar: hideDashboardToolbar,
          currentState: objToBase64([stateObject]),
          dashboard,
          parentDashboard: this.widgetContext.parentDashboard ?
            this.widgetContext.parentDashboard : this.widgetContext.dashboard,
          popoverComponent: componentRef.instance
        },
        {width: popoverWidth || '25vw', height: popoverHeight || '25vh'},
        popoverStyle,
        {}
      );
      this.widgetContext.registerPopoverComponent(component);
    }
  }

  private openDashboardStateInSeparateDialog(targetDashboardStateId: string, params?: StateParams, dialogTitle?: string,
                                             hideDashboardToolbar = true, dialogWidth?: number, dialogHeight?: number): MatDialogRef<any> {
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
    dashboard.dialogRef = this.dialog.open(this.embedDashboardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      viewContainerRef: this.widgetContentContainer,
      data: {
        dashboard,
        state: objToBase64([ stateObject ]),
        title,
        hideToolbar: hideDashboardToolbar,
        width: dialogWidth,
        height: dialogHeight,
        parentDashboard: this.widgetContext.parentDashboard ?
          this.widgetContext.parentDashboard : this.widgetContext.dashboard
      }
    });
    this.cd.markForCheck();
    return dashboard.dialogRef;
  }

  private elementClick($event: Event) {
    const elementClicked = ($event.target) as Element;
    const descriptors = this.getActionDescriptors('elementClick');
    if (descriptors.length) {
      const idsList = descriptors.map(descriptor => `#${descriptor.name}`).join(',');
      const targetElement = $(elementClicked).closest(idsList, this.widgetContext.$container[0]);
      if (targetElement.length && targetElement[0].id) {
        const descriptor = descriptors.find(descriptorInfo => descriptorInfo.name === targetElement[0].id);
        this.onWidgetAction($event, descriptor);
      }
    }
  }

  private cardClick($event: Event) {
    this.onClick($event, 'cardClick');
  }

  private click($event: Event) {
    this.onClick($event, 'click');
  }

  private onClick($event: Event, sourceId: string) {
    const descriptors = this.getActionDescriptors(sourceId);
    if (descriptors.length) {
      this.onWidgetAction($event, descriptors[0]);
    }
  }

  private onWidgetAction($event: Event, action: WidgetAction) {
    if ($event) {
      $event.stopPropagation();
    }
    const entityInfo = this.getActiveEntityInfo();
    const entityId = entityInfo ? entityInfo.entityId : null;
    const entityName = entityInfo ? entityInfo.entityName : null;
    const entityLabel = entityInfo && entityInfo.entityLabel ? entityInfo.entityLabel : null;
    this.handleWidgetAction($event, action, entityId, entityName, null, entityLabel);
  }

  private executeCustomPrettyAction($event: Event, descriptor: WidgetAction, entityId?: EntityId,
                                    entityName?: string, additionalParams?: any, entityLabel?: string) {
    const customPrettyFunction = descriptor.customFunction;
    const customHtml = descriptor.customHtml;
    const customCss = descriptor.customCss;
    const customResources = descriptor.customResources;
    const actionNamespace = `custom-action-pretty-${guid()}`;
    let htmlTemplate = '';
    if (isDefined(customHtml) && customHtml.length > 0) {
      htmlTemplate = customHtml;
    }
    this.loadCustomActionResources(actionNamespace, customCss, customResources, descriptor).subscribe({
      next: () => {
        if (isNotEmptyTbFunction(customPrettyFunction)) {
          compileTbFunction(this.http, customPrettyFunction, '$event', 'widgetContext', 'entityId',
            'entityName', 'htmlTemplate', 'additionalParams', 'entityLabel').subscribe({
            next: (compiled) => {
              try {
                if (!additionalParams) {
                  additionalParams = {};
                }
                this.widgetContext.customDialog.setAdditionalImports(descriptor.customImports);
                compiled.execute($event, this.widgetContext, entityId, entityName, htmlTemplate, additionalParams, entityLabel);
              } catch (e) {
                console.error(e);
              }
            },
            error: (err) => {
              console.error(err);
            }
          });
        }
      },
      error: (errorMessages: string[]) => {
        this.processResourcesLoadErrors(errorMessages);
      }
    });
  }

  private loadCustomActionResources(actionNamespace: string, customCss: string, customResources: Array<WidgetResource>,
                                    actionDescriptor: WidgetAction): Observable<any> {
    const resourceTasks: Observable<string>[] = [];
    const modulesTasks: Observable<ModulesWithComponents | string>[] = [];

    if (isDefined(customCss) && customCss.length > 0) {
      this.cssParser.cssPreviewNamespace = actionNamespace;
      this.cssParser.createStyleElement(actionNamespace, customCss, 'nonamespace');
    }

    if (isDefined(customResources) && customResources.length > 0) {
      customResources.forEach(resource => {
        if (resource.isModule) {
          modulesTasks.push(
            this.resources.loadModulesWithComponents(resource.url, this.modulesMap).pipe(
              catchError((e: Error) => of(e?.message ? e.message : `Failed to load custom action resource module: '${resource.url}'`))
            )
          );
        } else {
          resourceTasks.push(
            this.resources.loadResource(resource.url).pipe(
              catchError(() => of(`Failed to load custom action resource: '${resource.url}'`))
            )
          );
        }
      });

      if (modulesTasks.length) {
        const importsObservable: Observable<string | Type<any>[]> = forkJoin(modulesTasks).pipe(
          map(res => {
            const msg = res.find(r => typeof r === 'string');
            if (msg) {
              return msg as string;
            } else {
              const modulesWithComponents = flatModulesWithComponents(res as ModulesWithComponents[]);
              return modulesWithComponentsToTypes(modulesWithComponents);
            }
          })
        );

        resourceTasks.push(importsObservable.pipe(
          map((resolvedImports) => {
            if (typeof resolvedImports === 'string') {
              return resolvedImports;
            } else {
              actionDescriptor.customImports = resolvedImports;
              return null;
            }
          })));
      }

      return forkJoin(resourceTasks).pipe(
        switchMap(msgs => {
          const errors = msgs.filter(msg => msg && msg.length > 0);
          if (errors.length > 0) {
            return throwError(() => errors);
          } else {
            return of(null);
          }}
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

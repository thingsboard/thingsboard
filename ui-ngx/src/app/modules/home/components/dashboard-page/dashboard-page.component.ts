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
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  Inject,
  Injector,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  Optional,
  Renderer2,
  StaticProvider,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation,
  DOCUMENT
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router, UrlTree } from '@angular/router';
import { UtilsService } from '@core/services/utils.service';
import {
  BreakpointId,
  BreakpointInfo,
  Dashboard,
  DashboardConfiguration,
  DashboardLayoutId,
  DashboardLayoutInfo,
  DashboardLayoutsInfo,
  DashboardState,
  DashboardStateLayouts,
  GridSettings,
  LayoutDimension,
  LayoutType,
  ViewFormatType,
  WidgetLayout
} from '@app/shared/models/dashboard.models';
import { WINDOW } from '@core/services/window.service';
import { WindowMessage } from '@shared/models/window-message.model';
import { deepClone, guid, isDefined, isDefinedAndNotNull, isNotEmptyStr } from '@app/core/utils';
import {
  DashboardContext,
  DashboardPageInitData,
  DashboardPageLayout,
  DashboardPageLayoutContext,
  DashboardPageLayouts,
  DashboardPageScope,
  IDashboardController,
  LayoutWidgetsArray
} from './dashboard-page.models';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import {
  Widget,
  WidgetConfig,
  WidgetInfo,
  WidgetPosition,
  widgetType,
  widgetTypesData
} from '@shared/models/widget.models';
import { environment as env } from '@env/environment';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { EntityService } from '@core/http/entity.service';
import { AliasController } from '@core/api/alias-controller';
import { BehaviorSubject, Observable, of, Subject, Subscription, throwError } from 'rxjs';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { DashboardService } from '@core/http/dashboard.service';
import {
  DashboardContextMenuItem,
  IDashboardComponent,
  WidgetContextMenuItem
} from '../../models/dashboard-component.models';
import { WidgetComponentService } from '../../components/widget/widget-component.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { EntityAliases } from '@app/shared/models/alias.models';
import { EditWidgetComponent } from '@home/components/dashboard-page/edit-widget.component';
import {
  AddWidgetDialogComponent,
  AddWidgetDialogData
} from '@home/components/dashboard-page/add-widget-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {
  ManageDashboardLayoutsDialogComponent,
  ManageDashboardLayoutsDialogData
} from '@home/components/dashboard-page/layout/manage-dashboard-layouts-dialog.component';
import { SelectTargetLayoutDialogComponent } from '@home/components/dashboard/select-target-layout-dialog.component';
import {
  DashboardSettingsDialogComponent,
  DashboardSettingsDialogData
} from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import {
  ManageDashboardStatesDialogComponent,
  ManageDashboardStatesDialogData,
  ManageDashboardStatesDialogResult
} from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { AuthState } from '@app/core/auth/auth.models';
import { FiltersDialogComponent, FiltersDialogData } from '@home/components/filter/filters-dialog.component';
import { Filters } from '@shared/models/query/query.models';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  DISPLAY_WIDGET_TYPES_PANEL_DATA,
  DisplayWidgetTypesPanelComponent,
  DisplayWidgetTypesPanelData
} from '@home/components/dashboard-page/widget-types-panel.component';
import { DashboardWidgetSelectComponent } from '@home/components/dashboard-page/dashboard-widget-select.component';
import { MobileService } from '@core/services/mobile.service';

import {
  DashboardImageDialogComponent,
  DashboardImageDialogData,
  DashboardImageDialogResult
} from '@home/components/dashboard-page/dashboard-image-dialog.component';
import { SafeUrl } from '@angular/platform-browser';
import cssjs from '@core/css/css';

import { IAliasController } from '@core/api/widget-api.models';
import { MatButton, MatIconButton } from '@angular/material/button';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { catchError, distinctUntilChanged, map, skip, tap } from 'rxjs/operators';
import { LayoutFixedSize, LayoutWidthType } from '@home/components/dashboard-page/layout/layout.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import {
  MoveWidgetsDialogComponent,
  MoveWidgetsDialogResult
} from '@home/components/dashboard-page/layout/move-widgets-dialog.component';
import { HttpStatusCode } from '@angular/common/http';

// @dynamic
@Component({
    selector: 'tb-dashboard-page',
    templateUrl: './dashboard-page.component.html',
    styleUrls: ['./dashboard-page.component.scss'],
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DashboardPageComponent extends PageComponent implements IDashboardController, HasDirtyFlag, OnInit, AfterViewInit, OnDestroy {

  LayoutType = LayoutType;

  private destroyed = false;

  private forcePristine = false;

  get isDirty(): boolean {
    return this.isEdit && !this.forcePristine;
  }

  set isDirty(value: boolean) {
    this.forcePristine = !value;
  }

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  @HostBinding('class')
  dashboardPageClass: string;

  @Input()
  embedded = false;

  @Input()
  currentState: string;

  private hideToolbarValue = false;

  @Input()
  set hideToolbar(hideToolbar: boolean) {
    this.hideToolbarValue = hideToolbar;
  }

  get hideToolbar(): boolean {
    return ((this.hideToolbarValue || this.hideToolbarSetting()) && !this.isEdit) || (this.isEditingWidget || this.isAddingWidget);
  }

  @Input()
  syncStateWithQueryParam = true;

  @Input()
  dashboard: Dashboard;
  dashboardConfiguration: DashboardConfiguration;

  @Input()
  parentDashboard?: IDashboardComponent = null;

  @Input()
  popoverComponent?: TbPopoverComponent = null;

  @Input()
  parentAliasController?: IAliasController = null;

  @ViewChild('dashboardContainer') dashboardContainer: ElementRef<HTMLElement>;

  @ViewChild('dashboardContent', {read: ElementRef}) dashboardContent: ElementRef<HTMLElement>;

  prevDashboard: Dashboard;

  iframeMode = this.utils.iframeMode;
  widgetEditMode: boolean;
  singlePageMode: boolean;
  forceFullscreen = this.authState.forceFullscreen;

  readonly = false;
  isMobileApp = this.mobileService.isMobileApp();
  isFullscreen = false;
  isEdit = false;
  isEditingWidget = false;
  isEditingWidgetClosed = true;
  isMobile = !this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
  forceDashboardMobileMode = false;
  isAddingWidget = false;
  isAddingWidgetClosed = true;
  filterWidgetTypes: widgetType[] = null;

  isToolbarOpened = false;
  isToolbarOpenedAnimate = false;
  isRightLayoutOpened = false;

  editingWidget: Widget = null;
  editingWidgetLayout: WidgetLayout = null;
  editingWidgetOriginal: Widget = null;
  editingWidgetLayoutOriginal: WidgetLayout = null;
  editingWidgetSubtitle: string = null;
  editingLayoutCtx: DashboardPageLayoutContext = null;

  thingsboardVersion: string = env.tbVersion;

  translatedDashboardTitle: string;

  currentDashboardId: string;
  currentCustomerId: string;
  currentDashboardScope: DashboardPageScope;

  setStateDashboardId = false;

  addingLayoutCtx: DashboardPageLayoutContext;

  mainLayoutSize: {width: string; height: string; maxWidth: string; minWidth: string} =
    {width: '100%', height: '100%', maxWidth: '100%', minWidth: '100%'};
  rightLayoutSize: {width: string; height: string} = {width: '100%', height: '100%'};

  dashboardLogoLink = this.getDashboardLogoLink();

  private dashboardLogoCache: SafeUrl;
  private defaultDashboardLogo = 'assets/logo_title_white.svg';

  private dashboardResize$: ResizeObserver;

  dashboardCtx: DashboardContext = {
    instanceId: this.utils.guid(),
    getDashboard: () => this.dashboard,
    dashboardTimewindow: null,
    state: null,
    breakpoint: null,
    stateController: null,
    stateChanged: null,
    stateId: null,
    aliasController: null,
    runChangeDetection: this.runChangeDetection.bind(this)
  };

  layouts: DashboardPageLayouts = {
    main: {
      show: false,
      layoutCtx: {
        id: 'main',
        breakpoint: 'default',
        widgets: null,
        widgetLayouts: {},
        gridSettings: {},
        ignoreLoading: true,
        ctrl: null,
        dashboardCtrl: this,
        displayGrid: 'onDrag&Resize',
        layoutData: null,
        layoutDataChanged: new BehaviorSubject(null),
      }
    },
    right: {
      show: false,
      layoutCtx: {
        id: 'right',
        breakpoint: 'default',
        widgets: null,
        widgetLayouts: {},
        gridSettings: {},
        ignoreLoading: true,
        ctrl: null,
        dashboardCtrl: this,
        displayGrid: 'onDrag&Resize',
        layoutData: null,
        layoutDataChanged: new BehaviorSubject(null),
      }
    }
  };

  updateBreadcrumbs = new EventEmitter();

  private rxSubscriptions = new Array<Subscription>();

  get toolbarOpened(): boolean {
    return !this.widgetEditMode && !this.hideToolbar &&
      (this.toolbarAlwaysOpen() || this.isToolbarOpened || this.isEdit || this.showRightLayoutSwitch());
  }

  set toolbarOpened(toolbarOpened: boolean) {
  }

  get rightLayoutOpened(): boolean {
    return !this.isMobile || this.isRightLayoutOpened;
  }
  set rightLayoutOpened(rightLayoutOpened: boolean) {
  }

  get mobileDisplayRightLayoutFirst(): boolean {
    return this.isMobile && this.layouts.right.layoutCtx.gridSettings?.mobileDisplayLayoutFirst;
  }

  set mobileDisplayRightLayoutFirst(mobileDisplayRightLayoutFirst: boolean) {
  }

  @ViewChild('tbEditWidget') editWidgetComponent: EditWidgetComponent;

  @ViewChild('dashboardWidgetSelect') dashboardWidgetSelectComponent: DashboardWidgetSelectComponent;

  private changeMobileSize = new Subject<boolean>();

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              @Inject(DOCUMENT) private document: Document,
              private breakpointObserver: BreakpointObserver,
              private route: ActivatedRoute,
              private router: Router,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private widgetComponentService: WidgetComponentService,
              private dashboardService: DashboardService,
              private itembuffer: ItemBufferService,
              private importExport: ImportExportService,
              private mobileService: MobileService,
              private dialog: MatDialog,
              public translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private ngZone: NgZone,
              @Optional() @Inject('embeddedValue') private embeddedValue,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              public elRef: ElementRef,
              private injector: Injector) {
    super(store);
    if (isDefinedAndNotNull(this.embeddedValue)) {
      this.embedded = this.embeddedValue;
    }
  }

  ngOnInit() {
    this.rxSubscriptions.push(this.route.data.subscribe(
      (data) => {
        let dashboardPageInitData: DashboardPageInitData;
        if (this.embedded) {
          dashboardPageInitData = {
            dashboard: this.dashboardUtils.validateAndUpdateDashboard(this.dashboard),
            currentDashboardId: this.dashboard.id ? this.dashboard.id.id : null,
            widgetEditMode: false,
            singlePageMode: false
          };
        } else {
          dashboardPageInitData = {
            dashboard: data.dashboard,
            currentDashboardId: this.route.snapshot.params.dashboardId,
            widgetEditMode: data.widgetEditMode,
            singlePageMode: data.singlePageMode
          };
        }
        this.init(dashboardPageInitData);
        this.runChangeDetection();
      }
    ));
    if (this.syncStateWithQueryParam) {
      this.rxSubscriptions.push(this.route.queryParamMap.subscribe(
        (paramMap) => {
          if (paramMap.has('reload')) {
            this.dashboardCtx.aliasController.updateAliases();
            setTimeout(() => {
              this.mobileService.handleDashboardStateName(this.dashboardCtx.stateController.getCurrentStateName());
              this.mobileService.onDashboardLoaded(this.layouts.right.show, this.isRightLayoutOpened);
            });
          }
        }
      ));
    }
    this.rxSubscriptions.push(
      this.changeMobileSize.pipe(
        distinctUntilChanged(),
      ).subscribe((state) => {
        this.isMobile = state;
        this.updateLayoutSizes();
      })
    );

    this.rxSubscriptions.push(
      this.breakpointObserver.observe(
        this.dashboardUtils.getBreakpoints()
      ).pipe(
        map(value => this.parseBreakpointsResponse(value.breakpoints)),
        tap((value) => {
          this.dashboardCtx.breakpoint = value ? value.id : 'default';
          this.changeMobileSize.next(value ? this.isMobileSize(value) : false);
        }),
        distinctUntilChanged((_, next) => {
          if (this.layouts.right.show || this.isEdit) {
            return true;
          }
          let nextBreakpointConfiguration: BreakpointId = 'default';
          if (next && !!this.layouts.main.layoutCtx.layoutData?.[next.id]) {
            nextBreakpointConfiguration = next.id;
          }
          return this.layouts.main.layoutCtx.breakpoint === nextBreakpointConfiguration;
        }),
        skip(1)
      ).subscribe(() => {
          this.dashboardUtils.updatedLayoutForBreakpoint(this.layouts.main, this.dashboardCtx.breakpoint);
          this.updateLayoutSizes();
        }
      )
    );

    if (this.isMobileApp && this.syncStateWithQueryParam) {
      this.mobileService.registerToggleLayoutFunction(() => {
        setTimeout(() => {
          this.toggleLayouts();
          this.cd.detectChanges();
        });
      });
    }
  }

  ngAfterViewInit() {
    this.dashboardResize$ = new ResizeObserver(() => {
      this.ngZone.run(() => {
        this.updateLayoutSizes();
      });
    });
    this.dashboardResize$.observe(this.dashboardContainer.nativeElement);
    if (!this.widgetEditMode && !this.readonly && this.dashboardUtils.isEmptyDashboard(this.dashboard)) {
      this.setEditMode(true, false);
    }
  }

  private init(data: DashboardPageInitData) {

    this.reset();

    this.dashboard = data.dashboard;
    this.translatedDashboardTitle = this.getTranslatedDashboardTitle();
    if (!this.embedded && this.dashboard.id) {
      this.setStateDashboardId = true;
    }

    if (this.route.snapshot.queryParamMap.has('hideToolbar')) {
      this.hideToolbar = this.route.snapshot.queryParamMap.get('hideToolbar') === 'true';
    }

    if (this.route.snapshot.queryParamMap.has('embedded')) {
      this.embedded = this.route.snapshot.queryParamMap.get('embedded') === 'true';
    }

    this.currentDashboardId = data.currentDashboardId;

    if (this.route.snapshot.params.customerId) {
      this.currentCustomerId = this.route.snapshot.params.customerId;
      this.currentDashboardScope = 'customer';
    } else {
      this.currentDashboardScope = this.authUser.authority === Authority.TENANT_ADMIN ? 'tenant' : 'customer';
      this.currentCustomerId = this.authUser.customerId;
    }

    this.dashboardConfiguration = this.dashboard.configuration;
    this.dashboardCtx.dashboardTimewindow = this.dashboardConfiguration.timewindow;
    this.layouts.main.layoutCtx.widgets = new LayoutWidgetsArray(this.dashboardCtx);
    this.layouts.right.layoutCtx.widgets = new LayoutWidgetsArray(this.dashboardCtx);
    this.widgetEditMode = data.widgetEditMode;
    this.singlePageMode = data.singlePageMode;

    this.readonly = this.embedded || (this.singlePageMode && !this.widgetEditMode && !this.route.snapshot.queryParamMap.get('edit'))
                    || this.forceFullscreen || this.isMobileApp || this.authUser.authority === Authority.CUSTOMER_USER ||
                    this.route.snapshot.queryParamMap.get('readonly') === 'true';

    this.dashboardCtx.aliasController = this.parentAliasController ? this.parentAliasController : new AliasController(this.utils,
      this.entityService,
      this.translate,
      () => this.dashboardCtx.stateController,
      this.dashboardConfiguration.entityAliases,
      this.dashboardConfiguration.filters,
      this.parentDashboard?.aliasController.getUserFilters());

    this.updateDashboardCss();

    if (this.widgetEditMode) {
      const message: WindowMessage = {
        type: 'widgetEditModeInited'
      };
      this.window.parent.postMessage(JSON.stringify(message), '*');
    }
  }

  private updateDashboardCss() {
    this.cleanupDashboardCss();
    const cssString = this.dashboardConfiguration.settings.dashboardCss;
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      cssParser.testMode = false;
      this.dashboardPageClass  = 'tb-dashboard-page-css-' + guid();
      this.dashboardCtx.dashboardCssClass = this.dashboardPageClass;
      cssParser.cssPreviewNamespace = 'tb-default .' + this.dashboardPageClass;
      cssParser.createStyleElement(this.dashboardPageClass, cssString);
    }
  }

  private cleanupDashboardCss() {
    if (this.dashboardPageClass) {
      const el = this.document.getElementById(this.dashboardPageClass);
      if (el) {
        el.parentNode.removeChild(el);
      }
    }
  }

  private reset() {
    this.dashboard = null;
    this.translatedDashboardTitle = null;
    this.dashboardConfiguration = null;
    this.dashboardLogoCache = undefined;
    this.prevDashboard = null;

    this.widgetEditMode = false;
    this.singlePageMode = false;

    this.isFullscreen = false;
    this.isEdit = false;
    this.isEditingWidget = false;
    this.isEditingWidgetClosed = true;
    this.forceDashboardMobileMode = false;
    this.isAddingWidget = false;
    this.isAddingWidgetClosed = true;

    this.isToolbarOpened = false;
    this.isToolbarOpenedAnimate = false;
    this.isRightLayoutOpened = false;

    this.editingWidget = null;
    this.editingWidgetLayout = null;
    this.editingWidgetOriginal = null;
    this.editingWidgetLayoutOriginal = null;
    this.editingWidgetSubtitle = null;
    this.editingLayoutCtx = null;

    this.currentDashboardId = null;
    this.currentCustomerId = null;
    this.currentDashboardScope = null;

    this.setStateDashboardId = false;

    this.dashboardCtx.state = null;
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.cleanupDashboardCss();
    if (this.isMobileApp && this.syncStateWithQueryParam) {
      this.mobileService.unregisterToggleLayoutFunction();
    }
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
    if (this.dashboardResize$) {
      this.dashboardResize$.disconnect();
    }
    this.layouts.main.layoutCtx.layoutDataChanged.unsubscribe();
    this.layouts.right.layoutCtx.layoutDataChanged.unsubscribe();
  }

  public runChangeDetection() {
    this.ngZone.run(() => {
      this.cd.detectChanges();
    });
  }

  public openToolbar() {
    this.isToolbarOpenedAnimate = true;
    this.isToolbarOpened = true;
  }

  public closeToolbar() {
    this.isToolbarOpenedAnimate = true;
    this.isToolbarOpened = false;
  }

  public showCloseToolbar() {
    return !this.toolbarAlwaysOpen() && !this.isEdit && !this.showRightLayoutSwitch();
  }

  public hideFullscreenButton(): boolean {
    return (this.widgetEditMode || this.iframeMode || this.forceFullscreen || this.singlePageMode);
  }

  public toolbarAlwaysOpen(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.toolbarAlwaysOpen)) {
      return this.dashboard.configuration.settings.toolbarAlwaysOpen;
    } else {
      return true;
    }
  }

  private hideToolbarSetting(): boolean {
    if (isDefined(this.dashboard.configuration?.settings?.hideToolbar)) {
      const canApplyHideSetting = !this.forceFullscreen || this.isMobileApp || this.isPublicUser();
      return this.dashboard.configuration.settings.hideToolbar && canApplyHideSetting;
    } else {
      return false;
    }
  }

  public displayTitle(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showTitle)) {
      return this.dashboard.configuration.settings.showTitle;
    } else {
      return false;
    }
  }

  private getTranslatedDashboardTitle(): string {
    return this.utils.customTranslation(this.dashboard.title, this.dashboard.title);
  }

  public displayExport(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showDashboardExport)) {
      return this.dashboard.configuration.settings.showDashboardExport;
    } else {
      return true;
    }
  }

  public displayUpdateDashboardImage(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showUpdateDashboardImage)) {
      return this.dashboard.configuration.settings.showUpdateDashboardImage;
    } else {
      return true;
    }
  }

  public displayDashboardTimewindow(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showDashboardTimewindow)) {
      return this.dashboard.configuration.settings.showDashboardTimewindow;
    } else {
      return true;
    }
  }

  public displayDashboardsSelect(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showDashboardsSelect)) {
      return this.dashboard.configuration.settings.showDashboardsSelect;
    } else {
      return true;
    }
  }

  public displayEntitiesSelect(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showEntitiesSelect)) {
      return this.dashboard.configuration.settings.showEntitiesSelect;
    } else {
      return true;
    }
  }

  public displayFilters(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showFilters)) {
      return this.dashboard.configuration.settings.showFilters;
    } else {
      return true;
    }
  }

  public showDashboardLogo(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showDashboardLogo)) {
      return this.dashboard.configuration.settings.showDashboardLogo && (this.forceFullscreen || this.singlePageMode || this.isFullscreen);
    } else {
      return false;
    }
  }

  public get dashboardLogo(): string {
    return this.dashboard.configuration.settings.dashboardLogoUrl || this.defaultDashboardLogo;
  }

  public showRightLayoutSwitch(): boolean {
    return this.isMobile && !this.isMobileApp && this.layouts.right.show;
  }

  public toggleLayouts() {
    this.isRightLayoutOpened = !this.isRightLayoutOpened;
    this.mobileService.onDashboardRightLayoutChanged(this.isRightLayoutOpened);
  }

  public openRightLayout() {
    this.isRightLayoutOpened = true;
    this.mobileService.onDashboardRightLayoutChanged(this.isRightLayoutOpened);
  }

  public updateLayoutSizes() {
    let changeMainLayoutSize = false;
    let changeRightLayoutSize = false;
    if (this.dashboardCtx.state) {
      changeMainLayoutSize = this.updateMainLayoutSize();
      changeRightLayoutSize = this.updateRightLayoutSize();
    }
    if (changeMainLayoutSize || changeRightLayoutSize) {
      this.cd.markForCheck();
    }
  }

  private updateMainLayoutSize(): boolean {
    const prevMainLayoutWidth = this.mainLayoutSize.width;
    const prevMainLayoutHeight = this.mainLayoutSize.height;
    const prevMainLayoutMaxWidth = this.mainLayoutSize.maxWidth;
    const prevMainLayoutMinWidth = this.mainLayoutSize.minWidth;
    if (this.isEditingWidget && this.editingLayoutCtx.id === 'main') {
      this.mainLayoutSize.width = '100%';
    } else {
      this.mainLayoutSize.width = this.layouts.right.show && !this.isMobile ? this.calculateWidth('main') : '100%';
    }
    if (!this.isEditingWidget || this.editingLayoutCtx.id === 'main') {
      this.mainLayoutSize.height = '100%';
    } else {
      this.mainLayoutSize.height = '0px';
    }
    if (this.isEdit && !this.isEditingWidget) {
      const xOffset = this.dashboardContainer.nativeElement.getBoundingClientRect().x;
      const breakpoint = this.dashboardUtils.getBreakpointInfoById(this.layouts.main.layoutCtx.breakpoint);

      let maxWidth = '100%';
      let minWidth: string;

      if (breakpoint) {
        const isMobile = this.isMobileSize(breakpoint);
        if (breakpoint.maxWidth) {
          maxWidth = isMobile ? `${breakpoint.maxWidth}px` : `${breakpoint.maxWidth - xOffset}px`;
        }
        if (breakpoint.minWidth) {
          minWidth = isMobile ? `${breakpoint.minWidth}px` : `${breakpoint.minWidth - xOffset}px`;
        }
      }

      this.mainLayoutSize.maxWidth = maxWidth;
      this.mainLayoutSize.minWidth = minWidth;
    } else {
      this.mainLayoutSize.maxWidth = '100%';
      this.mainLayoutSize.minWidth = undefined;
    }
    return prevMainLayoutWidth !== this.mainLayoutSize.width || prevMainLayoutHeight !== this.mainLayoutSize.height ||
      prevMainLayoutMaxWidth !== this.mainLayoutSize.maxWidth || prevMainLayoutMinWidth !== this.mainLayoutSize.minWidth;
  }

  private updateRightLayoutSize(): boolean {
    const prevRightLayoutWidth = this.rightLayoutSize.width;
    const prevRightLayoutHeight = this.rightLayoutSize.height;
    if (this.isEditingWidget && this.editingLayoutCtx.id === 'right') {
      this.rightLayoutSize.width = '100%';
    } else {
      this.rightLayoutSize.width = this.isMobile ? '100%' : this.calculateWidth('right');
    }
    if (!this.isEditingWidget || this.editingLayoutCtx.id === 'right') {
      this.rightLayoutSize.height = '100%';
    } else {
      this.rightLayoutSize.height = '0px';
    }
    return prevRightLayoutWidth !== this.rightLayoutSize.width || prevRightLayoutHeight !== this.rightLayoutSize.height;
  }

  private calculateWidth(layout: DashboardLayoutId): string {
    let layoutDimension: LayoutDimension;
    const mainLayout = this.dashboard.configuration.states[this.dashboardCtx.state].layouts.main;
    const rightLayout = this.dashboard.configuration.states[this.dashboardCtx.state].layouts.right;
    if (rightLayout) {
      if (mainLayout.gridSettings.layoutDimension) {
        layoutDimension = mainLayout.gridSettings.layoutDimension;
      } else {
        layoutDimension = rightLayout.gridSettings.layoutDimension;
      }
    }
    if (layoutDimension) {
      if (layoutDimension.type === LayoutWidthType.PERCENTAGE) {
        if (layout === 'right') {
          return (100 - layoutDimension.leftWidthPercentage) + '%';
        } else {
          return layoutDimension.leftWidthPercentage + '%';
        }
      } else {
        const dashboardWidth = this.dashboardContainer.nativeElement.getBoundingClientRect().width;
        const minAvailableWidth = dashboardWidth - LayoutFixedSize.MIN;
        if (layoutDimension.fixedLayout === layout) {
          if (minAvailableWidth <= layoutDimension.fixedWidth) {
            return minAvailableWidth + 'px';
          } else {
            return layoutDimension.fixedWidth + 'px';
          }
        } else {
          if (minAvailableWidth <= layoutDimension.fixedWidth) {
            return LayoutFixedSize.MIN + 'px';
          } else {
            return (dashboardWidth - layoutDimension.fixedWidth) + 'px';
          }
        }
      }
    } else {
      return '50%';
    }
  }

  public isPublicUser(): boolean {
    return this.authUser.isPublic;
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  public isSystemAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  public exportDashboard($event: Event) {
    if ($event) {
      $event.preventDefault();
    }
    this.importExport.exportDashboard(this.currentDashboardId);
  }

  public openEntityAliases($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases: deepClone(this.dashboard.configuration.entityAliases),
        widgets: this.dashboardUtils.getWidgetsArray(this.dashboard),
        isSingleEntityAlias: false
      }
    }).afterClosed().subscribe((entityAliases) => {
      if (entityAliases) {
        this.dashboard.configuration.entityAliases = entityAliases;
        this.entityAliasesUpdated();
      }
    });
  }

  public openFilters($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<FiltersDialogComponent, FiltersDialogData,
      Filters>(FiltersDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filters: deepClone(this.dashboard.configuration.filters),
        widgets: this.dashboardUtils.getWidgetsArray(this.dashboard),
        isSingleFilter: false
      }
    }).afterClosed().subscribe((filters) => {
      if (filters) {
        this.dashboard.configuration.filters = filters;
        this.filtersUpdated();
      }
    });
  }

  public openDashboardSettings($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    let gridSettings: GridSettings = null;
    const layoutKeys = this.dashboardUtils.isSingleLayoutDashboard(this.dashboard);
    if (layoutKeys) {
      const layouts = this.dashboardUtils.getDashboardLayoutConfig(
        this.dashboard.configuration.states[layoutKeys.state].layouts[layoutKeys.layout],
        this.layouts[layoutKeys.layout].layoutCtx.breakpoint);
      gridSettings = deepClone(layouts.gridSettings);
    }
    this.dialog.open<DashboardSettingsDialogComponent, DashboardSettingsDialogData,
      DashboardSettingsDialogData>(DashboardSettingsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        settings: deepClone(this.dashboard.configuration.settings),
        gridSettings,
        breakpointId: this.layouts.main.layoutCtx.breakpoint
      }
    }).afterClosed().subscribe((data) => {
      if (data) {
        this.dashboard.configuration.settings = data.settings;
        this.dashboardLogoCache = undefined;
        this.updateDashboardCss();
        const newGridSettings = data.gridSettings;
        if (newGridSettings) {
          const layouts = deepClone(this.dashboard.configuration.states[layoutKeys.state].layouts);
          const layoutConfig = this.dashboardUtils.getDashboardLayoutConfig(
            layouts[layoutKeys.layout], this.layouts[layoutKeys.layout].layoutCtx.breakpoint);
          this.dashboardUtils.updateLayoutSettings(layoutConfig, newGridSettings);
          this.updateDashboardLayouts(layouts);
       }
      }
    });
  }

  public manageDashboardStates($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ManageDashboardStatesDialogComponent, ManageDashboardStatesDialogData,
      ManageDashboardStatesDialogResult>(ManageDashboardStatesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        states: deepClone(this.dashboard.configuration.states),
        widgets: this.dashboard.configuration.widgets as {[id: string]: Widget}
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        if (result.addWidgets) {
          Object.assign(this.dashboard.configuration.widgets, result.addWidgets);
        }
        if (result.states) {
          this.updateStates(result.states);
        }
      }
    });
  }

  public manageDashboardLayouts($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ManageDashboardLayoutsDialogComponent, ManageDashboardLayoutsDialogData,
      DashboardStateLayouts>(ManageDashboardLayoutsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        layouts: deepClone(this.dashboard.configuration.states[this.dashboardCtx.state].layouts)
      }
    }).afterClosed().subscribe((layouts) => {
      if (layouts) {
        this.updateDashboardLayouts(layouts);
      }
    });
  }

  private moveWidgets($event: Event, layoutId: DashboardLayoutId, breakpointId: BreakpointId) {
    if ($event) {
      $event.stopPropagation();
    }
    this.layouts[layoutId].layoutCtx.displayGrid = 'always';
    this.cd.markForCheck();
    this.dialog.open<MoveWidgetsDialogComponent, any,
      MoveWidgetsDialogResult>(MoveWidgetsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe((result) => {
      this.layouts[layoutId].layoutCtx.displayGrid = 'onDrag&Resize';
      if (result) {
        const dashboardLayout = this.dashboardConfiguration.states[this.dashboardCtx.state].layouts[layoutId];
        const targetLayout = this.dashboardUtils.getDashboardLayoutConfig(dashboardLayout, breakpointId);
        this.dashboardUtils.moveWidgets(targetLayout, result.cols, result.rows);
        this.updateLayouts();
      } else {
        this.cd.markForCheck();
      }
    });
  }

  private updateDashboardLayouts(newLayouts: DashboardStateLayouts) {
    this.dashboardUtils.setLayouts(this.dashboard, this.dashboardCtx.state, newLayouts);
    this.updateLayouts();
  }

  private updateStates(states: {[id: string]: DashboardState }) {
    this.dashboard.configuration.states = states;
    this.dashboardUtils.removeUnusedWidgets(this.dashboard);
    let targetState = this.dashboardCtx.state;
    if (!this.dashboard.configuration.states[targetState]) {
      targetState = this.dashboardUtils.getRootStateId(this.dashboardConfiguration.states);
    }
    this.openDashboardState(targetState);
  }

  public importWidget($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.importWidget(this.dashboard, this.dashboardCtx.state,
      this.editMissingAliases.bind(this),
      this.selectTargetLayout.bind(this), this.entityAliasesUpdated.bind(this), this.filtersUpdated.bind(this)).subscribe(
      (importData) => {
        if (importData) {
          if (this.isAddingWidget) {
            this.onAddWidgetClosed();
            this.isAddingWidgetClosed = true;
          }
          const widget = importData.widget;
          const layoutId = importData.layoutId;
          this.layouts[layoutId].layoutCtx.widgets.addWidgetId(widget.id);
          this.runChangeDetection();
        }
      }
    );
  }

  private editMissingAliases(widgets: Array<Widget>, isSingleWidget: boolean,
                             customTitle: string, missingEntityAliases: EntityAliases): Observable<EntityAliases> {
    return this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases: missingEntityAliases,
        widgets,
        customTitle,
        isSingleWidget,
        disableAdd: true
      }
    }).afterClosed().pipe(
      map((updatedEntityAliases) => {
          if (updatedEntityAliases) {
            return updatedEntityAliases;
          } else {
            throw new Error('Unable to resolve missing entity aliases!');
          }
        }
      ));
  }

  public currentDashboardIdChanged(dashboardId: string) {
    if (!this.widgetEditMode) {
      this.dashboardCtx.stateController.cleanupPreservedStates();
      if (this.currentDashboardScope === 'customer' && this.authUser.authority === Authority.TENANT_ADMIN) {
        this.router.navigateByUrl(`customers/${this.currentCustomerId}/dashboards/${dashboardId}`);
      } else {
        if (this.singlePageMode) {
          this.router.navigateByUrl(`dashboard/${dashboardId}`);
        } else {
          this.router.navigateByUrl(`dashboards/${dashboardId}`);
        }
      }
    }
  }

  public toggleDashboardEditMode() {
    this.setEditMode(!this.isEdit, true);
    this.notifyDashboardToggleEditMode();
  }

  public saveDashboard() {
    this.translatedDashboardTitle = this.getTranslatedDashboardTitle();
    this.notifyDashboardUpdated();
  }

  public openDashboardState(state: string, openRightLayout?: boolean) {
    if (!this.destroyed) {
      const layoutsData = this.dashboardUtils.getStateLayoutsData(this.dashboard, state);
      if (layoutsData) {
        this.dashboardCtx.state = state;
        this.dashboardCtx.aliasController.dashboardStateChanged();
        this.isRightLayoutOpened = openRightLayout ? true : false;
        this.updateLayouts(layoutsData);
        this.cd.markForCheck();
      }
      setTimeout(() => {
        this.mobileService.onDashboardLoaded(this.layouts.right.show, this.isRightLayoutOpened);
      });
    }
  }

  private updateLayouts(layoutsData?: DashboardLayoutsInfo) {
    if (!layoutsData) {
      layoutsData = this.dashboardUtils.getStateLayoutsData(this.dashboard, this.dashboardCtx.state);
    }
    for (const l of Object.keys(this.layouts)) {
      const layout: DashboardPageLayout = this.layouts[l];
      if (layoutsData[l]) {
        layout.show = true;
        const layoutInfo: DashboardLayoutInfo = layoutsData[l];
        this.updateLayout(layout, layoutInfo);
      } else {
        layout.show = false;
        this.updateLayout(layout, {default: {widgetIds: [], widgetLayouts: {}, gridSettings: null}});
      }
    }
  }

  private updateLayout(layout: DashboardPageLayout, layoutInfo: DashboardLayoutInfo) {
    layout.layoutCtx.layoutData = layoutInfo;
    layout.layoutCtx.layoutDataChanged.next();
    this.dashboardUtils.updatedLayoutForBreakpoint(layout, this.isEdit ? layout.layoutCtx.breakpoint : this.dashboardCtx.breakpoint);
    this.updateLayoutSizes();
  }

  private setEditMode(isEdit: boolean, revert: boolean) {
    this.isEdit = isEdit;
    if (this.isEdit) {
      this.dashboardCtx.stateController.preserveState();
      this.prevDashboard = deepClone(this.dashboard);
    } else {
      if (this.isEditingWidget) {
        this.onEditWidgetClosed();
        this.isEditingWidgetClosed = true;
      }
      if (this.isAddingWidget) {
        this.onAddWidgetClosed();
        this.isAddingWidgetClosed = true;
      }
      this.resetHighlight();
      if (revert) {
        this.dashboard = this.prevDashboard;
        this.dashboardLogoCache = undefined;
        this.dashboardConfiguration = this.dashboard.configuration;
        if (!this.widgetEditMode) {
          this.dashboardCtx.dashboardTimewindow = this.dashboardConfiguration.timewindow;
          this.updateDashboardCss();
          this.entityAliasesUpdated();
          this.filtersUpdated();
          this.updateLayouts();
        }
      }
    }
  }

  private resetHighlight() {
    for (const l of Object.keys(this.layouts)) {
      if (this.layouts[l].layoutCtx) {
        if (this.layouts[l].layoutCtx.ctrl) {
          this.layouts[l].layoutCtx.ctrl.resetHighlight();
        }
      }
    }
  }

  private entityAliasesUpdated() {
    this.dashboardCtx.aliasController.updateEntityAliases(this.dashboard.configuration.entityAliases);
  }

  private filtersUpdated() {
    this.dashboardCtx.aliasController.updateFilters(this.dashboard.configuration.filters);
  }

  private notifyDashboardToggleEditMode() {
    if (this.widgetEditMode) {
      const message: WindowMessage = {
        type: 'widgetEditModeToggle',
        data: this.isEdit
      };
      this.window.parent.postMessage(JSON.stringify(message), '*');
    }
  }

  private notifyDashboardUpdated() {
    if (this.widgetEditMode) {
      const widget = this.layouts.main.layoutCtx.widgets.widgetByIndex(0);
      const layout = this.layouts.main.layoutCtx.widgetLayouts[widget.id];
      widget.sizeX = layout.sizeX;
      widget.sizeY = layout.sizeY;
      const message: WindowMessage = {
        type: 'widgetEditUpdated',
        data: widget
      };
      this.window.parent.postMessage(JSON.stringify(message), '*');
      this.setEditMode(false, false);
    } else {
      let reInitDashboard = false;
      this.dashboard.configuration.timewindow = this.dashboardCtx.dashboardTimewindow;
      this.dashboardService.saveDashboard(this.dashboard).pipe(
        catchError((err) => {
          if (err.status === HttpStatusCode.Conflict) {
            reInitDashboard = true;
            return this.dashboardService.getDashboard(this.dashboard.id.id).pipe(
              map(dashboard => this.dashboardUtils.validateAndUpdateDashboard(dashboard))
            );
          }
          return throwError(() => err);
        })
      ).subscribe((dashboard) => {
        if (reInitDashboard) {
          const dashboardPageInitData: DashboardPageInitData = {
            dashboard,
            currentDashboardId: dashboard.id ? dashboard.id.id : null,
            widgetEditMode: this.widgetEditMode,
            singlePageMode: this.singlePageMode
          };
          const needReInitState = !this.isEdit;
          this.init(dashboardPageInitData);
          if (needReInitState) {
            this.dashboardCtx.stateController.reInit();
          }
        } else {
          this.dashboard.version = dashboard.version;
          this.setEditMode(false, false);
        }
      });
    }
  }

  helpLinkIdForWidgetType(): string {
    let link = 'widgetsConfig';
    if (this.editingWidget && this.editingWidget.type) {
      link = widgetTypesData.get(this.editingWidget.type).configHelpLinkId;
    }
    return link;
  }

  addWidget($event: Event, layoutCtx?: DashboardPageLayoutContext) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isEditingWidget) {
      this.onEditWidgetClosed();
      this.isEditingWidgetClosed = true;
      this.isAddingWidgetClosed = false;
    }
    this.isAddingWidget = true;
    this.addingLayoutCtx = layoutCtx;
  }

  onAddWidgetClosed() {
    this.isAddingWidget = false;
  }

  detailsDrawerOpenedStart() {
    if (this.isEditingWidget) {
      this.isEditingWidgetClosed = false;
    } else if (this.isAddingWidget) {
      this.isAddingWidgetClosed = false;
    }
  }

  detailsDrawerClosed() {
    this.isEditingWidgetClosed = true;
    this.isAddingWidgetClosed = true;
  }

  private addWidgetToLayout(widget: Widget, layoutId: DashboardLayoutId) {
    const layoutCtx = this.layouts[layoutId].layoutCtx;
    this.dashboardUtils.addWidgetToLayout(this.dashboard, this.dashboardCtx.state, layoutId, widget, undefined,
      undefined, -1, -1, layoutCtx.breakpoint);
    layoutCtx.widgets.addWidgetId(widget.id);
    this.runChangeDetection();
  }

  private isAddingToScadaLayout(): boolean {
    const layouts = this.dashboardConfiguration.states[this.dashboardCtx.state].layouts;
    let layoutIds: DashboardLayoutId[];
    if (this.addingLayoutCtx?.id) {
      layoutIds = [this.addingLayoutCtx?.id];
    } else {
      layoutIds = Object.keys(layouts) as DashboardLayoutId[];
    }
    return layoutIds.every(id => layouts[id].gridSettings.layoutType === LayoutType.scada);
  }

  private selectTargetLayout(): Observable<DashboardLayoutId> {
    const layouts = this.dashboardConfiguration.states[this.dashboardCtx.state].layouts;
    const layoutIds = Object.keys(layouts);
    if (layoutIds.length > 1) {
      return this.dialog.open<SelectTargetLayoutDialogComponent, any,
        DashboardLayoutId>(SelectTargetLayoutDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed();
    } else {
      return of(layoutIds[0] as DashboardLayoutId);
    }
  }

  private addWidgetToDashboard(widget: Widget) {
    this.dashboardUtils.prepareWidgetForSaving(widget);
    if (this.addingLayoutCtx) {
      this.addWidgetToLayout(widget, this.addingLayoutCtx.id);
      this.addingLayoutCtx = null;
    } else {
      this.selectTargetLayout().subscribe((layoutId) => {
        if (layoutId) {
          this.addWidgetToLayout(widget, layoutId);
        }
      });
    }
  }

  addWidgetFromType(widget: WidgetInfo) {
    this.onAddWidgetClosed();
    this.widgetComponentService.getWidgetInfo(widget.typeFullFqn).subscribe({
      next: (widgetTypeInfo) => {
        const config: WidgetConfig = this.dashboardUtils.widgetConfigFromWidgetType(widgetTypeInfo);
        if (!config.title) {
          config.title = 'New ' + widgetTypeInfo.widgetName;
        }
        let newWidget: Widget = {
          typeFullFqn: widgetTypeInfo.fullFqn,
          type: widgetTypeInfo.type,
          sizeX: widgetTypeInfo.sizeX,
          sizeY: widgetTypeInfo.sizeY,
          config,
          row: 0,
          col: 0
        };
        newWidget = this.dashboardUtils.validateAndUpdateWidget(newWidget);
        let isDefaultBreakpoint = true;
        if (this.addingLayoutCtx?.breakpoint) {
          isDefaultBreakpoint = this.addingLayoutCtx.breakpoint === 'default';
        } else if (!this.layouts.right.show) {
          isDefaultBreakpoint = this.layouts.main.layoutCtx.breakpoint === 'default';
        }
        const scada = this.isAddingToScadaLayout();
        if (scada) {
          newWidget = this.dashboardUtils.prepareWidgetForScadaLayout(newWidget, widgetTypeInfo.scada);
        }
        let showLayoutConfig = true;
        if (scada || this.layouts.right.show || !this.showLayoutConfigInEdit(this.layouts.main.layoutCtx)) {
          showLayoutConfig = false;
        }
        if (widgetTypeInfo.typeParameters.useCustomDatasources) {
          this.addWidgetToDashboard(newWidget);
        } else {
          this.dialog.open<AddWidgetDialogComponent, AddWidgetDialogData,
            Widget>(AddWidgetDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            maxWidth: '95vw',
            injector: this.injector,
            data: {
              dashboard: this.dashboard,
              aliasController: this.dashboardCtx.aliasController,
              stateController: this.dashboardCtx.stateController,
              widget: newWidget,
              widgetInfo: widgetTypeInfo,
              showLayoutConfig,
              isDefaultBreakpoint
            }
          }).afterClosed().subscribe((addedWidget) => {
            if (addedWidget) {
              this.addWidgetToDashboard(addedWidget);
            }
          });
        }
      },
      error: (errorData) => {
        const errorMessages: string[] = errorData.errorMessages;
        this.dialogService.alert(this.translate.instant('widget.widget-type-load-error'),
          errorMessages.join('<br>').replace(/\n/g, '<br>'));
      }
    });
  }

  onRevertWidgetEdit() {
    if (this.editWidgetComponent.widgetFormGroup.dirty) {
      this.editWidgetComponent.widgetFormGroup.markAsPristine();
      this.editingWidget = deepClone(this.editingWidgetOriginal);
      this.editingWidgetLayout = deepClone(this.editingWidgetLayoutOriginal);
    }
  }

  saveWidget() {
    this.editWidgetComponent.widgetFormGroup.markAsPristine();
    const widget = this.dashboardUtils.prepareWidgetForSaving(deepClone(this.editingWidget));
    const widgetLayout = deepClone(this.editingWidgetLayout);
    const id = this.editingWidgetOriginal.id;
    this.dashboardConfiguration.widgets[id] = widget;
    this.editingWidgetOriginal = widget;
    this.editingWidgetLayoutOriginal = widgetLayout;
    this.editingLayoutCtx.widgetLayouts[widget.id] = widgetLayout;
    setTimeout(() => {
      this.editingLayoutCtx.ctrl.highlightWidget(widget.id, 0);
    }, 0);
  }

  onEditWidgetClosed() {
    this.editingWidgetOriginal = null;
    this.editingWidget = null;
    this.editingWidgetLayoutOriginal = null;
    this.editingWidgetLayout = null;
    this.editingLayoutCtx = null;
    this.editingWidgetSubtitle = null;
    this.isEditingWidget = false;
    this.updateLayoutSizes();
    this.resetHighlight();
    this.forceDashboardMobileMode = false;
  }

  editWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    $event.stopPropagation();

    if (this.isAddingWidget) {
      this.onAddWidgetClosed();
      this.isAddingWidgetClosed = true;
      this.isEditingWidgetClosed = false;
    }

    if (this.editingWidgetOriginal === widget) {
      this.onEditWidgetClosed();
    } else {
      const transition = !this.forceDashboardMobileMode;
      this.editingWidgetOriginal = widget;
      this.editingWidgetLayoutOriginal = layoutCtx.widgetLayouts[widget.id];
      this.editingWidget = deepClone(this.editingWidgetOriginal);
      this.editingWidgetLayout = deepClone(this.editingWidgetLayoutOriginal);
      this.editingLayoutCtx = layoutCtx;
      this.editingWidgetSubtitle = this.widgetComponentService.getInstantWidgetInfo(this.editingWidget).widgetName;
      this.forceDashboardMobileMode = true;
      this.isEditingWidget = true;
      this.updateLayoutSizes();
      if (layoutCtx) {
        const delayOffset = transition ? 350 : 0;
        const delay = transition ? 400 : 300;
        setTimeout(() => {
          layoutCtx.ctrl.highlightWidget(widget.id, delay);
        }, delayOffset);
      }
    }
  }

  showLayoutConfigInEdit(layoutCtx: DashboardPageLayoutContext): boolean {
    return layoutCtx?.gridSettings?.layoutType === LayoutType.divider ||
      layoutCtx?.gridSettings?.layoutType === LayoutType.default &&
      (layoutCtx?.breakpoint === 'default' ||
        layoutCtx?.breakpoint !== 'default' && layoutCtx?.gridSettings?.viewFormat === ViewFormatType.list);
  }

  replaceReferenceWithWidgetCopy($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    $event.stopPropagation();

    const isRemove = layoutCtx.widgets.removeWidgetId(widget.id);

    const widgetCopy = this.dashboardUtils.replaceReferenceWithWidgetCopy(widget, this.dashboard, this.dashboardCtx.state,
      layoutCtx.id, layoutCtx.breakpoint, isRemove);

    layoutCtx.widgets.addWidgetId(widgetCopy.id);

    this.runChangeDetection();
  }

  copyWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    this.itembuffer.copyWidget(this.dashboard,
      this.dashboardCtx.state, layoutCtx.id, widget, layoutCtx.breakpoint);
  }

  copyWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    this.itembuffer.copyWidgetReference(this.dashboard,
      this.dashboardCtx.state, layoutCtx.id, widget, layoutCtx.breakpoint);
  }

  pasteWidget($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition) {
    this.itembuffer.pasteWidget(this.dashboard, this.dashboardCtx.state, layoutCtx.id, layoutCtx.breakpoint,
            pos, this.entityAliasesUpdated.bind(this), this.filtersUpdated.bind(this)).subscribe(
      (widget) => {
        layoutCtx.widgets.addWidgetId(widget.id);
        this.runChangeDetection();
      });
  }

  pasteWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition) {
    this.itembuffer.pasteWidgetReference(this.dashboard, this.dashboardCtx.state, layoutCtx.id, layoutCtx.breakpoint,
      pos).subscribe(
      (widget) => {
        layoutCtx.widgets.addWidgetId(widget.id);
        this.runChangeDetection();
      });
  }

  removeWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    let title = widget.config.title;
    if (!title || title.length === 0) {
      title = this.widgetComponentService.getInstantWidgetInfo(widget).widgetName;
    }
    const confirmTitle = this.translate.instant('widget.remove-widget-title', {widgetTitle: title});
    const confirmContent = this.translate.instant('widget.remove-widget-text');
    this.dialogService.confirm(confirmTitle,
      confirmContent,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
    ).subscribe((res) => {
      if (res) {
        if (layoutCtx.widgets.removeWidgetId(widget.id)) {
          this.dashboardUtils.removeWidgetFromLayout(this.dashboard, this.dashboardCtx.state, layoutCtx.id,
            widget.id, layoutCtx.breakpoint);
          this.runChangeDetection();
        }
      }
    });
  }

  exportWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget, widgetTitle: string) {
    $event.stopPropagation();
    this.importExport.exportWidget(this.dashboard, this.dashboardCtx.state, layoutCtx.id, widget, widgetTitle, layoutCtx.breakpoint);
  }

  dashboardMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext) {
    if (this.isEdit && !this.isEditingWidget) {
      layoutCtx.ctrl.resetHighlight();
    }
  }

  widgetClicked($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    if (this.isEditingWidget) {
      this.editWidget($event, layoutCtx, widget);
    }
  }

  widgetMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    if (this.isEdit && !this.isEditingWidget) {
      layoutCtx.ctrl.selectWidget(widget.id, 0);
    }
  }

  prepareDashboardContextMenu(layoutCtx: DashboardPageLayoutContext): Array<DashboardContextMenuItem> {
    const dashboardContextActions: Array<DashboardContextMenuItem> = [];
    if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
      dashboardContextActions.push(
        {
          action: this.openDashboardSettings.bind(this),
          enabled: true,
          value: 'dashboard.settings',
          icon: 'settings'
        }
      );
      dashboardContextActions.push(
        {
          action: this.openEntityAliases.bind(this),
          enabled: true,
          value: 'entity.aliases',
          icon: 'devices_other'
        }
      );
      dashboardContextActions.push(
        {
          action: ($event) => {
            layoutCtx.ctrl.pasteWidget($event);
          },
          enabled: this.itembuffer.hasWidget(),
          value: 'action.paste',
          icon: 'content_paste',
          shortcut: 'M-V'
        }
      );
      dashboardContextActions.push(
        {
          action: ($event) => {
            layoutCtx.ctrl.pasteWidgetReference($event);
          },
          enabled: this.itembuffer.canPasteWidgetReference(this.dashboard, this.dashboardCtx.state, layoutCtx.id, layoutCtx.breakpoint),
          value: 'action.paste-reference',
          icon: 'content_paste',
          shortcut: 'M-I'
        }
      );
      dashboardContextActions.push(
        {
          action: ($event) => {
            this.moveWidgets($event, layoutCtx.id, layoutCtx.breakpoint);
          },
          enabled: true,
          value: 'dashboard.move-all-widgets',
          icon: 'open_with'
        }
      );
    }
    return dashboardContextActions;
  }

  prepareWidgetContextMenu(layoutCtx: DashboardPageLayoutContext, widget: Widget, isReference: boolean): Array<WidgetContextMenuItem> {
    const widgetContextActions: Array<WidgetContextMenuItem> = [];
    if (this.isEdit && !this.isEditingWidget) {
      widgetContextActions.push(
        {
          action: (event, currentWidget) => {
            this.editWidget(event, layoutCtx, currentWidget);
          },
          enabled: true,
          value: 'action.edit',
          icon: 'edit'
        }
      );
      if (isReference) {
        widgetContextActions.push(
          {
            action: (event, currentWidget) => {
              this.replaceReferenceWithWidgetCopy(event, layoutCtx, currentWidget);
            },
            enabled: true,
            value: 'widget.replace-reference-with-widget-copy',
            icon: 'mdi:file-replace-outline'
          }
        );
      }
      if (!this.widgetEditMode) {
        widgetContextActions.push(
          {
            action: (event, currentWidget) => {
              this.copyWidget(event, layoutCtx, currentWidget);
            },
            enabled: true,
            value: 'action.copy',
            icon: 'content_copy',
            shortcut: 'M-C'
          }
        );
        widgetContextActions.push(
          {
            action: (event, currentWidget) => {
              this.copyWidgetReference(event, layoutCtx, currentWidget);
            },
            enabled: true,
            value: 'action.copy-reference',
            icon: 'content_copy',
            shortcut: 'M-R'
          }
        );
        widgetContextActions.push(
          {
            action: (event, currentWidget) => {
              this.removeWidget(event, layoutCtx, currentWidget);
            },
            enabled: true,
            value: 'action.delete',
            icon: 'clear',
            shortcut: 'M-X'
          }
        );
      }
    }
    return widgetContextActions;
  }

  clearSelectedWidgetBundle() {
    this.dashboardWidgetSelectComponent.search = '';
    this.dashboardWidgetSelectComponent.widgetsBundle = null;
    this.dashboardWidgetSelectComponent.selectWidgetMode = 'bundles';
  }

  editWidgetsTypesToDisplay($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'bottom',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const filterWidgetTypes = this.dashboardWidgetSelectComponent.filterWidgetTypes;
    const widgetTypesList = Array.from(this.dashboardWidgetSelectComponent.widgetTypes.values()).map(type =>
      ({type, display: filterWidgetTypes === null ? true : filterWidgetTypes.includes(type)}));

    const providers: StaticProvider[] = [
      {
        provide: DISPLAY_WIDGET_TYPES_PANEL_DATA,
        useValue: {
          types: widgetTypesList,
          typesUpdated: (newTypes) => {
            this.filterWidgetTypes = newTypes.filter(type => type.display).map(type => type.type);
            this.cd.markForCheck();
          }
        } as DisplayWidgetTypesPanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    overlayRef.attach(new ComponentPortal(DisplayWidgetTypesPanelComponent, this.viewContainerRef, injector));
    this.cd.markForCheck();
  }

  public updateDashboardImage($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DashboardImageDialogComponent, DashboardImageDialogData,
      DashboardImageDialogResult>(DashboardImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        dashboardId: this.dashboard.id,
        currentImage: this.dashboard.image,
        dashboardElement: this.dashboardContainer.nativeElement
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.dashboard.image = result.image;
      }
    });
  }

  toggleVersionControl($event: Event, versionControlButton: MatButton | MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = versionControlButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const versionControlPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, VersionControlComponent, 'leftTop', true, null,
        {
          detailsMode: true,
          active: true,
          singleEntityMode: true,
          externalEntityId: this.dashboard.externalId || this.dashboard.id,
          entityId: this.dashboard.id,
          entityName: this.dashboard.name,
          onBeforeCreateVersion: () => this.dashboardService.saveDashboard(this.dashboard).pipe(
              tap((dashboard) => {
                this.dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
                this.prevDashboard = deepClone(this.dashboard);
              })
            )
        }, {}, {}, {}, true);
      versionControlPopover.tbComponentRef.instance.popoverComponent = versionControlPopover;
      versionControlPopover.tbComponentRef.instance.versionRestored.subscribe(() => {
        this.dashboardService.getDashboard(this.currentDashboardId).subscribe((dashboard) => {
          dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
          const data: DashboardPageInitData = {
            dashboard,
            currentDashboardId: this.currentDashboardId,
            widgetEditMode: this.widgetEditMode,
            singlePageMode: this.singlePageMode
          };
          this.init(data);
          this.dashboardCtx.stateController.cleanupPreservedStates();
          this.dashboardCtx.stateController.resetState();
          this.setEditMode(true, false);
          this.updateBreadcrumbs.emit();
          this.ngZone.run(() => {
            this.cd.detectChanges();
          });
        });
      });
    }
  }

  get showMainLayoutFiller(): boolean {
    const layoutMaxWidth = this.dashboardUtils.getBreakpointInfoById(this.layouts.main.layoutCtx.breakpoint)?.maxWidth || Infinity;
    const dashboardMaxWidth = this.dashboardUtils.getBreakpointInfoById(this.dashboardCtx.breakpoint)?.maxWidth || Infinity;
    return !this.layouts.right.show && layoutMaxWidth < dashboardMaxWidth  && !this.isEditingWidget;
  }

  get currentBreakpointValue(): string {
    return this.dashboardUtils.getBreakpointSizeDescription(this.layouts.main.layoutCtx.breakpoint);
  }

  private parseBreakpointsResponse(breakpoints: {[key: string]: boolean}): BreakpointInfo {
    const activeBreakpoints: BreakpointInfo[] = [];
    Object.keys(breakpoints).map((key) => {
      if (breakpoints[key]) {
        activeBreakpoints.push(this.dashboardUtils.getBreakpointInfoByValue(key));
      }
    });
    return activeBreakpoints.pop();
  }

  private isMobileSize(breakpoint: BreakpointInfo): boolean {
    if (breakpoint?.maxWidth) {
      return breakpoint.maxWidth < 960;
    }
    return false;
  }

  private getDashboardLogoLink(): UrlTree {
    return this.forceFullscreen ? null : this.router.createUrlTree([], {relativeTo: this.route});
  }
}

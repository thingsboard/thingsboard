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
  ChangeDetectorRef,
  Component,
  Inject,
  Injector,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { UtilsService } from '@core/services/utils.service';
import { AuthService } from '@core/auth/auth.service';
import {
  Dashboard,
  DashboardConfiguration,
  DashboardLayoutId,
  DashboardLayoutInfo,
  DashboardLayoutsInfo,
  DashboardState,
  DashboardStateLayouts,
  GridSettings,
  WidgetLayout
} from '@app/shared/models/dashboard.models';
import { WINDOW } from '@core/services/window.service';
import { WindowMessage } from '@shared/models/window-message.model';
import { deepClone, isDefined, isDefinedAndNotNull } from '@app/core/utils';
import {
  DashboardContext,
  DashboardPageLayout,
  DashboardPageLayoutContext,
  DashboardPageLayouts,
  DashboardPageScope,
  IDashboardController,
  LayoutWidgetsArray
} from './dashboard-page.models';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import {
  DatasourceType,
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
import { Observable, of, Subscription } from 'rxjs';
import { FooterFabButtons } from '@shared/components/footer-fab-buttons.component';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardContextMenuItem, WidgetContextMenuItem } from '../../models/dashboard-component.models';
import { WidgetComponentService } from '../../components/widget/widget-component.service';
import { FormBuilder } from '@angular/forms';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { EntityAliases } from '@app/shared/models/alias.models';
import { EditWidgetComponent } from '@home/components/dashboard-page/edit-widget.component';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
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
  ManageDashboardStatesDialogData
} from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { AuthState } from '@app/core/auth/auth.models';
import { FiltersDialogComponent, FiltersDialogData } from '@home/components/filter/filters-dialog.component';
import { Filters } from '@shared/models/query/query.models';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  DISPLAY_WIDGET_TYPES_PANEL_DATA,
  DisplayWidgetTypesPanelComponent,
  DisplayWidgetTypesPanelData,
  WidgetTypes
} from '@home/components/dashboard-page/widget-types-panel.component';
import { DashboardWidgetSelectComponent } from '@home/components/dashboard-page/dashboard-widget-select.component';

// @dynamic
@Component({
  selector: 'tb-dashboard-page',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.scss'],
  encapsulation: ViewEncapsulation.None,
  // changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent extends PageComponent implements IDashboardController, OnInit, OnDestroy {

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  @Input()
  embedded = false;

  @Input()
  currentState: string;

  @Input()
  hideToolbar: boolean;

  @Input()
  syncStateWithQueryParam = true;

  @Input()
  dashboard: Dashboard;
  dashboardConfiguration: DashboardConfiguration;

  prevDashboard: Dashboard;

  iframeMode = this.utils.iframeMode;
  widgetEditMode: boolean;
  singlePageMode: boolean;
  forceFullscreen = this.authState.forceFullscreen;

  isFullscreen = false;
  isEdit = false;
  isEditingWidget = false;
  isEditingWidgetClosed = true;
  isMobile = !this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
  forceDashboardMobileMode = false;
  isAddingWidget = false;
  isAddingWidgetClosed = true;
  searchBundle = '';
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

  currentDashboardId: string;
  currentCustomerId: string;
  currentDashboardScope: DashboardPageScope;

  addingLayoutCtx: DashboardPageLayoutContext;


  dashboardCtx: DashboardContext = {
    instanceId: this.utils.guid(),
    getDashboard: () => this.dashboard,
    dashboardTimewindow: null,
    state: null,
    stateController: null,
    aliasController: null,
    runChangeDetection: this.runChangeDetection.bind(this)
  };

  layouts: DashboardPageLayouts = {
    main: {
      show: false,
      layoutCtx: {
        id: 'main',
        widgets: null,
        widgetLayouts: {},
        gridSettings: {},
        ignoreLoading: true,
        ctrl: null,
        dashboardCtrl: this
      }
    },
    right: {
      show: false,
      layoutCtx: {
        id: 'right',
        widgets: null,
        widgetLayouts: {},
        gridSettings: {},
        ignoreLoading: true,
        ctrl: null,
        dashboardCtrl: this
      }
    }
  };

  addWidgetFabButtons: FooterFabButtons = {
    fabTogglerName: 'dashboard.add-widget',
    fabTogglerIcon: 'add',
    buttons: [
      {
        name: 'dashboard.create-new-widget',
        icon: 'insert_drive_file',
        onAction: ($event) => {
          this.addWidget($event);
        }
      },
      {
        name: 'dashboard.import-widget',
        icon: 'file_upload',
        onAction: ($event) => {
          this.importWidget($event);
        }
      }
    ]
  };

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

  @ViewChild('tbEditWidget') editWidgetComponent: EditWidgetComponent;

  @ViewChild('dashboardWidgetSelect') dashboardWidgetSelectComponent: DashboardWidgetSelectComponent;

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private breakpointObserver: BreakpointObserver,
              private route: ActivatedRoute,
              private router: Router,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private authService: AuthService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private widgetComponentService: WidgetComponentService,
              private dashboardService: DashboardService,
              private itembuffer: ItemBufferService,
              private importExport: ImportExportService,
              private fb: FormBuilder,
              private dialog: MatDialog,
              private translate: TranslateService,
              private ngZone: NgZone,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {
    super(store);

  }

  ngOnInit() {
    this.rxSubscriptions.push(this.route.data.subscribe(
      (data) => {
        if (this.embedded) {
          data.dashboard = this.dashboardUtils.validateAndUpdateDashboard(this.dashboard);
          data.currentDashboardId = this.dashboard.id ? this.dashboard.id.id : null;
          data.widgetEditMode = false;
          data.singlePageMode = false;
        } else {
          data.currentDashboardId = this.route.snapshot.params.dashboardId;
        }
        this.init(data);
        this.runChangeDetection();
      }
    ));
    this.rxSubscriptions.push(this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .subscribe((state: BreakpointState) => {
          this.isMobile = !state.matches;
        }
    ));
  }

  private init(data: any) {

    this.reset();

    this.currentDashboardId = data.currentDashboardId;

    if (this.route.snapshot.params.customerId) {
      this.currentCustomerId = this.route.snapshot.params.customerId;
      this.currentDashboardScope = 'customer';
    } else {
      this.currentDashboardScope = this.authUser.authority === Authority.TENANT_ADMIN ? 'tenant' : 'customer';
      this.currentCustomerId = this.authUser.customerId;
    }

    this.dashboard = data.dashboard;
    this.dashboardConfiguration = this.dashboard.configuration;
    this.dashboardCtx.dashboardTimewindow = this.dashboardConfiguration.timewindow;
    this.layouts.main.layoutCtx.widgets = new LayoutWidgetsArray(this.dashboardCtx);
    this.layouts.right.layoutCtx.widgets = new LayoutWidgetsArray(this.dashboardCtx);
    this.widgetEditMode = data.widgetEditMode;
    this.singlePageMode = data.singlePageMode;

    this.dashboardCtx.aliasController = new AliasController(this.utils,
      this.entityService,
      this.translate,
      () => this.dashboardCtx.stateController,
      this.dashboardConfiguration.entityAliases,
      this.dashboardConfiguration.filters);

    if (this.widgetEditMode) {
      const message: WindowMessage = {
        type: 'widgetEditModeInited'
      };
      this.window.parent.postMessage(JSON.stringify(message), '*');
    }
  }

  private reset() {
    this.dashboard = null;
    this.dashboardConfiguration = null;
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

    this.dashboardCtx.state = null;
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  public runChangeDetection() {
    /*setTimeout(() => {
      this.cd.detectChanges();
    });*/
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
    return this.widgetEditMode || this.iframeMode || this.forceFullscreen || this.singlePageMode;
  }

  public toolbarAlwaysOpen(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.toolbarAlwaysOpen)) {
      return this.dashboard.configuration.settings.toolbarAlwaysOpen;
    } else {
      return true;
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

  public displayExport(): boolean {
    if (this.dashboard.configuration.settings &&
      isDefined(this.dashboard.configuration.settings.showDashboardExport)) {
      return this.dashboard.configuration.settings.showDashboardExport;
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

  public showRightLayoutSwitch(): boolean {
    return this.isMobile && this.layouts.right.show;
  }

  public toggleLayouts() {
    this.isRightLayoutOpened = !this.isRightLayoutOpened;
  }

  public openRightLayout() {
    this.isRightLayoutOpened = true;
  }

  public mainLayoutWidth(): string {
    if (this.isEditingWidget && this.editingLayoutCtx.id === 'main') {
      return '100%';
    } else {
      return this.layouts.right.show && !this.isMobile ? '50%' : '100%';
    }
  }

  public mainLayoutHeight(): string {
    if (!this.isEditingWidget || this.editingLayoutCtx.id === 'main') {
      return '100%';
    } else {
      return '0px';
    }
  }

  public rightLayoutWidth(): string {
    if (this.isEditingWidget && this.editingLayoutCtx.id === 'right') {
      return '100%';
    } else {
      return this.isMobile ? '100%' : '50%';
    }
  }

  public rightLayoutHeight(): string {
    if (!this.isEditingWidget || this.editingLayoutCtx.id === 'right') {
      return '100%';
    } else {
      return '0px';
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
      $event.stopPropagation();
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
      gridSettings = deepClone(this.dashboard.configuration.states[layoutKeys.state].layouts[layoutKeys.layout].gridSettings);
    }
    this.dialog.open<DashboardSettingsDialogComponent, DashboardSettingsDialogData,
      DashboardSettingsDialogData>(DashboardSettingsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        settings: deepClone(this.dashboard.configuration.settings),
        gridSettings
      }
    }).afterClosed().subscribe((data) => {
      if (data) {
        this.dashboard.configuration.settings = data.settings;
        const newGridSettings = data.gridSettings;
        if (newGridSettings) {
          const layout = this.dashboard.configuration.states[layoutKeys.state].layouts[layoutKeys.layout];
          this.dashboardUtils.updateLayoutSettings(layout, newGridSettings);
          this.updateLayouts();
       }
      }
    });
  }

  public manageDashboardStates($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ManageDashboardStatesDialogComponent, ManageDashboardStatesDialogData,
      {[id: string]: DashboardState }>(ManageDashboardStatesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        states: deepClone(this.dashboard.configuration.states)
      }
    }).afterClosed().subscribe((states) => {
      if (states) {
        this.updateStates(states);
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

  private importWidget($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.importWidget(this.dashboard, this.dashboardCtx.state,
      this.selectTargetLayout.bind(this), this.entityAliasesUpdated.bind(this), this.filtersUpdated.bind(this)).subscribe(
      (importData) => {
        if (importData) {
          const widget = importData.widget;
          const layoutId = importData.layoutId;
          this.layouts[layoutId].layoutCtx.widgets.addWidgetId(widget.id);
        }
      }
    );
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
  }

  public saveDashboard() {
    this.setEditMode(false, false);
    this.notifyDashboardUpdated();
  }

  public openDashboardState(state: string, openRightLayout?: boolean) {
    const layoutsData = this.dashboardUtils.getStateLayoutsData(this.dashboard, state);
    if (layoutsData) {
      this.dashboardCtx.state = state;
      this.dashboardCtx.aliasController.dashboardStateChanged();
      this.isRightLayoutOpened = openRightLayout ? true : false;
      this.updateLayouts(layoutsData);
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
        this.updateLayout(layout, {widgetIds: [], widgetLayouts: {}, gridSettings: null});
      }
    }
  }

  private updateLayout(layout: DashboardPageLayout, layoutInfo: DashboardLayoutInfo) {
    if (layoutInfo.gridSettings) {
      layout.layoutCtx.gridSettings = layoutInfo.gridSettings;
    }
    layout.layoutCtx.widgets.setWidgetIds(layoutInfo.widgetIds);
    layout.layoutCtx.widgetLayouts = layoutInfo.widgetLayouts;
    if (layout.show && layout.layoutCtx.ctrl) {
      layout.layoutCtx.ctrl.reload();
    }
    layout.layoutCtx.ignoreLoading = true;
  }

  private setEditMode(isEdit: boolean, revert: boolean) {
    this.isEdit = isEdit;
    if (this.isEdit) {
      this.dashboardCtx.stateController.preserveState();
      this.prevDashboard = deepClone(this.dashboard);
    } else {
      if (this.widgetEditMode) {
        if (revert) {
          this.dashboard = this.prevDashboard;
        }
      } else {
        this.resetHighlight();
        if (revert) {
          this.dashboard = this.prevDashboard;
          this.dashboardConfiguration = this.dashboard.configuration;
          this.dashboardCtx.dashboardTimewindow = this.dashboardConfiguration.timewindow;
          this.entityAliasesUpdated();
          this.filtersUpdated();
          this.updateLayouts();
        } else {
          this.dashboard.configuration.timewindow = this.dashboardCtx.dashboardTimewindow;
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
    } else {
      this.dashboardService.saveDashboard(this.dashboard).subscribe();
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
    setTimeout(() => {
      this.cd.detach();
    }, 0);
  }

  detailsDrawerOpened() {
    this.cd.reattach();
  }

  detailsDrawerClosedStart() {
    this.cd.detach();
  }

  detailsDrawerClosed() {
    this.isEditingWidgetClosed = true;
    this.isAddingWidgetClosed = true;
    this.cd.reattach();
  }

  private addWidgetToLayout(widget: Widget, layoutId: DashboardLayoutId) {
    this.dashboardUtils.addWidgetToLayout(this.dashboard, this.dashboardCtx.state, layoutId, widget);
    this.layouts[layoutId].layoutCtx.widgets.addWidgetId(widget.id);
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
    this.searchBundle = '';
    this.widgetComponentService.getWidgetInfo(widget.bundleAlias, widget.typeAlias, widget.isSystemType).subscribe(
      (widgetTypeInfo) => {
        const config: WidgetConfig = JSON.parse(widgetTypeInfo.defaultConfig);
        config.title = 'New ' + widgetTypeInfo.widgetName;
        config.datasources = [];
        if (isDefinedAndNotNull(config.alarmSource)) {
          config.alarmSource = {
            type: DatasourceType.entity,
            dataKeys: config.alarmSource.dataKeys || []
          };
        }
        const newWidget: Widget = {
          isSystemType: widget.isSystemType,
          bundleAlias: widget.bundleAlias,
          typeAlias: widgetTypeInfo.alias,
          type: widgetTypeInfo.type,
          title: 'New widget',
          image: null,
          description: null,
          sizeX: widgetTypeInfo.sizeX,
          sizeY: widgetTypeInfo.sizeY,
          config,
          row: 0,
          col: 0
        };
        if (widgetTypeInfo.typeParameters.useCustomDatasources) {
          this.addWidgetToDashboard(newWidget);
        } else {
          this.dialog.open<AddWidgetDialogComponent, AddWidgetDialogData,
            Widget>(AddWidgetDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            data: {
              dashboard: this.dashboard,
              aliasController: this.dashboardCtx.aliasController,
              widget: newWidget,
              widgetInfo: widgetTypeInfo
            }
          }).afterClosed().subscribe((addedWidget) => {
            if (addedWidget) {
              this.addWidgetToDashboard(addedWidget);
            }
          });
        }
      }
    );
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
    const widget = deepClone(this.editingWidget);
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
    this.resetHighlight();
    this.forceDashboardMobileMode = false;
  }

  editWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    $event.stopPropagation();
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
      if (layoutCtx) {
        const delayOffset = transition ? 350 : 0;
        const delay = transition ? 400 : 300;
        setTimeout(() => {
          layoutCtx.ctrl.highlightWidget(widget.id, delay);
        }, delayOffset);
      }
    }
  }

  copyWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    this.itembuffer.copyWidget(this.dashboard,
      this.dashboardCtx.state, layoutCtx.id, widget);
  }

  copyWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    this.itembuffer.copyWidgetReference(this.dashboard,
      this.dashboardCtx.state, layoutCtx.id, widget);
  }

  pasteWidget($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition) {
    this.itembuffer.pasteWidget(this.dashboard, this.dashboardCtx.state, layoutCtx.id,
            pos, this.entityAliasesUpdated.bind(this), this.filtersUpdated.bind(this)).subscribe(
      (widget) => {
        layoutCtx.widgets.addWidgetId(widget.id);
      });
  }

  pasteWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition) {
    this.itembuffer.pasteWidgetReference(this.dashboard, this.dashboardCtx.state, layoutCtx.id,
      pos).subscribe(
      (widget) => {
        layoutCtx.widgets.addWidgetId(widget.id);
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
          this.dashboardUtils.removeWidgetFromLayout(this.dashboard, this.dashboardCtx.state, layoutCtx.id, widget.id);
        }
      }
    });
  }

  exportWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget) {
    $event.stopPropagation();
    this.importExport.exportWidget(this.dashboard, this.dashboardCtx.state, layoutCtx.id, widget);
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
          enabled: this.itembuffer.canPasteWidgetReference(this.dashboard, this.dashboardCtx.state, layoutCtx.id),
          value: 'action.paste-reference',
          icon: 'content_paste',
          shortcut: 'M-I'
        }
      );
    }
    return dashboardContextActions;
  }

  prepareWidgetContextMenu(layoutCtx: DashboardPageLayoutContext, widget: Widget): Array<WidgetContextMenuItem> {
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

  widgetBundleSelected(){
    this.searchBundle = '';
  }

  clearSelectedWidgetBundle() {
    this.searchBundle = '';
    this.dashboardWidgetSelectComponent.widgetsBundle = null;
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

    const providers: StaticProvider[] = [
      {
        provide: DISPLAY_WIDGET_TYPES_PANEL_DATA,
        useValue: {
          types: Array.from(this.dashboardWidgetSelectComponent.widgetTypes.values()).map(type => {
            return {type, display: true};
          }),
          typesUpdated: (newTypes) => {
            this.filterWidgetTypes = newTypes.filter(type => type.display).map(type => type.type);
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
    this.cd.detectChanges();
  }

  onCloseSearchBundle() {
    this.searchBundle = '';
  }
}

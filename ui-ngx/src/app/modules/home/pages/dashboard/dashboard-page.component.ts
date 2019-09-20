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

import { Component, Inject, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { UtilsService } from '@core/services/utils.service';
import { AuthService } from '@core/auth/auth.service';
import {
  Dashboard,
  DashboardConfiguration,
  WidgetLayout,
  DashboardLayoutInfo,
  DashboardLayoutsInfo
} from '@app/shared/models/dashboard.models';
import { WINDOW } from '@core/services/window.service';
import { WindowMessage } from '@shared/models/window-message.model';
import { deepClone, isDefined } from '@app/core/utils';
import {
  DashboardContext, DashboardPageLayout,
  DashboardPageLayoutContext,
  DashboardPageLayouts,
  DashboardPageScope, IDashboardController
} from './dashboard-page.models';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Widget } from '@app/shared/models/widget.models';
import { environment as env } from '@env/environment';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { EntityService } from '@core/http/entity.service';
import { AliasController } from '@core/api/alias-controller';
import { Subscription } from 'rxjs';
import { FooterFabButtons } from '@shared/components/footer-fab-buttons.component';
import { IStateController } from '@core/api/widget-api.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

@Component({
  selector: 'tb-dashboard-page',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DashboardPageComponent extends PageComponent implements IDashboardController, OnDestroy {

  authUser: AuthUser = getCurrentAuthUser(this.store);

  dashboard: Dashboard;
  dashboardConfiguration: DashboardConfiguration;

  prevDashboard: Dashboard;

  iframeMode = this.utils.iframeMode;
  widgetEditMode: boolean;
  singlePageMode: boolean;
  forceFullscreen = this.authService.forceFullscreen;

  isFullscreen = false;
  isEdit = false;
  isEditingWidget = false;
  isMobile = !this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
  forceDashboardMobileMode = false;
  isAddingWidget = false;

  isToolbarOpened = false;
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

  layouts: DashboardPageLayouts = {
    main: {
      show: false,
      layoutCtx: {
        id: 'main',
        widgets: [],
        widgetLayouts: {},
        gridSettings: {},
        ignoreLoading: false,
        ctrl: null
      }
    },
    right: {
      show: false,
      layoutCtx: {
        id: 'right',
        widgets: [],
        widgetLayouts: {},
        gridSettings: {},
        ignoreLoading: false,
        ctrl: null
      }
    }
  };

  dashboardCtx: DashboardContext = {
    dashboard: null,
    dashboardTimewindow: null,
    state: null,
    stateController: null,
    aliasController: null
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
    return !this.widgetEditMode &&
      (this.toolbarAlwaysOpen() || this.isToolbarOpened || this.isEdit || this.showRightLayoutSwitch());
  }
  set toolbarOpened(toolbarOpened: boolean) {
  }

  get rightLayoutOpened(): boolean {
    return !this.isMobile || this.isRightLayoutOpened;
  }
  set rightLayoutOpened(rightLayoutOpened: boolean) {
  }

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private breakpointObserver: BreakpointObserver,
              private route: ActivatedRoute,
              private router: Router,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private authService: AuthService,
              private entityService: EntityService,
              private dialogService: DialogService) {
    super(store);

    this.rxSubscriptions.push(this.route.data.subscribe(
      (data) => {
        this.init(data);
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

    this.currentDashboardId = this.route.snapshot.params.dashboardId;

    if (this.route.snapshot.params.customerId) {
      this.currentCustomerId = this.route.snapshot.params.customerId;
      this.currentDashboardScope = 'customer';
    } else {
      this.currentDashboardScope = this.authUser.authority === Authority.TENANT_ADMIN ? 'tenant' : 'customer';
      this.currentCustomerId = this.authUser.customerId;
    }

    this.dashboard = data.dashboard;
    this.dashboardConfiguration = this.dashboard.configuration;
    this.widgetEditMode = data.widgetEditMode;
    this.singlePageMode = data.singlePageMode;

    this.dashboardCtx.dashboard = this.dashboard;
    this.dashboardCtx.dashboardTimewindow = this.dashboardConfiguration.timewindow;
    this.dashboardCtx.aliasController = new AliasController(this.utils,
      this.entityService,
      this.dashboardCtx.stateController,
      this.dashboardConfiguration.entityAliases);

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
    this.forceDashboardMobileMode = false;
    this.isAddingWidget = false;

    this.isToolbarOpened = false;
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
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  public openToolbar() {
    this.isToolbarOpened = true;
  }

  public closeToolbar() {
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
    // TODO:
    this.dialogService.todo();
  }

  public openEntityAliases($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }

  public openDashboardSettings($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }

  public manageDashboardStates($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }

  public manageDashboardLayouts($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }

  private addWidget($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }

  private importWidget($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }

  public currentDashboardIdChanged(dashboardId: string) {
    if (!this.widgetEditMode) {
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

  public openDashboardState(state: string, openRightLayout: boolean) {
    const layoutsData = this.dashboardUtils.getStateLayoutsData(this.dashboard, state);
    if (layoutsData) {
      this.dashboardCtx.state = state;
      this.dashboardCtx.aliasController.dashboardStateChanged();
      let layoutVisibilityChanged = false;
      for (const l of Object.keys(this.layouts)) {
        const layout: DashboardPageLayout = this.layouts[l];
        let showLayout;
        if (layoutsData[l]) {
          showLayout = true;
        } else {
          showLayout = false;
        }
        if (layout.show !== showLayout) {
          layout.show = showLayout;
          layoutVisibilityChanged = !this.isMobile;
        }
      }
      this.isRightLayoutOpened = openRightLayout ? true : false;
      this.updateLayouts(layoutsData, layoutVisibilityChanged);
    }
  }

  private updateLayouts(layoutsData: DashboardLayoutsInfo, layoutVisibilityChanged: boolean) {
    for (const l of Object.keys(this.layouts)) {
      const layout: DashboardPageLayout = this.layouts[l];
      if (layoutsData[l]) {
        const layoutInfo: DashboardLayoutInfo = layoutsData[l];
        if (layout.layoutCtx.id === 'main') {
          layout.layoutCtx.ctrl.setResizing(layoutVisibilityChanged);
        }
        this.updateLayout(layout, layoutInfo);
      } else {
        this.updateLayout(layout, {widgets: [], widgetLayouts: {}, gridSettings: null});
      }
    }
  }

  private updateLayout(layout: DashboardPageLayout, layoutInfo: DashboardLayoutInfo) {
    if (layoutInfo.gridSettings) {
      layout.layoutCtx.gridSettings = layoutInfo.gridSettings;
    }
    layout.layoutCtx.widgets = layoutInfo.widgets;
    layout.layoutCtx.widgetLayouts = layoutInfo.widgetLayouts;
    if (layout.show && layout.layoutCtx.ctrl) {
      layout.layoutCtx.ctrl.reload();
    }
    layout.layoutCtx.ignoreLoading = true;
  }

  private setEditMode(isEdit: boolean, revert: boolean) {
    this.isEdit = isEdit;
    if (this.isEdit) {
      // TODO:
      // this.dashboardCtx.stateController.preserveState();
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
}

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

import { DashboardLayoutId, GridSettings, WidgetLayout, Dashboard, WidgetLayouts } from '@app/shared/models/dashboard.models';
import { Widget } from '@app/shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { ILayoutController } from './layout/layout.models';
import {
  DashboardContextMenuItem,
  WidgetContextMenuItem,
  WidgetPosition
} from '@home/models/dashboard-component.models';
import { Observable } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';

export declare type DashboardPageScope = 'tenant' | 'customer';

export interface DashboardContext {
  state: string;
  dashboard: Dashboard;
  dashboardTimewindow: Timewindow;
  aliasController: IAliasController;
  stateController: IStateController;
  runChangeDetection: () => void;
}

export interface IDashboardController {
  dashboardCtx: DashboardContext;
  openRightLayout();
  openDashboardState(stateId: string, openRightLayout: boolean);
  addWidget($event: Event, layoutCtx: DashboardPageLayoutContext);
  editWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget, index: number);
  exportWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget, index: number);
  removeWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  widgetMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget, index: number);
  widgetClicked($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget, index: number);
  prepareDashboardContextMenu(layoutCtx: DashboardPageLayoutContext): Array<DashboardContextMenuItem>;
  prepareWidgetContextMenu(layoutCtx: DashboardPageLayoutContext, widget: Widget, index: number): Array<WidgetContextMenuItem>;
  copyWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  copyWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  pasteWidget($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
  pasteWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
}

export interface DashboardPageLayoutContext {
  id: DashboardLayoutId;
  widgets: Array<Widget>;
  widgetLayouts: WidgetLayouts;
  gridSettings: GridSettings;
  ctrl: ILayoutController;
  dashboardCtrl: IDashboardController;
  ignoreLoading: boolean;
}

export interface DashboardPageLayout {
  show: boolean;
  layoutCtx: DashboardPageLayoutContext;
}

export declare type DashboardPageLayouts = {[key in DashboardLayoutId]: DashboardPageLayout};


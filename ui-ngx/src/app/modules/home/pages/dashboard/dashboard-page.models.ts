///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Dashboard, DashboardLayoutId, GridSettings, WidgetLayouts } from '@app/shared/models/dashboard.models';
import { Widget, WidgetPosition } from '@app/shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { ILayoutController } from './layout/layout.models';
import { DashboardContextMenuItem, WidgetContextMenuItem } from '@home/models/dashboard-component.models';

export declare type DashboardPageScope = 'tenant' | 'customer';

export interface DashboardContext {
  state: string;
  getDashboard: () => Dashboard;
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
  editWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  exportWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  removeWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  widgetMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  widgetClicked($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  prepareDashboardContextMenu(layoutCtx: DashboardPageLayoutContext): Array<DashboardContextMenuItem>;
  prepareWidgetContextMenu(layoutCtx: DashboardPageLayoutContext, widget: Widget): Array<WidgetContextMenuItem>;
  copyWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  copyWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  pasteWidget($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
  pasteWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
}

export interface DashboardPageLayoutContext {
  id: DashboardLayoutId;
  widgets: LayoutWidgetsArray;
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

export class LayoutWidgetsArray implements Iterable<Widget> {

  private widgetIds: string[] = [];

  private loaded = false;

  constructor(private dashboardCtx: DashboardContext) {
  }

  size() {
    return this.widgetIds.length;
  }

  isLoading() {
    return !this.loaded;
  }

  isEmpty() {
    return this.loaded && this.widgetIds.length === 0;
  }

  setWidgetIds(widgetIds: string[]) {
    this.widgetIds = widgetIds;
    this.loaded = true;
  }

  addWidgetId(widgetId: string) {
    this.widgetIds.push(widgetId);
  }

  removeWidgetId(widgetId: string): boolean {
    const index = this.widgetIds.indexOf(widgetId);
    if (index > -1) {
      this.widgetIds.splice(index, 1);
      return true;
    }
    return false;
  }

  [Symbol.iterator](): Iterator<Widget> {
    let pointer = 0;
    const widgetIds = this.widgetIds;
    const dashboard = this.dashboardCtx.getDashboard();
    return {
      next(value?: any): IteratorResult<Widget> {
        if (pointer < widgetIds.length) {
          const widgetId = widgetIds[pointer++];
          const widget = dashboard.configuration.widgets[widgetId];
          return {
            done: false,
            value: widget
          };
        } else {
          return {
            done: true,
            value: null
          };
        }
      }
    };
  }

  public widgetByIndex(index: number): Widget {
    const widgetId = this.widgetIds[index];
    if (widgetId) {
      return this.widgetById(widgetId);
    } else {
      return null;
    }
  }

  private widgetById(widgetId: string): Widget {
    return this.dashboardCtx.getDashboard().configuration.widgets[widgetId];
  }

}

// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  BreakpointId,
  Dashboard,
  DashboardLayoutId,
  DashboardLayoutInfo,
  GridSettings,
  WidgetLayouts
} from '@app/shared/models/dashboard.models';
import { Widget, WidgetPosition } from '@app/shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { ILayoutController } from './layout/layout.models';
import { DashboardContextMenuItem, WidgetContextMenuItem } from '@home/models/dashboard-component.models';
import { BehaviorSubject, Observable } from 'rxjs';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { ElementRef } from '@angular/core';

export declare type DashboardPageScope = 'tenant' | 'customer';

export interface DashboardPageInitData {
  dashboard: Dashboard;
  currentDashboardId?: string;
  widgetEditMode?: boolean;
  singlePageMode?: boolean;
  entityGroup?: EntityGroupInfo;
  customerId?: string;
}

export interface DashboardContext {
  instanceId: string;
  state: string;
  breakpoint: BreakpointId;
  getDashboard: () => Dashboard;
  dashboardTimewindow: Timewindow;
  aliasController: IAliasController;
  stateController: IStateController;
  stateChanged: Observable<string>;
  stateId: Observable<string>;
  runChangeDetection: () => void;
  dashboardCssClass?: string;
}

export interface IDashboardController {
  dashboardCtx: DashboardContext;
  dashboardContainer: ElementRef;
  dashboardContent: ElementRef;
  elRef: ElementRef;
  aiConfigurableForDashboard: boolean;
  openRightLayout();
  openDashboardState(stateId: string, openRightLayout: boolean);
  addWidget($event: Event, layoutCtx: DashboardPageLayoutContext);
  configureWithAi($event: Event);
  editWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  replaceReferenceWithWidgetCopy($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  exportWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget, widgetTitle: string);
  removeWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  widgetMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  dashboardMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext);
  widgetClicked($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  prepareDashboardContextMenu(layoutCtx: DashboardPageLayoutContext): Array<DashboardContextMenuItem>;
  prepareWidgetContextMenu(layoutCtx: DashboardPageLayoutContext, widget: Widget, isReference: boolean): Array<WidgetContextMenuItem>;
  copyWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  copyWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  pasteWidget($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
  pasteWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
}

export interface DashboardPageLayoutContext {
  id: DashboardLayoutId;
  layoutData: DashboardLayoutInfo;
  layoutDataChanged: BehaviorSubject<void>;
  breakpoint: BreakpointId;
  widgets: LayoutWidgetsArray;
  widgetLayouts: WidgetLayouts;
  gridSettings: GridSettings;
  ctrl: ILayoutController;
  dashboardCtrl: IDashboardController;
  ignoreLoading: boolean;
  displayGrid: 'always' | 'onDrag&Resize' | 'none';
}

export interface DashboardPageLayout {
  show: boolean;
  layoutCtx: DashboardPageLayoutContext;
}

export declare type DashboardPageLayouts = {[key in DashboardLayoutId]: DashboardPageLayout};

export class LayoutWidgetsArray implements Iterable<Widget> {

  private widgetIds: string[] = [];

  private widget: Widget;

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
    this.widget = null;
    this.loaded = true;
  }

  setWidget(widget: Widget) {
    this.widget = widget;
    this.widgetIds = [this.widget.id];
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
    const oneWidget = this.widget;
    const widgetIds = this.widgetIds;
    const dashboard = this.dashboardCtx.getDashboard();
    return {
      next(value?: any): IteratorResult<Widget> {
        if (pointer < widgetIds.length) {
          const widgetId = widgetIds[pointer++];
          let widget: Widget;
          if (oneWidget && oneWidget.id === widgetId) {
            widget = oneWidget;
          } else {
            widget = dashboard.configuration.widgets[widgetId];
          }
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
    if (this.widget && this.widget.id === widgetId) {
      return this.widget;
    } else {
      return this.dashboardCtx.getDashboard().configuration.widgets[widgetId];
    }
  }

}

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

import { GridsterComponent, GridsterConfig, GridsterItem, GridsterItemComponentInterface } from 'angular-gridster2';
import { Widget, WidgetPosition, widgetType } from '@app/shared/models/widget.models';
import { WidgetLayout, WidgetLayouts } from '@app/shared/models/dashboard.models';
import { IDashboardWidget, WidgetAction, WidgetContext, WidgetHeaderAction } from './widget-component.models';
import { Timewindow } from '@shared/models/time/time.models';
import { Observable, of, Subject } from 'rxjs';
import { guid, isDefined, isEqual, isUndefined } from '@app/core/utils';
import { IterableDiffer, KeyValueDiffer } from '@angular/core';
import { IAliasController, IStateController } from '@app/core/api/widget-api.models';

export interface WidgetsData {
  widgets: Array<Widget>;
  widgetLayouts?: WidgetLayouts;
}

export interface ContextMenuItem {
  enabled: boolean;
  shortcut?: string;
  icon: string;
  value: string;
}

export interface DashboardContextMenuItem extends ContextMenuItem {
  action: (contextMenuEvent: MouseEvent) => void;
}

export interface WidgetContextMenuItem extends ContextMenuItem {
  action: (contextMenuEvent: MouseEvent, widget: Widget) => void;
}

export interface DashboardCallbacks {
  onEditWidget?: ($event: Event, widget: Widget) => void;
  onExportWidget?: ($event: Event, widget: Widget) => void;
  onRemoveWidget?: ($event: Event, widget: Widget) => void;
  onWidgetMouseDown?: ($event: Event, widget: Widget) => void;
  onWidgetClicked?: ($event: Event, widget: Widget) => void;
  prepareDashboardContextMenu?: ($event: Event) => Array<DashboardContextMenuItem>;
  prepareWidgetContextMenu?: ($event: Event, widget: Widget) => Array<WidgetContextMenuItem>;
}

export interface IDashboardComponent {
  gridsterOpts: GridsterConfig;
  gridster: GridsterComponent;
  dashboardWidgets: DashboardWidgets;
  mobileAutofillHeight: boolean;
  isMobileSize: boolean;
  autofillHeight: boolean;
  dashboardTimewindow: Timewindow;
  dashboardTimewindowChanged: Observable<Timewindow>;
  aliasController: IAliasController;
  stateController: IStateController;
  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval?: number, persist?: boolean): void;
  onResetTimewindow(): void;
  resetHighlight(): void;
  highlightWidget(widgetId: string, delay?: number);
  selectWidget(widgetId: string, delay?: number);
  getSelectedWidget(): Widget;
  getEventGridPosition(event: Event): WidgetPosition;
  notifyGridsterOptionsChanged();
  pauseChangeNotifications();
  resumeChangeNotifications();
  notifyLayoutUpdated();
}

declare type DashboardWidgetUpdateOperation = 'add' | 'remove' | 'update';

interface DashboardWidgetUpdateRecord {
  widget?: Widget;
  widgetLayout?: WidgetLayout;
  widgetId: string;
  operation: DashboardWidgetUpdateOperation;
}

export class DashboardWidgets implements Iterable<DashboardWidget> {

  highlightedMode = false;

  dashboardWidgets: Array<DashboardWidget> = [];
  widgets: Iterable<Widget>;
  widgetLayouts: WidgetLayouts;

  [Symbol.iterator](): Iterator<DashboardWidget> {
    return this.dashboardWidgets[Symbol.iterator]();
  }

  constructor(private dashboard: IDashboardComponent,
              private widgetsDiffer: IterableDiffer<Widget>,
              private widgetLayoutsDiffer: KeyValueDiffer<string, WidgetLayout>) {
  }

  doCheck() {
    const widgetChange = this.widgetsDiffer.diff(this.widgets);
    const widgetLayoutChange = this.widgetLayoutsDiffer.diff(this.widgetLayouts);
    const updateRecords: Array<DashboardWidgetUpdateRecord> = [];

    if (widgetChange !== null) {
      widgetChange.forEachAddedItem((added) => {
        updateRecords.push({
          widget: added.item,
          widgetId: added.item.id,
          widgetLayout: this.widgetLayouts ? this.widgetLayouts[added.item.id] : null,
          operation: 'add'
        });
      });
      widgetChange.forEachRemovedItem((removed) => {
        let operation = updateRecords.find((record) => record.widgetId === removed.item.id);
        if (operation) {
          operation.operation = 'update';
        } else {
          operation = {
            widgetId: removed.item.id,
            operation: 'remove'
          };
          updateRecords.push(operation);
        }
      });
    }
    if (widgetLayoutChange !== null) {
      widgetLayoutChange.forEachChangedItem((changed) => {
        const operation = updateRecords.find((record) => record.widgetId === changed.key);
        if (!operation) {
          const widget = this.dashboardWidgets.find((dashboardWidget) => dashboardWidget.widgetId === changed.key);
          if (widget) {
            updateRecords.push({
              widget: widget.widget,
              widgetId: changed.key,
              widgetLayout: changed.currentValue,
              operation: 'update'
            });
          }
        }
      });
    }
    if (updateRecords.length) {
      updateRecords.forEach((record) => {
        switch (record.operation) {
          case 'add':
            this.dashboardWidgets.push(
              new DashboardWidget(this.dashboard, record.widget, record.widgetLayout)
            );
            break;
          case 'remove':
            let index = this.dashboardWidgets.findIndex((dashboardWidget) => dashboardWidget.widgetId === record.widgetId);
            if (index > -1) {
              this.dashboardWidgets.splice(index, 1);
            }
            break;
          case 'update':
            index = this.dashboardWidgets.findIndex((dashboardWidget) => dashboardWidget.widgetId === record.widgetId);
            if (index > -1) {
              const prevDashboardWidget = this.dashboardWidgets[index];
              if (!isEqual(prevDashboardWidget.widget, record.widget) ||
                  !isEqual(prevDashboardWidget.widgetLayout, record.widgetLayout)) {
                this.dashboardWidgets[index] = new DashboardWidget(this.dashboard, record.widget, record.widgetLayout);
                this.dashboardWidgets[index].highlighted = prevDashboardWidget.highlighted;
                this.dashboardWidgets[index].selected = prevDashboardWidget.selected;
              } else {
                this.dashboardWidgets[index].widget = record.widget;
                this.dashboardWidgets[index].widgetLayout = record.widgetLayout;
              }
            }
            break;
        }
      });
      this.updateRowsAndSort();
    }
  }

  widgetLayoutsUpdated() {
    for (const w of Object.keys(this.widgetLayouts)) {
      const widgetLayout = this.widgetLayouts[w];
      const index = this.dashboardWidgets.findIndex((dashboardWidget) => dashboardWidget.widgetId === w);
      if (index > -1) {
        this.dashboardWidgets[index].widgetLayout = widgetLayout;
      }
    }
    this.updateRowsAndSort();
  }

  setWidgets(widgets: Iterable<Widget>, widgetLayouts: WidgetLayouts) {
    this.highlightedMode = false;
    this.widgets = widgets;
    this.widgetLayouts = widgetLayouts;
  }

  highlightWidget(widgetId: string): DashboardWidget {
    const widget = this.findWidgetById(widgetId);
    if (widget && (!this.highlightedMode || !widget.highlighted || this.highlightedMode && widget.highlighted)) {
      this.highlightedMode = true;
      widget.highlighted = true;
      this.dashboardWidgets.forEach((dashboardWidget) => {
        if (dashboardWidget !== widget) {
          dashboardWidget.highlighted = false;
        }
      });
      return widget;
    } else {
      return null;
    }
  }

  selectWidget(widgetId: string): DashboardWidget {
    const widget = this.findWidgetById(widgetId);
    if (widget && (!widget.selected)) {
      widget.selected = true;
      this.dashboardWidgets.forEach((dashboardWidget) => {
        if (dashboardWidget !== widget) {
          dashboardWidget.selected = false;
        }
      });
      return widget;
    } else {
      return null;
    }
  }

  resetHighlight(): DashboardWidget {
    const highlighted = this.dashboardWidgets.find((dashboardWidget) => dashboardWidget.highlighted);
    this.highlightedMode = false;
    this.dashboardWidgets.forEach((dashboardWidget) => {
      dashboardWidget.highlighted = false;
      dashboardWidget.selected = false;
    });
    return highlighted;
  }

  isHighlighted(widget: DashboardWidget): boolean {
    return (this.highlightedMode && widget.highlighted) || (widget.selected);
  }

  isNotHighlighted(widget: DashboardWidget): boolean {
    return this.highlightedMode && !widget.highlighted;
  }

  getSelectedWidget(): DashboardWidget {
    return this.dashboardWidgets.find((dashboardWidget) => dashboardWidget.selected);
  }

  private findWidgetById(widgetId: string): DashboardWidget {
    return this.dashboardWidgets.find((dashboardWidget) => dashboardWidget.widgetId === widgetId);
  }

  private updateRowsAndSort() {
    let maxRows = this.dashboard.gridsterOpts.maxRows;
    this.dashboardWidgets.forEach((dashboardWidget) => {
      const bottom = dashboardWidget.y + dashboardWidget.rows;
      maxRows = Math.max(maxRows, bottom);
    });
    this.sortWidgets();
    this.dashboard.gridsterOpts.maxRows = maxRows;
    this.dashboard.notifyGridsterOptionsChanged();
  }

  sortWidgets() {
    this.dashboardWidgets.sort((widget1, widget2) => {
      const row1 = widget1.widgetOrder;
      const row2 = widget2.widgetOrder;
      let res = row1 - row2;
      if (res === 0) {
        res = widget1.x - widget2.x;
      }
      return res;
    });
  }

}

export class DashboardWidget implements GridsterItem, IDashboardWidget {

  highlighted = false;
  selected = false;

  isFullscreen = false;

  color: string;
  backgroundColor: string;
  padding: string;
  margin: string;

  title: string;
  titleTooltip: string;
  showTitle: boolean;
  titleStyle: {[klass: string]: any};

  titleIcon: string;
  showTitleIcon: boolean;
  titleIconStyle: {[klass: string]: any};

  dropShadow: boolean;
  enableFullscreen: boolean;

  hasTimewindow: boolean;

  hasAggregation: boolean;

  style: {[klass: string]: any};

  showWidgetTitlePanel: boolean;
  showWidgetActions: boolean;

  customHeaderActions: Array<WidgetHeaderAction>;
  widgetActions: Array<WidgetAction>;

  widgetContext = new WidgetContext(this.dashboard, this, this.widget);

  widgetId: string;

  private gridsterItemComponentSubject = new Subject<GridsterItemComponentInterface>();
  private gridsterItemComponentValue: GridsterItemComponentInterface;

  set gridsterItemComponent(item: GridsterItemComponentInterface) {
    this.gridsterItemComponentValue = item;
    this.gridsterItemComponentSubject.next(this.gridsterItemComponentValue);
    this.gridsterItemComponentSubject.complete();
  }

  constructor(
    private dashboard: IDashboardComponent,
    public widget: Widget,
    public widgetLayout?: WidgetLayout) {
    if (!widget.id) {
      widget.id = guid();
    }
    this.widgetId = widget.id;
    this.updateWidgetParams();
  }

  gridsterItemComponent$(): Observable<GridsterItemComponentInterface> {
    if (this.gridsterItemComponentValue) {
      return of(this.gridsterItemComponentValue);
    } else {
      return this.gridsterItemComponentSubject.asObservable();
    }
  }

  updateWidgetParams() {
    this.color = this.widget.config.color || 'rgba(0, 0, 0, 0.87)';
    this.backgroundColor = this.widget.config.backgroundColor || '#fff';
    this.padding = this.widget.config.padding || '8px';
    this.margin = this.widget.config.margin || '0px';

    this.title = isDefined(this.widgetContext.widgetTitle)
      && this.widgetContext.widgetTitle.length ? this.widgetContext.widgetTitle : this.widget.config.title;
    this.titleTooltip = isDefined(this.widgetContext.widgetTitleTooltip)
      && this.widgetContext.widgetTitleTooltip.length ? this.widgetContext.widgetTitleTooltip : this.widget.config.titleTooltip;
    this.showTitle = isDefined(this.widget.config.showTitle) ? this.widget.config.showTitle : true;
    this.titleStyle = this.widget.config.titleStyle ? this.widget.config.titleStyle : {};

    this.titleIcon = isDefined(this.widget.config.titleIcon) ? this.widget.config.titleIcon : '';
    this.showTitleIcon = isDefined(this.widget.config.showTitleIcon) ? this.widget.config.showTitleIcon : false;
    this.titleIconStyle = {};
    if (this.widget.config.iconColor) {
      this.titleIconStyle.color = this.widget.config.iconColor;
    }
    if (this.widget.config.iconSize) {
      this.titleIconStyle.fontSize = this.widget.config.iconSize;
    }

    this.dropShadow = isDefined(this.widget.config.dropShadow) ? this.widget.config.dropShadow : true;
    this.enableFullscreen = isDefined(this.widget.config.enableFullscreen) ? this.widget.config.enableFullscreen : true;

    this.hasTimewindow = (this.widget.type === widgetType.timeseries || this.widget.type === widgetType.alarm) ?
      (isDefined(this.widget.config.useDashboardTimewindow) ?
        (!this.widget.config.useDashboardTimewindow && (isUndefined(this.widget.config.displayTimewindow)
          || this.widget.config.displayTimewindow)) : false)
      : false;

    this.hasAggregation = this.widget.type === widgetType.timeseries;

    this.style = {cursor: 'pointer',
      color: this.color,
      backgroundColor: this.backgroundColor,
      padding: this.padding,
      margin: this.margin};
    if (this.widget.config.widgetStyle) {
      this.style = {...this.style, ...this.widget.config.widgetStyle};
    }

    this.showWidgetTitlePanel = this.widgetContext.hideTitlePanel ? false :
      this.showTitle || this.hasTimewindow;

    this.showWidgetActions = this.widgetContext.hideTitlePanel ? false : true;

    this.customHeaderActions = this.widgetContext.customHeaderActions ? this.widgetContext.customHeaderActions : [];
    this.widgetActions = this.widgetContext.widgetActions ? this.widgetContext.widgetActions : [];
  }

  get x(): number {
    let res;
    if (this.widgetLayout) {
      res = this.widgetLayout.col;
    } else {
      res = this.widget.col;
    }
    return Math.floor(res);
  }

  set x(x: number) {
    if (!this.dashboard.isMobileSize) {
      if (this.widgetLayout) {
        this.widgetLayout.col = x;
      } else {
        this.widget.col = x;
      }
    }
  }

  get y(): number {
    let res;
    if (this.widgetLayout) {
      res = this.widgetLayout.row;
    } else {
      res = this.widget.row;
    }
    return Math.floor(res);
  }

  set y(y: number) {
    if (!this.dashboard.isMobileSize) {
      if (this.widgetLayout) {
        this.widgetLayout.row = y;
      } else {
        this.widget.row = y;
      }
    }
  }

  get cols(): number {
    let res;
    if (this.widgetLayout) {
      res = this.widgetLayout.sizeX;
    } else {
      res = this.widget.sizeX;
    }
    return Math.floor(res);
  }

  set cols(cols: number) {
    if (!this.dashboard.isMobileSize) {
      if (this.widgetLayout) {
        this.widgetLayout.sizeX = cols;
      } else {
        this.widget.sizeX = cols;
      }
    }
  }

  get rows(): number {
    let res;
    if (this.dashboard.isMobileSize && !this.dashboard.mobileAutofillHeight) {
      let mobileHeight;
      if (this.widgetLayout) {
        mobileHeight = this.widgetLayout.mobileHeight;
      }
      if (!mobileHeight && this.widget.config.mobileHeight) {
        mobileHeight = this.widget.config.mobileHeight;
      }
      if (mobileHeight) {
        res = mobileHeight;
      } else {
        const sizeY = this.widgetLayout ? this.widgetLayout.sizeY : this.widget.sizeY;
        res = sizeY * 24 / this.dashboard.gridsterOpts.minCols;
      }
    } else {
      if (this.widgetLayout) {
        res = this.widgetLayout.sizeY;
      } else {
        res = this.widget.sizeY;
      }
    }
    return Math.floor(res);
  }

  set rows(rows: number) {
    if (!this.dashboard.isMobileSize && !this.dashboard.autofillHeight) {
      if (this.widgetLayout) {
        this.widgetLayout.sizeY = rows;
      } else {
        this.widget.sizeY = rows;
      }
    }
  }

  get widgetOrder(): number {
    let order;
    if (this.widgetLayout && isDefined(this.widgetLayout.mobileOrder) && this.widgetLayout.mobileOrder >= 0) {
      order = this.widgetLayout.mobileOrder;
    } else if (isDefined(this.widget.config.mobileOrder) && this.widget.config.mobileOrder >= 0) {
      order = this.widget.config.mobileOrder;
    } else if (this.widgetLayout) {
      order = this.widgetLayout.row;
    } else {
      order = this.widget.row;
    }
    return order;
  }
}

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

import { GridsterComponent, GridsterConfig, GridsterItem, GridsterItemComponentInterface } from 'angular-gridster2';
import {
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  FormattedData,
  Widget,
  WidgetPosition,
  widgetType
} from '@app/shared/models/widget.models';
import { WidgetLayout, WidgetLayouts } from '@app/shared/models/dashboard.models';
import { IDashboardWidget, WidgetAction, WidgetContext, WidgetHeaderAction } from './widget-component.models';
import { Timewindow } from '@shared/models/time/time.models';
import { Observable, of, Subject } from 'rxjs';
import {
  convertKeysToCamelCase,
  formattedDataFormDatasourceData,
  guid,
  isDefined,
  isEmpty,
  isEqual,
  isUndefined
} from '@app/core/utils';
import { IterableDiffer, KeyValueDiffer } from '@angular/core';
import { IAliasController, IStateController } from '@app/core/api/widget-api.models';
import { enumerable } from '@shared/decorators/enumerable';
import { UtilsService } from '@core/services/utils.service';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ComponentStyle, iconStyle, textStyle } from '@shared/models/widget-settings.models';
import { TbContextMenuEvent } from '@shared/models/jquery-event.models';

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
  action: (contextMenuEvent: TbContextMenuEvent) => void;
}

export interface WidgetContextMenuItem extends ContextMenuItem {
  action: (contextMenuEvent: TbContextMenuEvent, widget: Widget) => void;
}

export interface DashboardCallbacks {
  onEditWidget?: ($event: Event, widget: Widget) => void;
  replaceReferenceWithWidgetCopy?: ($event: Event, widget: Widget) => void;
  onExportWidget?: ($event: Event, widget: Widget, widgeTitle: string) => void;
  onRemoveWidget?: ($event: Event, widget: Widget) => void;
  onWidgetMouseDown?: ($event: Event, widget: Widget) => void;
  onDashboardMouseDown?: ($event: Event) => void;
  onWidgetClicked?: ($event: Event, widget: Widget) => void;
  prepareDashboardContextMenu?: ($event: Event) => Array<DashboardContextMenuItem>;
  prepareWidgetContextMenu?: ($event: Event, widget: Widget, isReference: boolean) => Array<WidgetContextMenuItem>;
}

export interface IDashboardComponent {
  utils: UtilsService;
  gridsterOpts: GridsterConfig;
  gridster: GridsterComponent;
  dashboardWidgets: DashboardWidgets;
  mobileAutofillHeight: boolean;
  isMobileSize: boolean;
  isEdit: boolean;
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
  getEventGridPosition(event: TbContextMenuEvent | KeyboardEvent): WidgetPosition;
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

export const maxGridsterCol = 3000;
export const maxGridsterRow = 3000;

export class DashboardWidgets implements Iterable<DashboardWidget> {

  highlightedMode = false;

  dashboardWidgets: Array<DashboardWidget> = [];
  widgets: Iterable<Widget>;
  widgetLayouts: WidgetLayouts;

  parentDashboard?: IDashboardComponent;

  popoverComponent?: TbPopoverComponent;

  [Symbol.iterator](): Iterator<DashboardWidget> {
    return this.activeDashboardWidgets[Symbol.iterator]();
  }

  get activeDashboardWidgets(): Array<DashboardWidget> {
    if (!this.dashboard.isEdit) {
      if (this.dashboard.isMobileSize) {
        return this.dashboardWidgets.filter(w => !w.mobileHide);
      } else {
        return this.dashboardWidgets.filter(w => !w.desktopHide);
      }
    }
    return this.dashboardWidgets;
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
        let index;
        switch (record.operation) {
          case 'add':
            this.dashboardWidgets.push(
              new DashboardWidget(this.dashboard, record.widget, record.widgetLayout, this.parentDashboard, this.popoverComponent)
            );
            break;
          case 'remove':
            index = this.dashboardWidgets.findIndex((dashboardWidget) => dashboardWidget.widgetId === record.widgetId);
            if (index > -1) {
              this.dashboardWidgets.splice(index, 1);
            }
            break;
          case 'update':
            index = this.dashboardWidgets.findIndex((dashboardWidget) => dashboardWidget.widgetId === record.widgetId);
            if (index > -1) {
              const prevDashboardWidget = this.dashboardWidgets[index];
              if (!isEqual(prevDashboardWidget.widget, record.widget)) {
                this.dashboardWidgets[index] = new DashboardWidget(this.dashboard, record.widget, record.widgetLayout,
                  this.parentDashboard, this.popoverComponent);
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
      let detectChanges = false;
      if (!this.highlightedMode) {
        this.highlightedMode = true;
        detectChanges = true;
      }
      widget.highlighted = true;
      widget.selected = false;
      this.dashboardWidgets.forEach((dashboardWidget) => {
        if (dashboardWidget !== widget) {
          dashboardWidget.highlighted = false;
          dashboardWidget.selected = false;
          if (detectChanges) {
            dashboardWidget.widgetContext?.detectContainerChanges();
          }
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

  updateRowsAndSort() {
    let maxRows = this.dashboard.gridsterOpts.maxRows;
    this.activeDashboardWidgets.forEach((dashboardWidget) => {
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
      if (isDefined(row1) && isUndefined(row2)) {
        return -1;
      } else if (isUndefined(row1) && isDefined(row2)) {
        return 1;
      } else if (isUndefined(row1) && isUndefined(row2)) {
        return 0;
      }
      let res = row1 - row2;
      if (res === 0) {
        res = widget1.x - widget2.x;
      }
      return res;
    });
  }

}

export class DashboardWidget implements GridsterItem, IDashboardWidget {

  private highlightedValue = false;
  private selectedValue = false;
  private selectedCallback: (selected: boolean) => void = () => {};

  resizableHandles = {} as any;

  resizeEnabled = true;

  isFullscreen = false;
  isReference = false;

  color: string;
  backgroundColor: string;
  padding: string;
  margin: string;
  borderRadius: string;

  title$: Observable<string>;
  titleTooltip: string;
  showTitle: boolean;
  titleStyle: ComponentStyle;

  titleIcon: string;
  showTitleIcon: boolean;
  titleIconStyle: ComponentStyle;

  dropShadow: boolean;
  enableFullscreen: boolean;

  hasTimewindow: boolean;

  hasAggregation: boolean;

  onlyQuickInterval: boolean;

  onlyHistoryTimewindow: boolean;

  style: ComponentStyle;

  showWidgetTitlePanel: boolean;
  showWidgetActions: boolean;

  customHeaderActions: Array<WidgetHeaderAction>;
  widgetActions: Array<WidgetAction>;

  widgetContext = new WidgetContext(this.dashboard, this, this.widget, this.parentDashboard, this.popoverComponent);

  widgetId: string;

  private gridsterItemComponentSubject = new Subject<GridsterItemComponentInterface>();
  private gridsterItemComponentValue: GridsterItemComponentInterface;

  private aspectRatio: number;

  private heightValue: number;
  private widthValue: number;

  private rowsValue: number;
  private colsValue: number;


  get mobileHide(): boolean {
    return this.widgetLayout ? this.widgetLayout.mobileHide === true : false;
  }

  get desktopHide(): boolean {
    return this.widgetLayout ? this.widgetLayout.desktopHide === true : false;
  }

  set gridsterItemComponent(item: GridsterItemComponentInterface) {
    this.gridsterItemComponentValue = item;

    if (this.widgetLayout?.preserveAspectRatio) {
      this.applyPreserveAspectRatio(item);
    }

    this.gridsterItemComponentSubject.next(this.gridsterItemComponentValue);
    this.gridsterItemComponentSubject.complete();
  }

  private preserveAspectRatioApplied = false;

  private applyPreserveAspectRatio(item: GridsterItemComponentInterface) {

    if (this.widgetLayout?.preserveAspectRatio) {
      this.resizableHandles.ne = false;
      this.resizableHandles.sw = false;
      this.resizableHandles.nw = false;
    } else {
      this.resizableHandles.ne = true;
      this.resizableHandles.sw = true;
      this.resizableHandles.nw = true;
    }

    if (!this.preserveAspectRatioApplied) {
      const $item = item.$item;

      this.rowsValue = $item.rows;
      this.colsValue = $item.cols;

      Object.defineProperty($item, 'rows', {
        get: () => this.rowsValue,
        set: v => {
          if (this.rowsValue !== v) {
            if (this.preserveAspectRatio) {
              this.colsValue = v * this.aspectRatio;
            }
            this.rowsValue = v;
          }
        }
      });

      Object.defineProperty($item, 'cols', {
        get: () => this.colsValue,
        set: v => {
          if (this.colsValue !== v) {
            if (this.preserveAspectRatio) {
              this.rowsValue = v / this.aspectRatio;
            }
            this.colsValue = v;
          }
        }
      });

      const resizable = item.resize;

      if (resizable) {
        this.heightValue = resizable.height;
        this.widthValue = resizable.width;

        const setItemHeight = resizable.setItemHeight.bind(resizable);
        const setItemWidth = resizable.setItemWidth.bind(resizable);
        resizable.setItemHeight = (height) => {
          setItemHeight(height);
          this.heightValue = height;
          if (this.preserveAspectRatio) {
            setItemWidth(height * this.aspectRatio);
          }
        };
        resizable.setItemWidth = (width) => {
          setItemWidth(width);
          this.widthValue = width;
          if (this.preserveAspectRatio) {
            setItemHeight(width / this.aspectRatio);
          }
        };

        Object.defineProperty(resizable, 'height', {
          get: () => this.heightValue,
          set: v => {
            if (this.heightValue !== v) {
              if (this.preserveAspectRatio) {
                this.widthValue = v * this.aspectRatio;
              }
              this.heightValue = v;
            }
          }
        });

        Object.defineProperty(resizable, 'width', {
          get: () => this.widthValue,
          set: v => {
            if (this.widthValue !== v) {
              if (this.preserveAspectRatio) {
                this.heightValue = v / this.aspectRatio;
              }
              this.widthValue = v;
            }
          }
        });
      }
      this.preserveAspectRatioApplied = true;
    }
  }

  get highlighted() {
    return this.highlightedValue;
  }

  set highlighted(highlighted: boolean) {
    if (this.highlightedValue !== highlighted) {
      this.highlightedValue = highlighted;
      this.widgetContext.detectContainerChanges();
    }
  }

  onSelected(selectedCallback: (selected: boolean) => void) {
    this.selectedCallback = selectedCallback;
  }

  get selected() {
    return this.selectedValue;
  }

  set selected(selected: boolean) {
    if (this.selectedValue !== selected) {
      this.selectedValue = selected;
      this.selectedCallback(selected);
      this.widgetContext.detectContainerChanges();
    }
  }

  get widgetLayout(): WidgetLayout {
    return this.widgetLayoutValue;
  }

  set widgetLayout(value: WidgetLayout) {
    this.widgetLayoutValue = value;
    this._widgetLayoutUpdated();
  }

  private _widgetLayoutUpdated() {
    if (isDefined(this.widgetLayout?.resizable)) {
      this.resizeEnabled = this.widgetLayout.resizable;
    }
    if (this.widgetLayout?.preserveAspectRatio) {
      this.aspectRatio = this.widgetLayout.sizeX / this.widgetLayout.sizeY;
    }
    if (this.gridsterItemComponentValue) {
      this.applyPreserveAspectRatio(this.gridsterItemComponentValue);
    }
  }

  constructor(
    private dashboard: IDashboardComponent,
    public widget: Widget,
    private widgetLayoutValue?: WidgetLayout,
    private parentDashboard?: IDashboardComponent,
    private popoverComponent?: TbPopoverComponent) {

    this.widgetLayout = widgetLayoutValue;

    if (!widget.id) {
      widget.id = guid();
    }
    this.widgetId = widget.id;
    this.updateWidgetParams(false);
  }

  gridsterItemComponent$(): Observable<GridsterItemComponentInterface> {
    if (this.gridsterItemComponentValue) {
      return of(this.gridsterItemComponentValue);
    } else {
      return this.gridsterItemComponentSubject.asObservable();
    }
  }

  updateWidgetParams(detectChanges = true) {
    this.color = this.widget.config.color || 'rgba(0, 0, 0, 0.87)';
    this.backgroundColor = this.widget.config.backgroundColor || '#fff';
    this.padding = this.widget.config.padding || '8px';
    this.margin = this.widget.config.margin || '0px';
    this.borderRadius = this.widget.config.borderRadius;

    const title = isDefined(this.widgetContext.widgetTitle)
      && this.widgetContext.widgetTitle.length ? this.widgetContext.widgetTitle : this.widget.config.title;
    this.title$ = this.widgetContext.registerLabelPattern(title, this.title$);
    this.titleTooltip = isDefined(this.widgetContext.widgetTitleTooltip)
      && this.widgetContext.widgetTitleTooltip.length ? this.widgetContext.widgetTitleTooltip : this.widget.config.titleTooltip;
    this.titleTooltip = this.dashboard.utils.customTranslation(this.titleTooltip, this.titleTooltip);
    this.showTitle = isDefined(this.widget.config.showTitle) ? this.widget.config.showTitle : true;
    this.titleStyle = {...(this.widget.config.titleStyle || {}), ...textStyle(this.widget.config.titleFont)};
    if (this.widget.config.titleColor) {
      this.titleStyle.color = this.widget.config.titleColor;
    }
    this.titleIcon = isDefined(this.widget.config.titleIcon) ? this.widget.config.titleIcon : '';
    this.showTitleIcon = isDefined(this.widget.config.showTitleIcon) ? this.widget.config.showTitleIcon : false;
    this.titleIconStyle = this.widget.config.iconSize ? iconStyle(this.widget.config.iconSize) : {};
    if (this.widget.config.iconColor) {
      this.titleIconStyle.color = this.widget.config.iconColor;
    }
    this.dropShadow = isDefined(this.widget.config.dropShadow) ? this.widget.config.dropShadow : true;
    this.enableFullscreen = isDefined(this.widget.config.enableFullscreen) ? this.widget.config.enableFullscreen : true;

    let canHaveTimewindow = false;
    let onlyQuickInterval = false;
    let onlyHistoryTimewindow = false;
    if (this.widget.type === widgetType.timeseries || this.widget.type === widgetType.alarm) {
      canHaveTimewindow = true;
    } else if (this.widget.type === widgetType.latest) {
      canHaveTimewindow = datasourcesHasAggregation(this.widget.config.datasources);
      onlyQuickInterval = canHaveTimewindow;
      if (canHaveTimewindow) {
        onlyHistoryTimewindow = datasourcesHasOnlyComparisonAggregation(this.widget.config.datasources);
      }
    }

    this.hasTimewindow = canHaveTimewindow ?
      (isDefined(this.widget.config.useDashboardTimewindow) ?
        (!this.widget.config.useDashboardTimewindow && (isUndefined(this.widget.config.displayTimewindow)
          || this.widget.config.displayTimewindow)) : false)
      : false;

    this.onlyQuickInterval = onlyQuickInterval;
    this.onlyHistoryTimewindow = onlyHistoryTimewindow;

    this.hasAggregation = this.widget.type === widgetType.timeseries;

    this.style = {
      color: this.color,
      backgroundColor: this.backgroundColor,
      padding: this.padding,
      margin: this.margin,
      borderRadius: this.borderRadius };
    if (!isEmpty(this.widget.config.widgetStyle)) {
      this.style = {...this.style, ...convertKeysToCamelCase(this.widget.config.widgetStyle)};
    }

    this.showWidgetTitlePanel = this.widgetContext.hideTitlePanel ? false :
      this.showTitle || this.hasTimewindow;

    this.showWidgetActions = !this.widgetContext.hideTitlePanel;

    this.updateParamsFromData();
    this.widgetActions = this.widgetContext.widgetActions ? this.widgetContext.widgetActions : [];
    if (detectChanges) {
      this.widgetContext.detectContainerChanges();
    }
  }

  updateParamsFromData(detectChanges = false) {
    this.widgetContext.updateLabelPatterns();
    const update = this.updateCustomHeaderActions();
    if (update && detectChanges) {
      this.widgetContext.detectContainerChanges();
    }
  }

  private updateCustomHeaderActions(): boolean {
    let customHeaderActions: Array<WidgetHeaderAction>;
    if (this.widgetContext.customHeaderActions) {
      let data: FormattedData[] = [];
      if (this.widgetContext.customHeaderActions.some(action => action.useShowWidgetHeaderActionFunction)) {
        data = formattedDataFormDatasourceData(this.widgetContext.data || []);
      }
      customHeaderActions = this.widgetContext.customHeaderActions.filter(action => this.filterCustomHeaderAction(action, data));
    } else {
      customHeaderActions = [];
    }
    if (!isEqual(this.customHeaderActions, customHeaderActions)) {
      this.customHeaderActions = customHeaderActions;
      return true;
    }
    return false;
  }

  private filterCustomHeaderAction(action: WidgetHeaderAction, data: FormattedData[]): boolean {
    if (action.useShowWidgetHeaderActionFunction) {
      try {
        return action.showWidgetHeaderActionFunction.execute(this.widgetContext, data);
      } catch (e) {
        console.warn('Failed to execute showWidgetHeaderActionFunction', e);
        return false;
      }
    } else {
      return true;
    }
  }

  @enumerable(true)
  get x(): number {
    let res;
    if (this.widgetLayout) {
      res = this.widgetLayout.col;
    } else {
      res = this.widget.col;
    }
    return Math.floor(res);
  }

  set x(_: number) {}

  @enumerable(true)
  get y(): number {
    let res;
    if (this.widgetLayout) {
      res = this.widgetLayout.row;
    } else {
      res = this.widget.row;
    }
    return Math.floor(res);
  }

  set y(_: number) {}

  get preserveAspectRatio(): boolean {
    if (!this.dashboard.isMobileSize && this.widgetLayout) {
      return this.widgetLayout.preserveAspectRatio;
    } else {
      return false;
    }
  }

  @enumerable(true)
  get cols(): number {
    return Math.floor(this.sizeX);
  }

  set cols(cols: number) {
    if (!this.dashboard.isMobileSize) {
      this.sizeX = cols;
    }
  }

  @enumerable(true)
  get rows(): number {
    let res: number;
    if (this.dashboard.isMobileSize) {
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
        const sizeY = this.sizeY;
        res = sizeY * 24 / this.dashboard.gridsterOpts.minCols;
      }
    } else {
      res = this.sizeY;
    }
    return Math.max(Math.floor(res), 1);
  }

  set rows(rows: number) {
    if (!this.dashboard.isMobileSize && !this.dashboard.autofillHeight) {
      this.sizeY = rows;
    }
  }

  get sizeX(): number {
    if (this.widgetLayout) {
      return this.widgetLayout.sizeX;
    } else {
      return this.widget.sizeX;
    }
  }

  set sizeX(sizeX: number) {
    if (this.widgetLayout) {
      this.widgetLayout.sizeX = sizeX;
    } else {
      this.widget.sizeX = sizeX;
    }
  }

  get sizeY(): number {
    if (this.widgetLayout) {
      return this.widgetLayout.sizeY;
    } else {
      return this.widget.sizeY;
    }
  }

  set sizeY(sizeY: number) {
    if (this.widgetLayout) {
      this.widgetLayout.sizeY = sizeY;
    } else {
      this.widget.sizeY = sizeY;
    }
  }

  @enumerable(true)
  get widgetOrder(): number {
    let order;
    if (this.widgetLayout && isDefined(this.widgetLayout.mobileOrder) && this.widgetLayout.mobileOrder >= 0) {
      order = this.widgetLayout.mobileOrder;
    } else if (isDefined(this.widget.config.mobileOrder) && this.widget.config.mobileOrder >= 0) {
      order = this.widget.config.mobileOrder;
    } else if (!this.dashboard.isMobileSize) {
      if (this.widgetLayout) {
        order = this.widgetLayout.row;
      } else {
        order = this.widget.row;
      }
    }
    return order;
  }

  updatePosition(x: number, y: number) {
    if (this.widgetLayout) {
      this.widgetLayout.col = x;
      this.widgetLayout.row = y;
    } else {
      this.widget.col = x;
      this.widget.row = y;
    }
  }
}

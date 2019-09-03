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

import { GridsterConfig, GridsterItem, GridsterComponent } from 'angular-gridster2';
import { Widget, widgetType } from '@app/shared/models/widget.models';
import { WidgetLayout, WidgetLayouts } from '@app/shared/models/dashboard.models';
import { WidgetAction, WidgetContext, WidgetHeaderAction } from './widget-component.models';
import { Timewindow } from '@shared/models/time/time.models';
import { Observable } from 'rxjs';
import { isDefined, isUndefined } from '@app/core/utils';
import { EventEmitter } from '@angular/core';

export interface IAliasController {
  [key: string]: any | null;
  // TODO:
}

export interface WidgetsData {
  widgets: Array<Widget>;
  widgetLayouts?: WidgetLayouts;
}

export class DashboardConfig {
  widgetsData?: Observable<WidgetsData>;
  isEdit: boolean;
  isEditActionEnabled: boolean;
  isExportActionEnabled: boolean;
  isRemoveActionEnabled: boolean;
  onEditWidget?: ($event: Event, widget: Widget) => void;
  onExportWidget?: ($event: Event, widget: Widget) => void;
  onRemoveWidget?: ($event: Event, widget: Widget) => void;
  onWidgetMouseDown?: ($event: Event, widget: Widget) => void;
  onWidgetClicked?: ($event: Event, widget: Widget) => void;
  aliasController?: IAliasController;
  autofillHeight?: boolean;
  mobileAutofillHeight?: boolean;
  dashboardStyle?: {[klass: string]: any} | null;
  columns?: number;
  margins?: [number, number];
  dashboardTimewindow?: Timewindow;
  ignoreLoading?: boolean;
  dashboardClass?: string;
  mobileRowHeight?: number;

  private isMobileValue: boolean;
  private isMobileDisabledValue: boolean;

  private layoutChange = new EventEmitter();
  layoutChange$ = this.layoutChange.asObservable();
  layoutChangeTimeout = null;

  set isMobile(isMobile: boolean) {
    if (this.isMobileValue !== isMobile) {
      const changed = isDefined(this.isMobileValue);
      this.isMobileValue = isMobile;
      if (changed) {
        this.notifyLayoutChanged();
      }
    }
  }
  get isMobile(): boolean {
    return this.isMobileValue;
  }

  set isMobileDisabled(isMobileDisabled: boolean) {
    if (this.isMobileDisabledValue !== isMobileDisabled) {
      const changed = isDefined(this.isMobileDisabledValue);
      this.isMobileDisabledValue = isMobileDisabled;
      if (changed) {
        this.notifyLayoutChanged();
      }
    }
  }
  get isMobileDisabled(): boolean {
    return this.isMobileDisabledValue;
  }

  private notifyLayoutChanged() {
    if (this.layoutChangeTimeout) {
      clearTimeout(this.layoutChangeTimeout);
    }
    this.layoutChangeTimeout = setTimeout(() => {
      this.doNotifyLayoutChanged();
    }, 0);
  }

  private doNotifyLayoutChanged() {
    this.layoutChange.emit();
    this.layoutChangeTimeout = null;
  }
}

export interface IDashboardComponent {
  options: DashboardConfig;
  gridsterOpts: GridsterConfig;
  gridster: GridsterComponent;
  isMobileSize: boolean;
}

export class DashboardWidget implements GridsterItem {

  isFullscreen = false;

  color: string;
  backgroundColor: string;
  padding: string;
  margin: string;

  title: string;
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

  hasWidgetTitleTemplate: boolean;
  widgetTitleTemplate: string;

  showWidgetTitlePanel: boolean;
  showWidgetActions: boolean;

  customHeaderActions: Array<WidgetHeaderAction>;
  widgetActions: Array<WidgetAction>;

  widgetContext: WidgetContext = {};

  constructor(
    private dashboard: IDashboardComponent,
    public widget: Widget,
    private widgetLayout?: WidgetLayout) {
    this.updateWidgetParams();
  }

  updateWidgetParams() {
    this.color = this.widget.config.color || 'rgba(0, 0, 0, 0.87)';
    this.backgroundColor = this.widget.config.backgroundColor || '#fff';
    this.padding = this.widget.config.padding || '8px';
    this.margin = this.widget.config.margin || '0px';

    this.title = isDefined(this.widgetContext.widgetTitle)
      && this.widgetContext.widgetTitle.length ? this.widgetContext.widgetTitle : this.widget.config.title;
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
      this.style = {...this.widget.config.widgetStyle, ...this.style};
    }

    this.hasWidgetTitleTemplate = this.widgetContext.widgetTitleTemplate ? true : false;
    this.widgetTitleTemplate = this.widgetContext.widgetTitleTemplate ? this.widgetContext.widgetTitleTemplate : '';

    this.showWidgetTitlePanel = this.widgetContext.hideTitlePanel ? false :
      this.hasWidgetTitleTemplate || this.showTitle || this.hasTimewindow;

    this.showWidgetActions = this.widgetContext.hideTitlePanel ? false : true;

    this.customHeaderActions = this.widgetContext.customHeaderActions ? this.widgetContext.customHeaderActions : [];
    this.widgetActions = this.widgetContext.widgetActions ? this.widgetContext.widgetActions : [];
  }

  get x(): number {
    if (this.widgetLayout) {
      return this.widgetLayout.col;
    } else {
      return this.widget.col;
    }
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
    if (this.widgetLayout) {
      return this.widgetLayout.row;
    } else {
      return this.widget.row;
    }
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
    if (this.widgetLayout) {
      return this.widgetLayout.sizeX;
    } else {
      return this.widget.sizeX;
    }
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
    if (this.dashboard.isMobileSize && !this.dashboard.options.mobileAutofillHeight) {
      let mobileHeight;
      if (this.widgetLayout) {
        mobileHeight = this.widgetLayout.mobileHeight;
      }
      if (!mobileHeight && this.widget.config.mobileHeight) {
        mobileHeight = this.widget.config.mobileHeight;
      }
      if (mobileHeight) {
        return mobileHeight;
      } else {
        return this.widget.sizeY * 24 / this.dashboard.gridsterOpts.minCols;
      }
    } else {
      if (this.widgetLayout) {
        return this.widgetLayout.sizeY;
      } else {
        return this.widget.sizeY;
      }
    }
  }

  set rows(rows: number) {
    if (!this.dashboard.isMobileSize && !this.dashboard.options.autofillHeight) {
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

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

import { DashboardLayoutId, GridSettings, WidgetLayout, Dashboard } from '@app/shared/models/dashboard.models';
import { Widget } from '@app/shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';

export declare type DashboardPageScope = 'tenant' | 'customer';

export interface DashboardContext {
  state: string;
  dashboard: Dashboard;
  dashboardTimewindow: Timewindow;
  aliasController: IAliasController;
  stateController: IStateController;
}

export interface DashboardPageLayoutContext {
  id: DashboardLayoutId;
  widgets: Array<Widget>;
  widgetLayouts: {[id: string]: WidgetLayout};
  gridSettings: GridSettings;
  ignoreLoading: boolean;
}

export interface DashboardPageLayout {
  show: boolean;
  layoutCtx: DashboardPageLayoutContext;
}

export interface DashboardPageLayouts {
  main: DashboardPageLayout;
  right: DashboardPageLayout;
}

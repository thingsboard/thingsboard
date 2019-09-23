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

import { Component, OnDestroy, OnInit, Input, ChangeDetectorRef, ViewChild } from '@angular/core';
import { StateControllerComponent } from '@home/pages/dashboard/states/state-controller.component';
import { ILayoutController } from '@home/pages/dashboard/layout/layout.models';
import { DashboardContext, DashboardPageLayoutContext } from '@home/pages/dashboard/dashboard-page.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Widget } from '@shared/models/widget.models';
import { WidgetLayouts } from '@shared/models/dashboard.models';
import { GridsterComponent } from 'angular-gridster2';
import { IDashboardComponent } from '@home/models/dashboard-component.models';

@Component({
  selector: 'tb-dashboard-layout',
  templateUrl: './dashboard-layout.component.html',
  styleUrls: ['./dashboard-layout.component.scss']
})
export class DashboardLayoutComponent extends PageComponent implements ILayoutController, OnInit, OnDestroy {

  layoutCtxValue: DashboardPageLayoutContext;

  @Input()
  set layoutCtx(val: DashboardPageLayoutContext) {
    this.layoutCtxValue = val;
    if (this.layoutCtxValue) {
      this.layoutCtxValue.ctrl = this;
    }
  }
  get layoutCtx(): DashboardPageLayoutContext {
    return this.layoutCtxValue;
  }

  @Input()
  dashboardCtx: DashboardContext;

  @Input()
  isEdit: boolean;

  @Input()
  isMobile: boolean;

  @Input()
  widgetEditMode: boolean;

  @ViewChild('dashboard', {static: true}) dashboard: IDashboardComponent;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  reload() {
  }

  setResizing(layoutVisibilityChanged: boolean) {
  }

  resetHighlight() {
  }

}

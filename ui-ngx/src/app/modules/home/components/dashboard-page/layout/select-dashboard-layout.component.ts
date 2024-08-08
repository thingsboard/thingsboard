///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-select-dashboard-layout',
  templateUrl: './select-dashboard-layout.component.html',
  styleUrls: ['./select-dashboard-layout.component.scss']
})
export class SelectDashboardLayoutComponent implements OnInit, OnDestroy {

  @Input()
  dashboardCtrl: DashboardPageComponent;

  layout = {
    default: 'Default',
    xs: 'xs',
    sm: 'sm',
    md: 'md',
    lg: 'lg',
    xl: 'xl'
  };

  layouts = Object.keys(this.layout);

  selectLayout = 'default';

  private allowBreakpointsSize = new Set<string>();

  private stateChanged$: Subscription;

  constructor() { }

  ngOnInit() {
    this.stateChanged$ = this.dashboardCtrl.dashboardCtx.stateChanged.subscribe(() => {
      if (this.dashboardCtrl.layouts.main.layoutCtx.layoutData) {
        this.allowBreakpointsSize = new Set(Object.keys(this.dashboardCtrl.layouts.main.layoutCtx?.layoutData));
      } else {
        this.allowBreakpointsSize.add('default');
      }
      if (this.allowBreakpointsSize.has(this.dashboardCtrl.layouts.main.layoutCtx.breakpoint)) {
        this.selectLayout = this.dashboardCtrl.layouts.main.layoutCtx.breakpoint;
      } else {
        this.selectLayout = 'default';
        this.dashboardCtrl.layouts.main.layoutCtx.breakpoint = this.selectLayout;
      }
    });
  }

  ngOnDestroy() {
    this.stateChanged$.unsubscribe();
  }

  selectLayoutChanged() {
    if (!this.dashboardCtrl.layouts.main.layoutCtx.layoutData[this.selectLayout]) {
      this.dashboardCtrl.layouts.main.layoutCtx.ctrl.createBreakpointConfig(this.selectLayout);
    }
    this.dashboardCtrl.layouts.main.layoutCtx.ctrl.updatedCurrentBreakpoint(this.selectLayout);
    this.dashboardCtrl.updateLayoutSizes();
  }
}

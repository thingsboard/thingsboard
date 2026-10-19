// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { Subscription } from 'rxjs';
import { BreakpointId } from '@shared/models/dashboard.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

@Component({
    selector: 'tb-select-dashboard-breakpoint',
    templateUrl: './select-dashboard-breakpoint.component.html',
    styleUrls: ['./select-dashboard-breakpoint.component.scss'],
    standalone: false
})
export class SelectDashboardBreakpointComponent implements OnInit, OnDestroy {

  @Input()
  dashboardCtrl: DashboardPageComponent;

  selectedBreakpoint: BreakpointId = 'default';

  breakpointIds: Array<BreakpointId> = ['default'];

  private layoutDataChanged$: Subscription;

  constructor(private dashboardUtils: DashboardUtilsService) {
  }

  ngOnInit() {
    this.layoutDataChanged$ = this.dashboardCtrl.layouts.main.layoutCtx.layoutDataChanged.subscribe(() => {
      if (this.dashboardCtrl.layouts.main.layoutCtx.layoutData) {
        this.breakpointIds = Object.keys(this.dashboardCtrl.layouts.main.layoutCtx?.layoutData) as BreakpointId[];
        this.breakpointIds.sort((a, b) => {
          const aMaxWidth = this.dashboardUtils.getBreakpointInfoById(a)?.maxWidth || Infinity;
          const bMaxWidth = this.dashboardUtils.getBreakpointInfoById(b)?.maxWidth || Infinity;
          return bMaxWidth - aMaxWidth;
        });
        if (this.breakpointIds.indexOf(this.dashboardCtrl.layouts.main.layoutCtx.breakpoint) > -1) {
          this.selectedBreakpoint = this.dashboardCtrl.layouts.main.layoutCtx.breakpoint;
        } else {
          this.selectedBreakpoint = 'default';
          this.dashboardCtrl.layouts.main.layoutCtx.breakpoint = this.selectedBreakpoint;
        }
      }
    });
  }

  ngOnDestroy() {
    this.layoutDataChanged$.unsubscribe();
  }

  selectLayoutChanged() {
    this.dashboardUtils.updatedLayoutForBreakpoint(this.dashboardCtrl.layouts.main, this.selectedBreakpoint);
    this.dashboardCtrl.updateLayoutSizes();
  }

  getName(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointName(breakpointId);
  }

  getIcon(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointIcon(breakpointId);
  }

  getSizeDescription(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointSizeDescription(breakpointId);
  }
}

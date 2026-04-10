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

import { ChangeDetectorRef, Component, HostBinding, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Dashboard, DashboardLayoutId } from '@shared/models/dashboard.models';
import { IAliasController, StateObject } from '@core/api/widget-api.models';
import { updateEntityParams, WidgetContext } from '@home/models/widget-component.models';
import { deepClone, isDefinedAndNotNull, isNotEmptyStr, objToBase64 } from '@core/utils';
import { IDashboardComponent } from '@home/models/dashboard-component.models';
import { EntityId } from '@shared/models/id/entity-id';
import { Subscription } from 'rxjs';

@Component({
    selector: 'tb-dashboard-state',
    templateUrl: './dashboard-state.component.html',
    styleUrls: ['./dashboard-state.component.scss'],
    standalone: false
})
export class DashboardStateComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @Input()
  stateId: string;

  @Input()
  syncParentStateParams = false;

  @Input()
  defaultAutofillLayout = true;

  @Input()
  defaultMargin: number;

  @Input()
  defaultOuterMargin: boolean;

  @Input()
  defaultBackgroundColor;

  @Input()
  entityParamName: string;

  @Input()
  entityId: EntityId;

  currentState: string;

  dashboard: Dashboard;

  parentDashboard: IDashboardComponent;

  parentAliasController: IAliasController;

  stateExists = true;

  private stateSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.dashboard = deepClone(this.ctx.stateController.dashboardCtrl.dashboardCtx.getDashboard());
    const state = this.dashboard.configuration.states[this.stateId];
    if (state) {
      for (const layoutId of Object.keys(state.layouts)) {
        if (this.defaultAutofillLayout) {
          state.layouts[layoutId as DashboardLayoutId].gridSettings.autoFillHeight = true;
          state.layouts[layoutId as DashboardLayoutId].gridSettings.mobileAutoFillHeight = true;
        }
        if (isDefinedAndNotNull(this.defaultMargin)) {
          state.layouts[layoutId as DashboardLayoutId].gridSettings.margin = this.defaultMargin;
        }
        if (isDefinedAndNotNull(this.defaultOuterMargin)) {
          state.layouts[layoutId as DashboardLayoutId].gridSettings.outerMargin = this.defaultOuterMargin;
        }
        if (isNotEmptyStr(this.defaultBackgroundColor)) {
          state.layouts[layoutId as DashboardLayoutId].gridSettings.backgroundColor = this.defaultBackgroundColor;
        }
      }
      this.updateCurrentState();
      this.parentDashboard = this.ctx.parentDashboard ?
        this.ctx.parentDashboard : this.ctx.dashboard;
      if (this.syncParentStateParams) {
        this.parentAliasController = this.parentDashboard.aliasController;
        this.stateSubscription = this.ctx.stateController.dashboardCtrl.dashboardCtx.stateChanged.subscribe(() => {
          this.updateCurrentState();
          this.cd.markForCheck();
        });
      }
    } else {
      this.stateExists = false;
    }
  }

  ngOnDestroy(): void {
    if (this.stateSubscription) {
      this.stateSubscription.unsubscribe();
    }
  }

  private updateCurrentState(): void {
    const stateObject: StateObject = {};
    const params = deepClone(this.ctx.stateController.getStateParams());
    updateEntityParams(params, this.entityParamName, this.entityId);
    stateObject.params = params;
    if (this.stateId) {
      stateObject.id = this.stateId;
    }
    this.currentState = objToBase64([stateObject]);
  }
}

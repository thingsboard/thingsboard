///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { IStateControllerComponent, StateControllerState } from '@home/pages/dashboard/states/state-controller.models';
import { IDashboardController } from '../dashboard-page.models';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { Subscription } from 'rxjs';
import { NgZone, OnDestroy, OnInit, Directive } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { StatesControllerService } from '@home/pages/dashboard/states/states-controller.service';
import { EntityId } from '@app/shared/models/id/entity-id';
import { StateObject, StateParams } from '@app/core/api/widget-api.models';

@Directive()
export abstract class StateControllerComponent implements IStateControllerComponent, OnInit, OnDestroy {

  stateObject: StateControllerState = [];
  dashboardCtrl: IDashboardController;
  preservedState: any;

  isMobileValue: boolean;
  set isMobile(val: boolean) {
    if (this.isMobileValue !== val) {
      this.isMobileValue = val;
      if (this.inited) {
        this.onMobileChanged();
      }
    }
  }
  get isMobile(): boolean {
    return this.isMobileValue;
  }

  stateValue: string;
  set state(val: string) {
    if (this.stateValue !== val) {
      this.stateValue = val;
      if (this.inited) {
        this.onStateIdChanged();
      }
    }
  }
  get state(): string {
    return this.stateValue;
  }

  dashboardIdValue: string;
  set dashboardId(val: string) {
    if (this.dashboardIdValue !== val) {
      this.dashboardIdValue = val;
/*      if (this.inited) {
        this.currentState = this.route.snapshot.queryParamMap.get('state');
        this.init();
      }*/
    }
  }
  get dashboardId(): string {
    return this.dashboardIdValue;
  }

  statesValue: { [id: string]: DashboardState };
  set states(val: { [id: string]: DashboardState }) {
    if (this.statesValue !== val) {
      this.statesValue = val;
      if (this.inited) {
        this.onStatesChanged();
      }
    }
  }
  get states(): { [id: string]: DashboardState } {
    return this.statesValue;
  }

  currentState: string;

  private rxSubscriptions = new Array<Subscription>();

  private inited = false;

  protected constructor(protected router: Router,
                        protected route: ActivatedRoute,
                        protected ngZone: NgZone,
                        protected statesControllerService: StatesControllerService) {
  }

  ngOnInit(): void {
    this.rxSubscriptions.push(this.route.queryParamMap.subscribe((paramMap) => {
      const dashboardId = this.route.snapshot.params.dashboardId || '';
      if (this.dashboardId === dashboardId) {
        const newState = this.decodeStateParam(paramMap.get('state'));
        if (this.currentState !== newState) {
          this.currentState = newState;
          if (this.inited) {
            this.onStateChanged();
          }
        }
      }
    }));
    this.init();
    this.inited = true;
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  protected updateStateParam(newState: string) {
    this.currentState = newState;
    const queryParams: Params = { state: this.currentState };
    this.ngZone.run(() => {
      this.router.navigate(
        [],
        {
          relativeTo: this.route,
          queryParams,
          queryParamsHandling: 'merge',
        });
    });
  }

  public openRightLayout(): void {
    this.dashboardCtrl.openRightLayout();
  }

  public preserveState() {
    this.statesControllerService.preserveStateControllerState(this.stateControllerId(), this.stateObject);
  }

  public cleanupPreservedStates() {
    this.statesControllerService.cleanupPreservedStates();
  }

  public reInit() {
    this.preservedState = null;
    this.currentState = this.decodeStateParam(this.route.snapshot.queryParamMap.get('state'));
    this.init();
  }

  private decodeStateParam(stateURI: string): string{
    return stateURI !== null ? decodeURIComponent(stateURI) : null;
  }

  protected abstract init();

  protected abstract onMobileChanged();

  protected abstract onStateIdChanged();

  protected abstract onStatesChanged();

  protected abstract onStateChanged();

  protected abstract stateControllerId(): string;

  public abstract getEntityId(entityParamName: string): EntityId;

  public abstract getStateId(): string;

  public abstract getStateIdAtIndex(index: number): string;

  public abstract getStateIndex(): number;

  public abstract getStateParams(): StateParams;

  public abstract getStateParamsByStateId(stateId: string): StateParams;

  public abstract navigatePrevState(index: number): void;

  public abstract openState(id: string, params?: StateParams, openRightLayout?: boolean): void;

  public abstract pushAndOpenState(states: Array<StateObject>, openRightLayout?: boolean): void;

  public abstract resetState(): void;

  public abstract updateState(id?: string, params?: StateParams, openRightLayout?: boolean): void;

}

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

import {
  ComponentRef,
  Directive,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewContainerRef
} from '@angular/core';
import { DashboardState } from '@shared/models/dashboard.models';
import { IDashboardController } from '@home/components/dashboard-page/dashboard-page.models';
import { StatesControllerService } from '@home/components/dashboard-page/states/states-controller.service';
import { IStateControllerComponent } from '@home/components/dashboard-page/states/state-controller.models';
import { BehaviorSubject, Subject } from 'rxjs';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: 'tb-states-component'
})
export class StatesComponentDirective implements OnInit, OnDestroy, OnChanges {

  @Input()
  statesControllerId: string;

  @Input()
  dashboardCtrl: IDashboardController;

  @Input()
  dashboardId: string;

  @Input()
  states: {[id: string]: DashboardState };

  @Input()
  state: string;

  @Input()
  currentState: string;

  @Input()
  syncStateWithQueryParam: boolean;

  @Input()
  isMobile: boolean;

  stateControllerComponentRef: ComponentRef<IStateControllerComponent>;
  stateControllerComponent: IStateControllerComponent;

  private stateChangedSubject = new Subject<string>();
  private stateIdSubject: BehaviorSubject<string>;

  constructor(private viewContainerRef: ViewContainerRef,
              private statesControllerService: StatesControllerService) {
  }

  ngOnInit(): void {
    this.stateIdSubject = new BehaviorSubject<string>(this.dashboardCtrl.dashboardCtx.state);
    this.init();
  }

  ngOnDestroy(): void {
    this.destroy();
    this.stateChangedSubject.complete();
    this.stateIdSubject.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    let reInitController = false;
    let initController = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'statesControllerId') {
          this.reInit();
        } else if (propName === 'states') {
          this.stateControllerComponent.states = this.states;
        } else if (propName === 'dashboardId') {
          this.stateControllerComponent.dashboardId = this.dashboardId;
          reInitController = true;
        } else if (propName === 'isMobile') {
          this.stateControllerComponent.isMobile = this.isMobile;
        } else if (propName === 'state') {
          this.stateControllerComponent.state = this.state;
        } else if (propName === 'currentState') {
          this.stateControllerComponent.currentState = this.currentState;
          initController = true;
        } else if (propName === 'syncStateWithQueryParam') {
          this.stateControllerComponent.syncStateWithQueryParam = this.syncStateWithQueryParam;
        }
      }
    }
    if (initController) {
      this.stateControllerComponent.init();
    } else if (reInitController) {
      this.stateControllerComponent.reInit();
    }
  }

  private reInit() {
    this.destroy();
    this.init();
  }

  private init() {
    this.viewContainerRef.clear();
    let stateControllerData = this.statesControllerService.getStateController(this.statesControllerId);
    if (!stateControllerData) {
      stateControllerData = this.statesControllerService.getStateController('default');
    }
    const stateControllerInstanceId = this.dashboardCtrl.dashboardCtx.instanceId + '_' +  this.statesControllerId;
    const preservedState = this.statesControllerService.withdrawStateControllerState(stateControllerInstanceId);
    this.stateControllerComponentRef = this.viewContainerRef.createComponent(stateControllerData.component);
    this.stateControllerComponent = this.stateControllerComponentRef.instance;
    this.dashboardCtrl.dashboardCtx.stateController = this.stateControllerComponent;
    this.dashboardCtrl.dashboardCtx.stateChanged = this.stateChangedSubject.asObservable();
    this.dashboardCtrl.dashboardCtx.stateId = this.stateIdSubject.asObservable();
    this.stateControllerComponent.stateChanged().subscribe((state) => {
      this.stateChangedSubject.next(state);
    });
    this.stateControllerComponent.stateId().subscribe((stateId) => {
      this.stateIdSubject.next(stateId);
    });
    this.stateControllerComponent.preservedState = preservedState;
    this.stateControllerComponent.dashboardCtrl = this.dashboardCtrl;
    this.stateControllerComponent.stateControllerInstanceId = stateControllerInstanceId;
    this.stateControllerComponent.state = this.state;
    this.stateControllerComponent.currentState = this.currentState;
    this.stateControllerComponent.syncStateWithQueryParam = this.syncStateWithQueryParam;
    this.stateControllerComponent.isMobile = this.isMobile;
    this.stateControllerComponent.states = this.states;
    this.stateControllerComponent.dashboardId = this.dashboardId;
  }

  private destroy() {
    if (this.stateControllerComponentRef) {
      this.stateControllerComponentRef.destroy();
    }
  }
}

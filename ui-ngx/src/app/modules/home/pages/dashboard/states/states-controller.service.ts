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

import { ComponentFactory, ComponentFactoryResolver, Injectable, Type } from '@angular/core';
import { deepClone } from '@core/utils';
import { IStateControllerComponent } from '@home/pages/dashboard/states/state-controller.models';

export interface StateControllerData {
  factory: ComponentFactory<IStateControllerComponent>;
  state?: any;
}

@Injectable()
export class StatesControllerService {

  statesControllers: {[stateControllerId: string]: StateControllerData} = {};

  constructor(private componentFactoryResolver: ComponentFactoryResolver) {
  }

  public registerStatesController(stateControllerId: string, stateControllerComponent: Type<IStateControllerComponent>): void {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(stateControllerComponent);
    this.statesControllers[stateControllerId] = {
      factory: componentFactory
    };
  }

  public getStateControllers(): {[stateControllerId: string]: StateControllerData} {
    return this.statesControllers;
  }

  public getStateController(stateControllerId: string): StateControllerData {
    return this.statesControllers[stateControllerId];
  }

  public preserveStateControllerState(stateControllerId: string, state: any) {
    this.statesControllers[stateControllerId].state = deepClone(state);
  }

  public withdrawStateControllerState(stateControllerId: string): any {
    const state = this.statesControllers[stateControllerId].state;
    this.statesControllers[stateControllerId].state = null;
    return state;
  }

  public cleanupPreservedStates() {
    for (const stateControllerId of Object.keys(this.statesControllers)) {
      this.statesControllers[stateControllerId].state = null;
    }
  }
}

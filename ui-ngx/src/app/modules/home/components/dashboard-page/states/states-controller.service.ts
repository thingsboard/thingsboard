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

import { Injectable, Type } from '@angular/core';
import { deepClone } from '@core/utils';
import { IStateControllerComponent } from '@home/components/dashboard-page/states/state-controller.models';

export interface StateControllerData {
  component: Type<IStateControllerComponent>;
}

@Injectable()
export class StatesControllerService {

  statesControllers: {[stateControllerId: string]: StateControllerData} = {};

  statesControllerStates: {[stateControllerInstanceId: string]: any} = {};

  constructor() {
  }

  public registerStatesController(stateControllerId: string, stateControllerComponent: Type<IStateControllerComponent>): void {
    this.statesControllers[stateControllerId] = {
      component: stateControllerComponent
    };
  }

  public getStateControllers(): {[stateControllerId: string]: StateControllerData} {
    return this.statesControllers;
  }

  public getStateController(stateControllerId: string): StateControllerData {
    return this.statesControllers[stateControllerId];
  }

  public preserveStateControllerState(stateControllerInstanceId: string, state: any) {
    this.statesControllerStates[stateControllerInstanceId] = deepClone(state);
  }

  public withdrawStateControllerState(stateControllerInstanceId: string): any {
    const state = this.statesControllerStates[stateControllerInstanceId];
    delete this.statesControllerStates[stateControllerInstanceId];
    return state;
  }

  public cleanupPreservedStates() {
    this.statesControllerStates = {};
  }
}

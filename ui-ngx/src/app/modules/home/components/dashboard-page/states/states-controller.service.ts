// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

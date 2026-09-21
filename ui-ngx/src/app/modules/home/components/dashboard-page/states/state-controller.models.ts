// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { IStateController, StateObject } from '@core/api/widget-api.models';
import { IDashboardController } from '@home/components/dashboard-page/dashboard-page.models';
import { DashboardState } from '@shared/models/dashboard.models';

export declare type StateControllerState = StateObject[];

export interface IStateControllerComponent extends IStateController {
  stateControllerInstanceId: string;
  state: string;
  currentState: string;
  syncStateWithQueryParam: boolean;
  isMobile: boolean;
  states: {[id: string]: DashboardState };
  dashboardId: string;
  preservedState: any;
  reInit(): void;
  init(): void;
}

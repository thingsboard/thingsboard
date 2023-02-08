///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { StateObject, StateParams } from '@core/api/widget-api.models';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { DefaultEntitiesData, StateControllerState } from './state-controller.models';
import { StateControllerComponent } from './state-controller.component';
import { StatesControllerService } from '@home/components/dashboard-page/states/states-controller.service';
import { EntityId } from '@app/shared/models/id/entity-id';
import { UtilsService } from '@core/services/utils.service';
import { base64toObj, deepClone, insertVariable, isEmpty, objToBase64 } from '@app/core/utils';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { EntityService } from '@core/http/entity.service';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { map, tap } from 'rxjs/operators';
import { MobileService } from '@core/services/mobile.service';
import { AliasFilterType } from '@shared/models/alias.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { BaseData } from '@shared/models/base-data';
import { NULL_UUID } from '@shared/models/id/has-uuid';

@Component({
  selector: 'tb-entity-state-controller',
  templateUrl: './entity-state-controller.component.html',
  styleUrls: ['./entity-state-controller.component.scss']
})
export class EntityStateControllerComponent extends StateControllerComponent implements OnInit, OnDestroy {

  selectedStateIndex = -1;
  authUser = getCurrentAuthUser(this.store);
  defaultEntitiesFromAliases: DefaultEntitiesData = {};
  defaultEntitiesData: DefaultEntitiesData;

  constructor(protected router: Router,
              protected route: ActivatedRoute,
              protected ngZone: NgZone,
              protected statesControllerService: StatesControllerService,
              private store: Store<AppState>,
              private utils: UtilsService,
              private entityService: EntityService,
              private mobileService: MobileService,
              private dashboardUtils: DashboardUtilsService) {
    super(router, route, ngZone, statesControllerService);
  }

  ngOnInit(): void {
    super.ngOnInit();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
  }

  public init() {
    this.extractDefaultEntities().subscribe((entitiesDataMap) => {
      if (entitiesDataMap) {
        this.defaultEntitiesData = entitiesDataMap;
      }
      if (this.preservedState) {
        this.stateObject = this.preservedState;
        this.selectedStateIndex = this.stateObject.length - 1;
        setTimeout(() => {
          this.gotoState(this.stateObject[this.stateObject.length - 1].id, true);
        }, 1);
      } else {
        const initialState = this.currentState;
        this.stateObject = this.parseState(initialState);
        this.selectedStateIndex = this.stateObject.length - 1;
        setTimeout(() => {
          this.gotoState(this.stateObject[this.stateObject.length - 1].id, false);
        }, 1);
      }
    });
  }

  protected onMobileChanged() {
  }

  protected onStateIdChanged() {
  }

  protected onStatesChanged() {
  }

  protected onStateChanged() {
    this.stateObject = this.parseState(this.currentState);
    this.selectedStateIndex = this.stateObject.length - 1;
    this.gotoState(this.stateObject[this.stateObject.length - 1].id, false);
  }

  protected stateControllerId(): string {
    return 'entity';
  }

  public getStateParams(): StateParams {
    if (this.stateObject && this.stateObject.length) {
      return this.stateObject[this.stateObject.length - 1].params;
    } else {
      return {};
    }
  }

  public openState(id: string, params?: StateParams, openRightLayout?: boolean): void {
    if (this.states && this.states[id]) {
      this.resolveEntity(params).subscribe(
        () => {
          const newState: StateObject = {
            id,
            params
          };
          this.stateObject.push(newState);
          this.selectedStateIndex = this.stateObject.length - 1;
          this.gotoState(this.stateObject[this.stateObject.length - 1].id, true, openRightLayout);
        }
      );
    }
  }

  public pushAndOpenState(states: Array<StateObject>, openRightLayout?: boolean): void {
    if (this.states) {
      for (const state of states) {
        if (!this.states[state.id]) {
          return;
        }
      }
      forkJoin(states.map(state => this.resolveEntity(state.params))).subscribe(
        () => {
          this.stateObject.push(...states);
          this.selectedStateIndex = this.stateObject.length - 1;
          this.gotoState(this.stateObject[this.stateObject.length - 1].id, true, openRightLayout);
        }
      );
    }
  }

  public updateState(id: string, params?: StateParams, openRightLayout?: boolean): void {
    if (!id) {
      id = this.getStateId();
    }
    if (this.states && this.states[id]) {
      this.resolveEntity(params).subscribe(
        () => {
          this.stateObject[this.stateObject.length - 1] = {
            id,
            params
          };
          this.gotoState(this.stateObject[this.stateObject.length - 1].id, true, openRightLayout);
        }
      );
    }
  }

  public getEntityId(entityParamName: string): EntityId {
    const stateParams = this.getStateParams();
    if (!entityParamName || !entityParamName.length) {
      return stateParams.entityId;
    } else if (stateParams[entityParamName]) {
      return stateParams[entityParamName].entityId;
    }
    return null;
  }

  public getStateId(): string {
    if (this.stateObject && this.stateObject.length) {
      return this.stateObject[this.stateObject.length - 1].id;
    } else {
      return '';
    }
  }

  public getStateIdAtIndex(index: number): string {
    if (this.stateObject && this.stateObject[index]) {
      return this.stateObject[index].id;
    } else {
      return '';
    }
  }

  public getStateIndex(): number {
    if (this.stateObject && this.stateObject.length) {
      return this.stateObject.length - 1;
    } else {
      return -1;
    }
  }

  public getStateParamsByStateId(stateId: string): StateParams {
    const stateObj = this.getStateObjById(stateId);
    if (stateObj) {
      return stateObj.params;
    } else {
      return null;
    }
  }

  public navigatePrevState(index: number): void {
    if (index < this.stateObject.length - 1) {
      this.stateObject.splice(index + 1, this.stateObject.length - index - 1);
      this.selectedStateIndex = this.stateObject.length - 1;
      this.gotoState(this.stateObject[this.stateObject.length - 1].id, true);
    }
  }

  public resetState(): void {
    const rootStateId = this.dashboardUtils.getRootStateId(this.states);
    this.stateObject = [ { id: rootStateId, params: {} } ];
    this.gotoState(rootStateId, true);
  }

  public getStateName(index: number): string {
    let result = '';
    const state = this.stateObject[index];
    if (state) {
      const dashboardState = this.states[state.id];
      if (dashboardState) {
        let stateName = dashboardState.name;
        stateName = this.utils.customTranslation(stateName, stateName);
        const params = this.stateObject[index].params;
        const defaultParams = this.defaultEntitiesFromAliases["default"];
        let entityName: string = '';
        let entityLabel: string = '';

        if (params && Object.keys(params).length) {
          entityName = params.entityName ? params.entityName : '';
          entityLabel = params.entityLabel ? params.entityLabel : '';
        } else if (defaultParams) {
          const data = this.defaultEntitiesData[defaultParams.id];
          entityName = data ? data.name : '';
          entityLabel = data ? data.label : '';
        }

        result = insertVariable(stateName, 'entityName', entityName);
        result = insertVariable(result, 'entityLabel', entityLabel);

        if (params && Object.keys(params).length) {
          for (const prop of Object.keys(params)) {
            if (params[prop] && params[prop].entityName) {
              result = insertVariable(result, prop + ':entityName', params[prop].entityName);
            }
            if (params[prop] && params[prop].entityLabel) {
              result = insertVariable(result, prop + ':entityLabel', params[prop].entityLabel);
            }
          }
        } else if (this.defaultEntitiesFromAliases && Object.keys(this.defaultEntitiesFromAliases))  {
          for (const prop of Object.keys(this.defaultEntitiesFromAliases)) {
            if (this.defaultEntitiesFromAliases[prop] &&
                this.defaultEntitiesFromAliases[prop].id &&
                this.defaultEntitiesData[this.defaultEntitiesFromAliases[prop].id]
            ) {
              const entityData: BaseData<EntityId> = this.defaultEntitiesData[this.defaultEntitiesFromAliases[prop].id];
              if (entityData.name) {
                result = insertVariable(result, prop + ':entityName', entityData.name);
              }
              if (entityData.label) {
                result = insertVariable(result, prop + ':entityLabel', entityData.label);
              }
            }
          }
        }
      }
    }
    return result;
  }

  public getCurrentStateName(): string {
    return this.getStateName(this.stateObject.length - 1);
  }

  public selectedStateIndexChanged() {
    this.navigatePrevState(this.selectedStateIndex);
  }

  private parseState(stateBase64: string): StateControllerState {
    let result: StateControllerState;
    if (stateBase64) {
      try {
        result = base64toObj(stateBase64);
      } catch (e) {
        result = [ { id: null, params: {} } ];
      }
    }
    if (!result) {
      result = [];
    }
    if (!result.length) {
      result[0] = { id: null, params: {} };
    }
    const rootStateId = this.dashboardUtils.getRootStateId(this.states);
    if (!result[0].id) {
      result[0].id = rootStateId;
    }
    if (!this.states[result[0].id]) {
      result[0].id = rootStateId;
    }
    let i = result.length;
    while (i--) {
      if (!result[i].id || !this.states[result[i].id]) {
        result.splice(i, 1);
      }
    }
    return result;
  }

  private gotoState(stateId: string, update: boolean, openRightLayout?: boolean) {
    const isStateIdChanged = this.dashboardCtrl.dashboardCtx.state !== stateId;
    this.dashboardCtrl.openDashboardState(stateId, openRightLayout);
    if (this.syncStateWithQueryParam) {
      this.mobileService.handleDashboardStateName(this.getStateName(this.stateObject.length - 1));
    }
    if (update) {
      this.updateLocation(isStateIdChanged);
    }
  }

  private updateLocation(isStateIdChanged: boolean) {
    if (this.stateObject[this.stateObject.length - 1].id) {
      let newState;
      if (this.isDefaultState()) {
        newState = null;
      } else {
        newState = objToBase64(this.stateObject);
      }
      this.updateStateParam(newState, !isStateIdChanged);
    }
  }

  private isDefaultState(): boolean {
    if (this.stateObject.length === 1) {
      const state = this.stateObject[0];
      const rootStateId = this.dashboardUtils.getRootStateId(this.states);
      if (state.id === rootStateId && (!state.params || isEmpty(state.params))) {
        return true;
      }
    }
    return false;
  }

  private resolveEntity(params: StateParams): Observable<void> {
    if (params && params.targetEntityParamName) {
      params = params[params.targetEntityParamName];
    }
    if (params && params.entityId && params.entityId.id && params.entityId.entityType) {
      if (this.isEntityResolved(params)) {
        return of(null);
      } else {
        return this.entityService.getEntity(params.entityId.entityType as EntityType,
          params.entityId.id, {ignoreLoading: true, ignoreErrors: true}).pipe(
            tap((entity) => {
              params.entityName = entity.name;
              params.entityLabel = entity.label;
            }),
          map(() => null)
        );
      }
    } else {
      return of(null);
    }
  }

  private isEntityResolved(params: StateParams): boolean {
    return !(!params.entityName || !params.entityName.length);
  }

  private getStateObjById(id: string): StateObject {
    return this.stateObject.find((stateObj) => stateObj.id === id);
  }

  private extractDefaultEntities(): Observable<DefaultEntitiesData | null> {
    const entityAliases = this.dashboardCtrl.dashboardCtx.aliasController.getEntityAliases();
    const entitiesObs: Array<Observable<BaseData<EntityId>>> = [];

    Object.values(entityAliases).forEach((alias) => {
      if (alias.filter.type === AliasFilterType.stateEntity && alias.filter.defaultStateEntity) {
        const entityId = this.extractEntityId(deepClone(alias.filter.defaultStateEntity));
        const paramKey = alias.filter.stateEntityParamName ? alias.filter.stateEntityParamName : "default";
        if (!this.defaultEntitiesFromAliases[paramKey] && entityId.id !== NULL_UUID) {
          this.defaultEntitiesFromAliases[paramKey] = entityId;
          entitiesObs.push(this.entityService.getEntity(entityId.entityType as EntityType,
            entityId.id, {ignoreLoading: true, ignoreErrors: true}));
        }
      }
    });

    if (entitiesObs.length) {
      try {
        return forkJoin(entitiesObs).pipe(
          map((entitiesData) => {
            const entitiesDataMap: DefaultEntitiesData = {};
            (entitiesData as Array<BaseData<EntityId>>).forEach((entity) => {
              if (entity) {
                entitiesDataMap[entity.id.id] = entity;
              }
            })
            return entitiesDataMap;
          })
        );
      } catch {
        return of(null);
      }
    } else {
      return of(null);
    }
  }

  private extractEntityId(aliasDefaultEntity: EntityId): EntityId {
    const entityType = aliasDefaultEntity.entityType;
    let id: string;
    switch (entityType) {
      case AliasEntityType.CURRENT_CUSTOMER:
        id = aliasDefaultEntity.id && this.authUser.customerId === NULL_UUID ? aliasDefaultEntity.id :
          this.authUser.customerId;
        aliasDefaultEntity.entityType = EntityType.CUSTOMER;
        break;
      case AliasEntityType.CURRENT_TENANT:
        id = this.authUser.tenantId;
        aliasDefaultEntity.entityType = EntityType.TENANT;
        break;
      case AliasEntityType.CURRENT_USER:
        id = this.authUser.userId;
        aliasDefaultEntity.entityType = EntityType.USER;
        break;
      case AliasEntityType.CURRENT_USER_OWNER:
        id = this.authUser.authority === Authority.CUSTOMER_USER ? this.authUser.customerId : this.authUser.tenantId;
        aliasDefaultEntity.entityType = this.authUser.authority === Authority.CUSTOMER_USER ? EntityType.CUSTOMER :
          EntityType.TENANT;
    }

    if (id) {
      aliasDefaultEntity.id = id;
    }

    return aliasDefaultEntity;
  }
}

///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { AliasInfo, IAliasController, StateControllerHolder, StateEntityInfo } from '@core/api/widget-api.models';
import { forkJoin, Observable, of, ReplaySubject, Subject } from 'rxjs';
import {
  Datasource,
  DatasourceType,
  datasourceTypeTranslationMap,
  TargetDevice,
  TargetDeviceType,
  targetDeviceValid
} from '@app/shared/models/widget.models';
import { deepClone, isDefinedAndNotNull, isEqual } from '@core/utils';
import { EntityService } from '@core/http/entity.service';
import { UtilsService } from '@core/services/utils.service';
import { AliasFilterType, EntityAliases, SingleEntityFilter } from '@shared/models/alias.models';
import { EntityInfo } from '@shared/models/entity.models';
import { map, mergeMap } from 'rxjs/operators';
import {
  createDefaultEntityDataPageLink,
  Filter,
  FilterInfo,
  filterInfoToKeyFilters,
  Filters,
  KeyFilter,
  singleEntityDataPageLink,
  singleEntityFilterFromDeviceId,
  updateDatasourceFromEntityInfo
} from '@shared/models/query/query.models';
import { TranslateService } from '@ngx-translate/core';

export class AliasController implements IAliasController {

  entityAliasesChangedSubject = new Subject<Array<string>>();
  entityAliasesChanged: Observable<Array<string>> = this.entityAliasesChangedSubject.asObservable();

  filtersChangedSubject = new Subject<Array<string>>();
  filtersChanged: Observable<Array<string>> = this.filtersChangedSubject.asObservable();

  private entityAliasResolvedSubject = new Subject<string>();
  entityAliasResolved: Observable<string> = this.entityAliasResolvedSubject.asObservable();

  entityAliases: EntityAliases;
  filters: Filters;
  userFilters: Filters;

  resolvedAliases: { [aliasId: string]: AliasInfo } = {};
  resolvedAliasesObservable: { [aliasId: string]: Observable<AliasInfo> } = {};

  resolvedDevices: { [deviceId: string]: EntityInfo } = {};
  resolvedDevicesObservable: { [deviceId: string]: Observable<EntityInfo> } = {};

  resolvedAliasesToStateEntities: { [aliasId: string]: StateEntityInfo } = {};

  constructor(private utils: UtilsService,
              private entityService: EntityService,
              private translate: TranslateService,
              private stateControllerHolder: StateControllerHolder,
              private origEntityAliases: EntityAliases,
              private origFilters: Filters,
              private origUserFilters?: Filters) {
    this.entityAliases = deepClone(this.origEntityAliases) || {};
    this.filters = deepClone(this.origFilters) || {};
    this.userFilters = deepClone(this.origUserFilters) || {};
  }

  updateEntityAliases(newEntityAliases: EntityAliases) {
    const changedAliasIds: Array<string> = [];
    for (const aliasId of Object.keys(newEntityAliases)) {
      const newEntityAlias = newEntityAliases[aliasId];
      const prevEntityAlias = this.entityAliases[aliasId];
      if (!isEqual(newEntityAlias, prevEntityAlias)) {
        changedAliasIds.push(aliasId);
        this.setAliasUnresolved(aliasId);
      }
    }
    for (const aliasId of Object.keys(this.entityAliases)) {
      if (!newEntityAliases[aliasId]) {
        changedAliasIds.push(aliasId);
        this.setAliasUnresolved(aliasId);
      }
    }
    this.entityAliases = deepClone(newEntityAliases);
    if (changedAliasIds.length) {
      this.entityAliasesChangedSubject.next(changedAliasIds);
    }
  }

  updateFilters(newFilters: Filters) {
    const changedFilterIds: Array<string> = [];
    for (const filterId of Object.keys(newFilters)) {
      const newFilter = newFilters[filterId];
      const prevFilter = this.filters[filterId];
      if (!isEqual(newFilter, prevFilter)) {
        changedFilterIds.push(filterId);
      }
    }
    for (const filterId of Object.keys(this.filters)) {
      if (!newFilters[filterId]) {
        changedFilterIds.push(filterId);
      }
    }
    this.filters = deepClone(newFilters);
    if (changedFilterIds.length) {
      for (const filterId of changedFilterIds) {
        delete this.userFilters[filterId];
      }
      this.filtersChangedSubject.next(changedFilterIds);
    }
  }

  updateAliases(aliasIds?: Array<string>) {
    if (!aliasIds) {
      aliasIds = [];
      for (const aliasId of Object.keys(this.resolvedAliases)) {
        aliasIds.push(aliasId);
      }
    }
    const tasks: Observable<AliasInfo>[] = [];
    for (const aliasId of aliasIds) {
      this.setAliasUnresolved(aliasId);
      tasks.push(this.getAliasInfo(aliasId));
    }
    forkJoin(tasks).subscribe(() => {
      this.entityAliasesChangedSubject.next(aliasIds);
    });
  }

  dashboardStateChanged() {
    const changedAliasIds: Array<string> = [];
    for (const aliasId of Object.keys(this.resolvedAliasesToStateEntities)) {
      const stateEntityInfo = this.resolvedAliasesToStateEntities[aliasId];
      const newEntityId = this.stateControllerHolder().getEntityId(stateEntityInfo.entityParamName);
      const prevEntityId = stateEntityInfo.entityId;
      if (!isEqual(newEntityId, prevEntityId)) {
        changedAliasIds.push(aliasId);
        this.setAliasUnresolved(aliasId);
      }
    }
    if (changedAliasIds.length) {
      this.entityAliasesChangedSubject.next(changedAliasIds);
    }
  }

  setAliasUnresolved(aliasId: string) {
    delete this.resolvedAliases[aliasId];
    delete this.resolvedAliasesObservable[aliasId];
    delete this.resolvedAliasesToStateEntities[aliasId];
  }

  getEntityAliases(): EntityAliases {
    return this.entityAliases;
  }

  getFilters(): Filters {
    return this.filters;
  }

  getUserFilters(): Filters {
    return this.userFilters;
  }

  getFilterInfo(filterId: string): FilterInfo {
    if (this.userFilters[filterId]) {
      return this.userFilters[filterId];
    } else {
      return this.filters[filterId];
    }
  }

  getKeyFilters(filterId: string): Array<KeyFilter> {
    const filter = this.getFilterInfo(filterId);
    if (filter) {
      return filterInfoToKeyFilters(filter);
    } else {
      return [];
    }
  }

  getEntityAliasId(aliasName: string): string {
    for (const aliasId of Object.keys(this.entityAliases)) {
      const alias = this.entityAliases[aliasId];
      if (alias.alias === aliasName) {
        return aliasId;
      }
    }
    return null;
  }

  getAliasInfo(aliasId: string): Observable<AliasInfo> {
    let aliasInfo = this.resolvedAliases[aliasId];
    if (aliasInfo) {
      return of(aliasInfo);
    } else if (this.resolvedAliasesObservable[aliasId]) {
      return this.resolvedAliasesObservable[aliasId];
    } else {
      const resolvedAliasSubject = new ReplaySubject<AliasInfo>();
      this.resolvedAliasesObservable[aliasId] = resolvedAliasSubject.asObservable();
      const entityAlias = this.entityAliases[aliasId];
      if (entityAlias) {
        this.entityService.resolveAlias(entityAlias, this.stateControllerHolder().getStateParams()).subscribe(
          (resolvedAliasInfo) => {
            this.resolvedAliases[aliasId] = resolvedAliasInfo;
            delete this.resolvedAliasesObservable[aliasId];
            if (resolvedAliasInfo.stateEntity) {
              this.resolvedAliasesToStateEntities[aliasId] = {
                entityParamName: resolvedAliasInfo.entityParamName,
                entityId: this.stateControllerHolder().getEntityId(resolvedAliasInfo.entityParamName)
              };
            }
            this.entityAliasResolvedSubject.next(aliasId);
            resolvedAliasSubject.next(resolvedAliasInfo);
            resolvedAliasSubject.complete();
          },
          () => {
            resolvedAliasSubject.error(null);
            delete this.resolvedAliasesObservable[aliasId];
          }
        );
      } else {
        resolvedAliasSubject.error(null);
        const res = this.resolvedAliasesObservable[aliasId];
        delete this.resolvedAliasesObservable[aliasId];
        return res;
      }
      aliasInfo = this.resolvedAliases[aliasId];
      if (aliasInfo) {
        return of(aliasInfo);
      } else {
        return this.resolvedAliasesObservable[aliasId];
      }
    }
  }

  resolveSingleEntityInfo(aliasId: string): Observable<EntityInfo> {
    return this.getAliasInfo(aliasId).pipe(
      mergeMap((aliasInfo) => {
        if (aliasInfo.resolveMultiple) {
          if (aliasInfo.entityFilter) {
            return this.entityService.findSingleEntityInfoByEntityFilter(aliasInfo.entityFilter,
              {ignoreLoading: true, ignoreErrors: true});
          } else {
            return of(null);
          }
        } else {
          return of(aliasInfo.currentEntity);
        }
      })
    );
  }

  resolveSingleEntityInfoForDeviceId(deviceId: string): Observable<EntityInfo> {
    let entityInfo = this.resolvedDevices[deviceId];
    if (entityInfo) {
      return of(entityInfo);
    } else if (this.resolvedDevicesObservable[deviceId]) {
      return this.resolvedDevicesObservable[deviceId];
    } else {
      const resolvedDeviceSubject = new ReplaySubject<EntityInfo>();
      this.resolvedDevicesObservable[deviceId] = resolvedDeviceSubject.asObservable();
      const entityFilter = singleEntityFilterFromDeviceId(deviceId);
      this.entityService.findSingleEntityInfoByEntityFilter(entityFilter,
        {ignoreLoading: true, ignoreErrors: true}).subscribe(
        (resolvedEntityInfo) => {
          this.resolvedDevices[deviceId] = resolvedEntityInfo;
          delete this.resolvedDevicesObservable[deviceId];
          resolvedDeviceSubject.next(resolvedEntityInfo);
          resolvedDeviceSubject.complete();
        },
        () => {
          resolvedDeviceSubject.error(null);
          delete this.resolvedDevicesObservable[deviceId];
        }
      );
      entityInfo = this.resolvedDevices[deviceId];
      if (entityInfo) {
        return of(entityInfo);
      } else {
        return this.resolvedDevicesObservable[deviceId];
      }
    }
  }

  resolveSingleEntityInfoForTargetDevice(targetDevice: TargetDevice): Observable<EntityInfo> {
    if (targetDeviceValid(targetDevice)) {
      if (targetDevice.type === TargetDeviceType.entity) {
        return this.resolveSingleEntityInfo(targetDevice.entityAliasId);
      } else {
        return this.resolveSingleEntityInfoForDeviceId(targetDevice.deviceId);
      }
    } else {
      return of(null);
    }
  }

  private resolveDatasource(datasource: Datasource, forceFilter = false): Observable<Datasource> {
    const newDatasource = deepClone(datasource);
    if (newDatasource.type === DatasourceType.entity
      || newDatasource.type === DatasourceType.device
      || newDatasource.type === DatasourceType.entityCount
      || newDatasource.type === DatasourceType.alarmCount) {
      if (newDatasource.filterId) {
        newDatasource.keyFilters = this.getKeyFilters(newDatasource.filterId);
      }
      if (newDatasource.type === DatasourceType.alarmCount) {
        newDatasource.alarmFilter = this.entityService.resolveAlarmFilter(newDatasource.alarmFilterConfig, false);
      }
      if (newDatasource.type === DatasourceType.device) {
        newDatasource.type = DatasourceType.entity;
        newDatasource.entityFilter = singleEntityFilterFromDeviceId(newDatasource.deviceId);
        if (forceFilter) {
          return this.entityService.findSingleEntityInfoByEntityFilter(newDatasource.entityFilter,
            {ignoreLoading: true, ignoreErrors: true}).pipe(
            map((entity) => {
              if (entity) {
                updateDatasourceFromEntityInfo(newDatasource, entity, true);
              }
              return newDatasource;
            })
          );
        } else {
          return of(newDatasource);
        }
      } else if (newDatasource.entityAliasId) {
        return this.getAliasInfo(newDatasource.entityAliasId).pipe(
          mergeMap((aliasInfo) => {
            newDatasource.aliasName = aliasInfo.alias;
            if (!aliasInfo.entityFilter) {
              newDatasource.unresolvedStateEntity = true;
              newDatasource.name = 'Unresolved';
              newDatasource.entityName = 'Unresolved';
              return of(newDatasource);
            }
            if (aliasInfo.resolveMultiple) {
              newDatasource.entityFilter = aliasInfo.entityFilter;
              if (forceFilter) {
                return this.entityService.findSingleEntityInfoByEntityFilter(aliasInfo.entityFilter,
                  {ignoreLoading: true, ignoreErrors: true}).pipe(
                  map((entity) => {
                    if (entity) {
                      updateDatasourceFromEntityInfo(newDatasource, entity, true);
                    }
                    return newDatasource;
                  })
                );
              } else {
                return of(newDatasource);
              }
            } else {
              if (aliasInfo.currentEntity) {
                updateDatasourceFromEntityInfo(newDatasource, aliasInfo.currentEntity, true);
              } else if (aliasInfo.stateEntity) {
                newDatasource.unresolvedStateEntity = true;
                newDatasource.name = 'Unresolved';
                newDatasource.entityName = 'Unresolved';
              }
              return of(newDatasource);
            }
          })
        );
      } else if (newDatasource.entityId && !newDatasource.entityFilter) {
        newDatasource.entityFilter = {
          singleEntity: {
            id: newDatasource.entityId,
            entityType: newDatasource.entityType,
          },
          type: AliasFilterType.singleEntity
        } as SingleEntityFilter;
        return of(newDatasource);
      } else {
        newDatasource.aliasName = newDatasource.entityName;
        newDatasource.name = newDatasource.entityName;
        return of(newDatasource);
      }
    } else {
      return of(newDatasource);
    }
  }

  resolveAlarmSource(alarmSource: Datasource): Observable<Datasource> {
    return this.resolveDatasource(alarmSource).pipe(
      map((datasource) => {
        if (datasource.type === DatasourceType.function) {
          let name: string;
          if (datasource.name && datasource.name.length) {
            name = datasource.name;
          } else {
            name = DatasourceType.function;
          }
          datasource.name = name;
          datasource.aliasName = name;
          datasource.entityName = name;
        }
        return datasource;
      })
    );
  }

  resolveDatasources(datasources: Array<Datasource>, singleEntity?: boolean, pageSize = 1024): Observable<Array<Datasource>> {
    if (!datasources || !datasources.length) {
      return of([]);
    }
    const toResolve = singleEntity ? [datasources[0]] : datasources;
    const observables = new Array<Observable<Datasource>>();
    toResolve.forEach((datasource) => {
      observables.push(this.resolveDatasource(datasource));
    });
    return forkJoin(observables).pipe(
      map((result) => {
        let functionIndex = 0;
        let entityCountIndex = 0;
        let alarmCountIndex = 0;
        result.forEach((datasource) => {
          if (datasource.type === DatasourceType.function || datasource.type === DatasourceType.entityCount ||
            datasource.type === DatasourceType.alarmCount) {
            let name: string;
            if (datasource.name && datasource.name.length) {
              name = datasource.name;
            } else {
              name = this.translate.instant(datasourceTypeTranslationMap.get(datasource.type));
              if (datasource.type === DatasourceType.function) {
                functionIndex++;
                if (functionIndex > 1) {
                  name += ' ' + functionIndex;
                }
              } else if (datasource.type === DatasourceType.entityCount) {
                entityCountIndex++;
                if (entityCountIndex > 1) {
                  name += ' ' + entityCountIndex;
                }
              } else {
                alarmCountIndex++;
                if (alarmCountIndex > 1) {
                  name += ' ' + alarmCountIndex;
                }
              }
            }
            datasource.name = name;
            datasource.aliasName = name;
            datasource.entityName = name;
          } else {
            if (singleEntity) {
              datasource.pageLink = deepClone(singleEntityDataPageLink);
            } else if (!datasource.pageLink) {
              pageSize = isDefinedAndNotNull(pageSize) && pageSize > 0 ? pageSize : 1024;
              datasource.pageLink = createDefaultEntityDataPageLink(pageSize);
            }
          }
        });
        return result;
      })
    );
  }

  getInstantAliasInfo(aliasId: string): AliasInfo {
    return this.resolvedAliases[aliasId];
  }

  updateCurrentAliasEntity(aliasId: string, currentEntity: EntityInfo) {
    const aliasInfo = this.resolvedAliases[aliasId];
    if (aliasInfo) {
      const prevCurrentEntity = aliasInfo.currentEntity;
      if (!isEqual(currentEntity, prevCurrentEntity)) {
        aliasInfo.currentEntity = currentEntity;
        this.entityAliasesChangedSubject.next([aliasId]);
      }
    }
  }

  updateUserFilter(filter: Filter) {
    let prevUserFilter = this.userFilters[filter.id];
    if (!prevUserFilter) {
      prevUserFilter = this.filters[filter.id];
    }
    if (prevUserFilter && !isEqual(prevUserFilter, filter)) {
      this.userFilters[filter.id] = filter;
      this.filtersChangedSubject.next([filter.id]);
    }
  }
}

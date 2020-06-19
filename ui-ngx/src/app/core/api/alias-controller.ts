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

import { AliasInfo, IAliasController, StateControllerHolder, StateEntityInfo } from '@core/api/widget-api.models';
import { forkJoin, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { DataKey, Datasource, DatasourceType } from '@app/shared/models/widget.models';
import { deepClone, isEqual, createLabelFromDatasource } from '@core/utils';
import { EntityService } from '@core/http/entity.service';
import { UtilsService } from '@core/services/utils.service';
import { EntityAliases } from '@shared/models/alias.models';
import { EntityInfo } from '@shared/models/entity.models';
import { map } from 'rxjs/operators';

export class AliasController implements IAliasController {

  entityAliasesChangedSubject = new Subject<Array<string>>();
  entityAliasesChanged: Observable<Array<string>> = this.entityAliasesChangedSubject.asObservable();

  private entityAliasResolvedSubject = new Subject<string>();
  entityAliasResolved: Observable<string> = this.entityAliasResolvedSubject.asObservable();

  entityAliases: EntityAliases;

  resolvedAliases: {[aliasId: string]: AliasInfo} = {};
  resolvedAliasesObservable: {[aliasId: string]: Observable<AliasInfo>} = {};

  resolvedAliasesToStateEntities: {[aliasId: string]: StateEntityInfo} = {};

  constructor(private utils: UtilsService,
              private entityService: EntityService,
              private stateControllerHolder: StateControllerHolder,
              private origEntityAliases: EntityAliases) {
    this.entityAliases = deepClone(this.origEntityAliases);
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
              const stateEntityInfo: StateEntityInfo = {
                entityParamName: resolvedAliasInfo.entityParamName,
                entityId: this.stateControllerHolder().getEntityId(resolvedAliasInfo.entityParamName)
              };
              this.resolvedAliasesToStateEntities[aliasId] = stateEntityInfo;
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

  private resolveDatasource(datasource: Datasource, isSingle?: boolean): Observable<Array<Datasource>> {
    if (datasource.type === DatasourceType.entity) {
      if (datasource.entityAliasId) {
        return this.getAliasInfo(datasource.entityAliasId).pipe(
          map((aliasInfo) => {
            datasource.aliasName = aliasInfo.alias;
            if (aliasInfo.resolveMultiple && !isSingle) {
              let newDatasource: Datasource;
              const resolvedEntities = aliasInfo.resolvedEntities;
              if (resolvedEntities && resolvedEntities.length) {
                const datasources: Array<Datasource> = [];
                for (let i = 0; i < resolvedEntities.length; i++) {
                  const resolvedEntity = resolvedEntities[i];
                  newDatasource = deepClone(datasource);
                  if (resolvedEntity.origEntity) {
                    newDatasource.entity = deepClone(resolvedEntity.origEntity);
                  } else {
                    newDatasource.entity = {};
                  }
                  newDatasource.entityId = resolvedEntity.id;
                  newDatasource.entityType = resolvedEntity.entityType;
                  newDatasource.entityName = resolvedEntity.name;
                  newDatasource.entityLabel = resolvedEntity.label;
                  newDatasource.entityDescription = resolvedEntity.entityDescription;
                  newDatasource.name = resolvedEntity.name;
                  newDatasource.generated = i > 0 ? true : false;
                  datasources.push(newDatasource);
                }
                return datasources;
              } else {
                if (aliasInfo.stateEntity) {
                  newDatasource = deepClone(datasource);
                  newDatasource.unresolvedStateEntity = true;
                  return [newDatasource];
                } else {
                  return [];
                  // throw new Error('Unable to resolve datasource.');
                }
              }
            } else {
              const entity = aliasInfo.currentEntity;
              if (entity) {
                if (entity.origEntity) {
                  datasource.entity = deepClone(entity.origEntity);
                } else {
                  datasource.entity = {};
                }
                datasource.entityId = entity.id;
                datasource.entityType = entity.entityType;
                datasource.entityName = entity.name;
                datasource.entityLabel = entity.label;
                datasource.name = entity.name;
                datasource.entityDescription = entity.entityDescription;
                return [datasource];
              } else {
                if (aliasInfo.stateEntity) {
                  datasource.unresolvedStateEntity = true;
                  return [datasource];
                } else {
                  return [];
                  // throw new Error('Unable to resolve datasource.');
                }
              }
            }
          })
        );
      } else {
        datasource.aliasName = datasource.entityName;
        datasource.name = datasource.entityName;
        return of([datasource]);
      }
    } else {
      return of([datasource]);
    }
  }

  resolveAlarmSource(alarmSource: Datasource): Observable<Datasource> {
    return this.resolveDatasource(alarmSource, true).pipe(
      map((datasources) => {
        const datasource = datasources && datasources.length ? datasources[0] : deepClone(alarmSource);
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
        } else if (datasource.unresolvedStateEntity) {
          datasource.name = 'Unresolved';
          datasource.entityName = 'Unresolved';
        }
        return datasource;
      })
    );
  }

  resolveDatasources(datasources: Array<Datasource>): Observable<Array<Datasource>> {
    const newDatasources = deepClone(datasources);
    const observables = new Array<Observable<Array<Datasource>>>();
    newDatasources.forEach((datasource) => {
      observables.push(this.resolveDatasource(datasource));
    });
    return forkJoin(observables).pipe(
      map((arrayOfDatasources) => {
        const result = new Array<Datasource>();
        arrayOfDatasources.forEach((datasourcesArray) => {
          result.push(...datasourcesArray);
        });
        result.sort((d1, d2) => {
          const i1 = d1.generated ? 1 : 0;
          const i2 = d2.generated ? 1 : 0;
          return i1 - i2;
        });
        let index = 0;
        let functionIndex = 0;
        result.forEach((datasource) => {
          if (datasource.type === DatasourceType.function) {
            let name: string;
            if (datasource.name && datasource.name.length) {
              name = datasource.name;
            } else {
              functionIndex++;
              name = DatasourceType.function;
              if (functionIndex > 1) {
                name += ' ' + functionIndex;
              }
            }
            datasource.name = name;
            datasource.aliasName = name;
            datasource.entityName = name;
          } else if (datasource.unresolvedStateEntity) {
            datasource.name = 'Unresolved';
            datasource.entityName = 'Unresolved';
          }
          datasource.dataKeys.forEach((dataKey) => {
            if (datasource.generated) {
              dataKey._hash = Math.random();
              dataKey.color = this.utils.getMaterialColor(index);
            }
            index++;
          });
          this.updateDatasourceKeyLabels(datasource);
        });
        return result;
      })
    );
  }

  private updateDatasourceKeyLabels(datasource: Datasource) {
    datasource.dataKeys.forEach((dataKey) => {
      this.updateDataKeyLabel(dataKey, datasource);
    });
  }

  private updateDataKeyLabel(dataKey: DataKey, datasource: Datasource) {
    if (!dataKey.pattern) {
      dataKey.pattern = deepClone(dataKey.label);
    }
    dataKey.label = createLabelFromDatasource(datasource, dataKey.pattern);
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
}

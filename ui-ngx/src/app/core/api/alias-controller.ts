///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { IAliasController, AliasInfo, IStateController } from '@core/api/widget-api.models';
import { Observable, of, Subject } from 'rxjs';
import { Datasource } from '@app/shared/models/widget.models';
import { deepClone } from '@core/utils';
import { EntityService } from '@core/http/entity.service';
import { UtilsService } from '@core/services/utils.service';
import { EntityAliases } from '@shared/models/alias.models';
import { EntityInfo } from '@shared/models/entity.models';
import * as equal from 'deep-equal';

export class DummyAliasController implements IAliasController {

  entityAliasesChanged: Observable<Array<string>>;
  entityAliasResolved: Observable<string>;

  [key: string]: any | null;

  constructor() {
    this.entityAliasesChanged = new Subject<Array<string>>().asObservable();
    this.entityAliasResolved = new Subject<string>().asObservable();
  }

  getAliasInfo(aliasId): Observable<AliasInfo> {
    return of(null);
  }

  resolveDatasources(datasources: Array<Datasource>): Observable<Array<Datasource>> {
    return of(deepClone(datasources));
  }

  getEntityAliases(): EntityAliases {
    return undefined;
  }

  getInstantAliasInfo(aliasId: string): AliasInfo {
    return undefined;
  }

  updateCurrentAliasEntity(aliasId: string, currentEntity: EntityInfo) {
  }

  updateEntityAliases(entityAliases: EntityAliases) {
  }

}

export class AliasController implements IAliasController {

  private entityAliasesChangedSubject = new Subject<Array<string>>();
  entityAliasesChanged: Observable<Array<string>> = this.entityAliasesChangedSubject.asObservable();

  private entityAliasResolvedSubject = new Subject<string>();
  entityAliasResolved: Observable<string> = this.entityAliasResolvedSubject.asObservable();

  entityAliases: EntityAliases;

  resolvedAliases: {[aliasId: string]: AliasInfo} = {};

  [key: string]: any | null;

  constructor(private utils: UtilsService,
              private entityService: EntityService,
              private stateController: IStateController,
              private origEntityAliases: EntityAliases) {
    this.entityAliases = deepClone(this.origEntityAliases);
  }

  getAliasInfo(aliasId: string): Observable<AliasInfo> {
    return of(null);
  }

  resolveDatasources(datasources: Array<Datasource>): Observable<Array<Datasource>> {
    return of(deepClone(datasources));
  }

  getEntityAliases(): EntityAliases {
    return this.entityAliases;
  }

  getInstantAliasInfo(aliasId: string): AliasInfo {
    return this.resolvedAliases[aliasId];
  }

  updateCurrentAliasEntity(aliasId: string, currentEntity: EntityInfo) {
    const aliasInfo = this.resolvedAliases[aliasId];
    if (aliasInfo) {
      const prevCurrentEntity = aliasInfo.currentEntity;
      if (!equal(currentEntity, prevCurrentEntity)) {
        aliasInfo.currentEntity = currentEntity;
        this.entityAliasesChangedSubject.next([aliasId]);
      }
    }
  }

  updateEntityAliases(entityAliases: EntityAliases) {
  }

}

///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';

export interface Unit {
  name: string;
  symbol: string;
  tags: string[];
}

export enum UnitsType {
  capacity = 'capacity'
}

export enum Units {
  percent = '%',
  liters = 'L'
}

export const unitBySymbol = (_units: Array<Unit>, symbol: string): Unit => _units.find(u => u.symbol === symbol);

const searchUnitTags = (unit: Unit, searchText: string): boolean =>
  !!unit.tags.find(t => t.toUpperCase().includes(searchText.toUpperCase()));

export const searchUnits = (_units: Array<Unit>, searchText: string): Array<Unit> => _units.filter(
    u => u.symbol.toUpperCase().includes(searchText.toUpperCase()) ||
      u.name.toUpperCase().includes(searchText.toUpperCase()) ||
      searchUnitTags(u, searchText)
);

export const getUnits = (resourcesService: ResourcesService): Observable<Array<Unit>> =>
  resourcesService.loadJsonResource('/assets/metadata/units.json');

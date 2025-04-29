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

import { AllMeasures } from '@core/services/unit/definitions/all';

export enum UnitsType {
  capacity = 'capacity'
}

export type TbUnitConvertor = (value: number) => number;
export type UnitDescriptionGroupByMeasure<TMeasure extends string> = Partial<Record<TMeasure, UnitDescription[]>>;

export interface UnitDescription {
  abbr: string;
  measure: AllMeasures;
  system: UnitSystem;
  name: string;
  tags: string[];
}

export enum UnitSystem {
  METRIC = 'METRIC',
  IMPERIAL = 'IMPERIAL',
  HYBRID = 'HYBRID'
}

export const UnitSystems = Object.values(UnitSystem);

export interface Unit {
  name: string;
  tags: string[];
  to_anchor: number;
  anchor_shift?: number;
}

export type TbUnit = string | TbUnitMapping;

export interface TbUnitMapping {
  from: string;
  METRIC: string;
  IMPERIAL: string;
  HYBRID: string;
}

export type TbMeasure<TUnits extends string> = Partial<Record<UnitSystem, TbMeasureUnits<TUnits>>>;

export interface TbMeasureUnits<TUnits extends string> {
  ratio?: number;
  transform?: (value: number) => number;
  units?: Partial<Record<TUnits, Unit>>;
}

export interface Conversion<TMeasures extends string, TUnits extends string> {
  abbr: TUnits;
  measure: TMeasures;
  system: UnitSystem;
  unit: Unit;
}

export type UnitCache<TMeasures, TUnits> = Map<
  string,
  {
    system: UnitSystem;
    measure: TMeasures;
    unit: Unit;
    abbr: TUnits;
  }
>;

const searchUnitTags = (unit: UnitDescription, searchText: string): boolean =>
  !!unit.tags.find(t => t.toUpperCase().includes(searchText));

export const searchUnits = (_units: Array<UnitDescription>, searchText: string): Array<UnitDescription> => _units.filter(
    u => u.abbr.toUpperCase().includes(searchText) ||
      u.name.toUpperCase().includes(searchText) ||
      searchUnitTags(u, searchText)
);

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
import { Injector } from '@angular/core';
import { isDefinedAndNotNull, isNotEmptyStr, isNumeric } from '@core/utils';
import { UnitService } from '@core/services/unit/unit.service';

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

const searchUnitTags = (unit: UnitDescription, searchText: string): boolean =>
  !!unit.tags.find(t => t.toUpperCase().includes(searchText));

export const searchUnits = (_units: Array<UnitDescription>, searchText: string): Array<UnitDescription> => _units.filter(
    u => u.abbr.toUpperCase().includes(searchText) ||
      u.name.toUpperCase().includes(searchText) ||
      searchUnitTags(u, searchText)
);

export interface FormatValueSettingProcessor {
  dec?: number;
  units?: TbUnit;
  showZeroDecimals?: boolean;
}

export abstract class FormatValueProcessor {

  static fromSettings($injector: Injector, settings: FormatValueSettingProcessor): FormatValueProcessor {
    if (typeof settings.units !== 'string' && isDefinedAndNotNull(settings.units?.from)) {
      return new ConvertUnitProcessor($injector, settings)
    } else {
      return new SimpleUnitProcessor($injector, settings);
    }
  }

  protected constructor(protected $injector: Injector,
                        protected settings: FormatValueSettingProcessor) {
  }

  abstract format(value: any): string;
}

export class SimpleUnitProcessor extends FormatValueProcessor {

  private readonly isDefinedUnit: boolean;
  private readonly isDefinedDec: boolean;
  private readonly showZeroDecimals: boolean;

  constructor(protected $injector: Injector,
              protected settings: FormatValueSettingProcessor) {
    super($injector, settings);
    this.isDefinedUnit = isNotEmptyStr(settings.units);
    this.isDefinedDec = isDefinedAndNotNull(settings.dec);
    this.showZeroDecimals = !!settings.showZeroDecimals;
  }

  format(value: any): string {
    if (isDefinedAndNotNull(value) && isNumeric(value) && (this.isDefinedDec || this.isDefinedUnit || Number(value).toString() === value)) {
      let formatted = value;
      if (this.isDefinedDec) {
        formatted = Number(formatted).toFixed(this.settings.dec);
      }
      if (!this.showZeroDecimals) {
        formatted = Number(formatted)
      }
      formatted = formatted.toString();
      if (this.isDefinedUnit) {
        formatted += ` ${this.settings.units}`;
      }
      return formatted;
    }
    return value ?? '';
  }
}

export class ConvertUnitProcessor extends FormatValueProcessor {

  private readonly isDefinedDec: boolean;
  private readonly showZeroDecimals: boolean;
  private readonly unitConvertor: TbUnitConvertor;
  private readonly unitAbbr: string;

  constructor(protected $injector: Injector,
              protected settings: FormatValueSettingProcessor) {
    super($injector, settings);
    const unitService = this.$injector.get(UnitService);
    const userUnitSystem = unitService.getUnitSystem();
    const unit = settings.units as TbUnitMapping;
    const fromUnit = unit.from;
    this.unitAbbr = isNotEmptyStr(unit[userUnitSystem]) ? unit[userUnitSystem] : fromUnit;
    try {
      this.unitConvertor = unitService.geUnitConvertor(fromUnit, this.unitAbbr);
    } catch (e) {/**/}

    this.isDefinedDec = isDefinedAndNotNull(settings.dec);
    this.showZeroDecimals = !!settings.showZeroDecimals;
  }

  format(value: any): string {
    if (isDefinedAndNotNull(value) && isNumeric(value)) {
      let formatted: number | string = Number(value);
      if (this.unitConvertor) {
        formatted = this.unitConvertor(value);
      }
      if (this.isDefinedDec) {
        formatted = Number(formatted).toFixed(this.settings.dec);
      }
      if (!this.showZeroDecimals) {
        formatted = Number(formatted)
      }
      formatted = formatted.toString();
      if (this.unitAbbr) {
        formatted += ` ${this.unitAbbr}`;
      }
      return formatted;
    }
    return value ?? '';
  }
}

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

import { Injectable } from '@angular/core';
import moment from 'moment-timezone';
import {
  AllMeasures,
  AllMeasuresUnits,
  Converter,
  getUnitConverter,
  TbUnitConverter,
  TbUnitMapping,
  UnitInfo,
  UnitInfoGroupByMeasure,
  UnitSystem
} from '@shared/models/unit.models';
import { isNotEmptyStr, isObject } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Injectable({
  providedIn: 'root'
})
export class UnitService {

  private currentUnitSystem: UnitSystem = UnitSystem.METRIC;
  private converter: Converter;

  constructor(private translate: TranslateService) {
    this.translate.onLangChange.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.converter = getUnitConverter(this.translate);
      console.warn(this.converter.listUnits());
      console.warn(this.converter.listUnits(null, UnitSystem.IMPERIAL));
    });
  }

  getUnitSystem(): UnitSystem {
    return this.currentUnitSystem;
  }

  setUnitSystem(unitSystem: UnitSystem) {
    if (isNotEmptyStr(unitSystem)) {
      this.currentUnitSystem = unitSystem;
    } else {
      this.currentUnitSystem = this.getUnitSystemByTimezone();
    }
    console.warn('[Unit system] setUnitSystem', this.currentUnitSystem);
  }

  getUnits(measure?: AllMeasures, unitSystem?: UnitSystem): UnitInfo[] {
    return this.converter?.listUnits(measure, unitSystem);
  }

  getUnitsGroupedByMeasure(measure?: AllMeasures, unitSystem?: UnitSystem): UnitInfoGroupByMeasure<AllMeasures> {
    return this.converter?.unitsGroupByMeasure(measure, unitSystem);
  }

  getUnitInfo(symbol: AllMeasuresUnits | string): UnitInfo {
    return this.converter.describe(symbol);
  }

  getDefaultUnit(measure: AllMeasures, unitSystem: UnitSystem): AllMeasuresUnits {
    return this.converter.getDefaultUnit(measure, unitSystem);
  }

  geUnitConverter(unit: TbUnitMapping): TbUnitConverter;
  geUnitConverter(from: string, to: string): TbUnitConverter;
  geUnitConverter(unit: TbUnitMapping | string, to?: string): TbUnitConverter {
    try {
      if (unit !== null && typeof unit === 'object') {
        const target = this.getTargetUnitSymbol(unit);
        return this.converter.getUnitConverter(unit.from, target);
      }
      return this.converter.getUnitConverter(unit as string, to);
    } catch (e) {
      console.warn(e);
      return (x: number) => x;
    }
  }

  getTargetUnitSymbol(unit: TbUnitMapping | string): string {
    if (isObject(unit)) {
      return isNotEmptyStr(unit[this.currentUnitSystem]) ? unit[this.currentUnitSystem] : (unit as TbUnitMapping).from;
    }
    return typeof unit === 'string' ? unit : null;
  }

  convertUnitValue(value: number, unit: TbUnitMapping): number;
  convertUnitValue(value: number, from: string, to: string): number;
  convertUnitValue(value: number, unit: string | TbUnitMapping, to?: string): number {
    try {
      if (unit !== null && typeof unit === 'object') {
        const target = this.getTargetUnitSymbol(unit);
        return this.converter.convert(value, unit.from, target);
      }
      return this.converter.convert(value, unit as string, to);
    } catch (e) {
      console.warn(e);
      return value;
    }
  }

  private getUnitSystemByTimezone(): UnitSystem {
    const timeZone = moment.tz.guess(true);
    const imperialCountries = ['US', 'LR', 'MM'];

    if (moment.tz.zonesForCountry('GB').includes(timeZone)) {
      return UnitSystem.HYBRID;
    }
    return imperialCountries.some(country =>
      moment.tz.zonesForCountry(country).includes(timeZone)
    ) ? UnitSystem.IMPERIAL : UnitSystem.METRIC;
  }
}

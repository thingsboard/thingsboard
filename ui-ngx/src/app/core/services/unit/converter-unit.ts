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

import {
  Conversion,
  TbMeasure,
  TbUnitConvertor,
  Unit,
  UnitCache,
  UnitDescription,
  UnitDescriptionGroupByMeasure,
  UnitSystem
} from '@shared/models/unit.models';
import { AllMeasures } from '@core/services/unit/definitions/all';
import { TranslateService } from '@ngx-translate/core';

type Entries<T, S extends keyof T> = [S, T[keyof T]];

export class Converter<
  TMeasures extends AllMeasures,
  TUnits extends string,
> {
  private readonly measureData: Record<TMeasures, TbMeasure<TUnits>>;
  private unitCache: Map<
    string,
    {
      system: UnitSystem;
      measure: TMeasures;
      unit: Unit;
      abbr: TUnits;
    }
  >;

  constructor(
    measures: Record<TMeasures, TbMeasure<TUnits>>,
    unitCache: UnitCache<TMeasures, TUnits>
  ) {
    this.measureData = measures;
    this.unitCache = unitCache;
  }

  convertor(from: TUnits | string, to: TUnits | string): TbUnitConvertor {
    return (value: number) => this.convert(value, from, to);
  }

  convert(value: number, from: TUnits | string, to: TUnits | string): number {
    const origin = this.getUnit(from);
    const destination = this.getUnit(to);

    if (!origin) {
      throw new Error(`Unsupported unit: ${from}`);
    }
    if (!destination) {
      throw new Error(`Unsupported unit: ${to}`);
    }
    if (origin.abbr === destination.abbr) {
      return value;
    }
    if (destination.measure !== origin.measure) {
      throw Error(`Cannot convert incompatible measures: ${origin.measure} to ${destination.measure}`);
    }
    let result = value * origin.unit.to_anchor;
    if (origin.unit.anchor_shift) {
      result -= origin.unit.anchor_shift;
    }
    if (origin.system !== destination.system) {
      const measureUnits = this.measureData[origin.measure][origin.system];
      const transform = measureUnits?.transform;
      const ratio = measureUnits?.ratio;
      if (typeof transform === 'function') {
        result = transform(result);
      } else if (typeof ratio === 'number') {
        result *= ratio;
      } else {
        throw Error('System anchor requires a defined ratio or transform function');
      }
    }

    if (destination.unit.anchor_shift) {
      result += destination.unit.anchor_shift;
    }
    return result / destination.unit.to_anchor;
  }

  getDefaultUnit(measureName: TMeasures | (string & {}), unitSystem: UnitSystem): TUnits {
    if (!this.isMeasure(measureName)) {
      return null;
    }
    const units = this.getUnitsForMeasure(measureName, unitSystem);
    if (!units) {
      return null;
    }
    for (const [abbr, unit] of Object.entries(units) as [TUnits, Unit][]) {
      if (unit.to_anchor === 1 && (!unit.anchor_shift || unit.anchor_shift === 0)) {
        return abbr;
      }
    }
    return null;
  }

  getUnit(abbr: TUnits | (string & {})): Conversion<TMeasures, TUnits> | null {
    return this.unitCache.get(abbr) ?? null;
  }

  describe(abbr: TUnits | (string & {})): UnitDescription {
    const unit = this.getUnit(abbr);
    return unit ? this.describeUnit(unit) : null;
  }

  list(measureName?: TMeasures, unitSystem?: UnitSystem): UnitDescription[] {
    const results: UnitDescription[] = [];

    const measures = measureName
      ? { [measureName]: this.measureData[measureName] } as Record<TMeasures, TbMeasure<TUnits>>
      : this.measureData;

    for (const [name, measure] of Object.entries(measures) as [TMeasures, TbMeasure<TUnits>][]) {
      if (!this.isMeasure(name)) {
        continue;
      }

      const systems = unitSystem
        ? [unitSystem]
        : (Object.keys(measure) as UnitSystem[]);

      for (const system of systems) {
        const units = this.getUnitsForMeasure(name, system);
        if (!units) {
          continue;
        }

        for (const [abbr, unit] of Object.entries(units) as [TUnits, Unit][]) {
          results.push(
            this.describeUnit({
              abbr,
              measure: name as TMeasures,
              system,
              unit,
            })
          );
        }
      }
    }
    return results;
  }

  listGroupByMeasure(measureName?: TMeasures, unitSystem?: UnitSystem): UnitDescriptionGroupByMeasure<TMeasures> | never {
    const results: UnitDescriptionGroupByMeasure<TMeasures> = {};

    const measures = measureName
      ? { [measureName]: this.measureData[measureName]} as Record<TMeasures, TbMeasure<TUnits>>
      : this.measureData;

    for (const [name, measure] of Object.entries(measures) as [TMeasures, TbMeasure<TUnits>][]) {
      if (!this.isMeasure(name)) {
        continue;
      }

      results[name] = [];

      const systems = unitSystem
        ? [unitSystem]
        : (Object.keys(measure) as UnitSystem[]);

      for (const system of systems) {
        const units = this.getUnitsForMeasure(name, system);
        if (!units) {
          continue;
        }

        for (const [abbr, unit] of Object.entries(units) as [TUnits, Unit][]) {
          results[name].push(
            this.describeUnit({
              abbr,
              measure: name as TMeasures,
              system,
              unit,
            })
          );
        }
      }
    }
    return results;
  }

  private describeUnit(unit: Conversion<TMeasures, TUnits>): UnitDescription {
    return {
      abbr: unit.abbr,
      measure: unit.measure,
      system: unit.system,
      name: unit.unit.name,
      tags: unit.unit.tags
    };
  }

  private isMeasure(measureName: string): measureName is TMeasures {
    return measureName in this.measureData;
  }

  private getUnitsForMeasure(
    measureName: TMeasures,
    unitSystem: UnitSystem
  ): Partial<Record<TUnits, Unit>> | null {
    const measure = this.measureData[measureName];
    let system = unitSystem;
    let units = measure[system]?.units;
    if (!units && unitSystem === UnitSystem.IMPERIAL) {
      system = UnitSystem.METRIC;
      units = measure[system]?.units;
    }
    return units ?? null;
  }
}

export function buildUnitCache<
  TMeasures extends string,
  TUnits extends string,
>(measures: Record<TMeasures, TbMeasure<TUnits>>,
  translate: TranslateService
) {
  const unitCache: UnitCache<TMeasures, TUnits> = new Map();
  for (const [measureName, measure] of Object.entries(measures) as Entries<
    typeof measures,
    TMeasures
  >[]) {
    for (const [systemName, system] of Object.entries(
      measure
    ) as Entries<TbMeasure<TUnits>, UnitSystem>[]) {
      for (const [testAbbr, unit] of Object.entries(system.units) as Entries<
        Record<TUnits, Unit>,
        TUnits
      >[]) {
        unit.name = translate.instant(unit.name);
        unitCache.set(testAbbr, {
          measure: measureName,
          system: systemName,
          abbr: testAbbr,
          unit,
        });
      }
    }
  }
  return unitCache;
}

export function configureMeasurements<
  TMeasures extends AllMeasures,
  TUnits extends string,
>(
  measures: Record<TMeasures, TbMeasure<TUnits>>,
  translate: TranslateService
): Converter<TMeasures, TUnits> {
  if (typeof measures !== 'object') {
    throw new TypeError('The measures argument needs to be an object');
  }

  const unitCache = buildUnitCache(measures, translate);
  return new Converter<TMeasures, TUnits>(measures, unitCache);
}

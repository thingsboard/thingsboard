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
  TbMeasure,
  TbUnitConvertor,
  Unit,
  UnitDescription,
  UnitDescriptionGroupByMeasure,
  UnitSystem
} from '@shared/models/unit.models';
import { AllMeasures } from '@core/services/unit/definitions/all';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';

export interface Conversion<
  TMeasures extends string,
  TUnits extends string,
> {
  abbr: TUnits;
  measure: TMeasures;
  system: UnitSystem;
  unit: Unit;
}

// export interface BestResult<TUnits extends string> {
//   val: number;
//   unit: TUnits;
//   name: string;
//   tags: string[];
// }

type Entries<T, S extends keyof T> = [S, T[keyof T]];

export type UnitCache<TMeasures, TUnits> = Map<
  string,
  {
    system: UnitSystem;
    measure: TMeasures;
    unit: Unit;
    abbr: TUnits;
  }
>;

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

  convertor(from: TUnits | (string & {}), to: TUnits | (string & {})): TbUnitConvertor{
    const origin = this.getUnit(from);
    if (origin === null) {
      throw Error(`Unsupported unit ${from}`);
    }
    const destination = this.getUnit(to);
    if (destination === null) {
      throw Error(`Unsupported unit ${from}`);
    }
    if (origin.abbr === destination.abbr) {
      return (value: number) => value;
    }
    if (destination.measure !== origin.measure) {
      throw Error(`Cannot convert incompatible measures of ${destination.measure} and ${origin.measure}`);
    }
    return (value: number): number => {
      let result = value * origin.unit.to_anchor;
      if (origin.unit.anchor_shift) {
        result -= origin.unit.anchor_shift;
      }

      if (origin.system !== destination.system) {
        const measure = this.measureData[origin.measure];
        const transform = measure[origin.system]?.transform;
        const ratio = measure[origin.system]?.ratio;

        if (typeof transform === 'function') {
          result = transform(result);
        } else if (typeof ratio === 'number') {
          result *= ratio;
        } else {
          throw Error('A system anchor needs to either have a defined ratio number or a transform function.');
        }
      }

      if (destination.unit.anchor_shift) {
        result += destination.unit.anchor_shift;
      }
      return result / destination.unit.to_anchor;
    };
  }

  convert(value: number, from: TUnits | (string & {}), to: TUnits | (string & {})): number {
    const origin = this.getUnit(from);
    if (origin === null) {
      throw Error(`Unsupported unit ${from}`);
    }
    const destination = this.getUnit(to);
    if (destination === null) {
      throw Error(`Unsupported unit ${from}`);
    }
    if (origin.abbr === destination.abbr) {
      return value;
    }
    if (destination.measure !== origin.measure) {
      throw Error(`Cannot convert incompatible measures of ${destination.measure} and ${origin.measure}`);
    }
    let result = value * origin.unit.to_anchor;
    if (origin.unit.anchor_shift) {
      result -= origin.unit.anchor_shift;
    }
    if (origin.system !== destination.system) {
      const measure = this.measureData[origin.measure];
      const transform = measure[origin.system]?.transform;
      const ratio = measure[origin.system]?.ratio;
      if (typeof transform === 'function') {
        result = transform(result);
      } else if (typeof ratio === 'number') {
        result *= ratio;
      } else {
        throw Error('A system anchor needs to either have a defined ratio number or a transform function.');
      }
    }

    if (destination.unit.anchor_shift) {
      result += destination.unit.anchor_shift;
    }
    return result / destination.unit.to_anchor;
  }

  // toBest(options?: {
  //   exclude?: (TUnits | (string & {}))[];
  //   cutOffNumber?: number;
  //   system?: TSystems | (string & {});
  // }): BestResult<TUnits> | null {
  //   if (this.origin == null)
  //     throw new OperationOrderError('.toBest must be called after .from');
  //
  //   const isNegative = this.val < 0;
  //
  //   let exclude: (TUnits | (string & {}))[] = [];
  //   let cutOffNumber = isNegative ? -1 : 1;
  //   let system: TSystems | (string & {}) = this.origin.system;
  //
  //   if (typeof options === 'object') {
  //     exclude = options.exclude ?? [];
  //     cutOffNumber = options.cutOffNumber ?? cutOffNumber;
  //     system = options.system ?? this.origin.system;
  //   }
  //
  //   let best: BestResult<TUnits> | null = null;
  //   /**
  //    Looks through every possibility for the 'best' available unit.
  //    i.e. Where the value has the fewest numbers before the decimal point,
  //    but is still higher than 1.
  //    */
  //   for (const possibility of this.possibilities()) {
  //     const unit = this.describe(possibility);
  //     const isIncluded = exclude.indexOf(possibility) === -1;
  //
  //     if (isIncluded && unit.system === system) {
  //       const result = this.to(possibility);
  //       if (isNegative ? result > cutOffNumber : result < cutOffNumber) {
  //         continue;
  //       }
  //       if (
  //         best === null ||
  //         (isNegative
  //           ? result <= cutOffNumber && result > best.val
  //           : result >= cutOffNumber && result < best.val)
  //       ) {
  //         best = {
  //           val: result,
  //           unit: possibility,
  //           name: unit.name,
  //           tags: unit.tags
  //         };
  //       }
  //     }
  //   }
  //
  //   if (best == null) {
  //     return {
  //       val: this.val,
  //       unit: this.origin.abbr,
  //       name: this.origin.unit.name,
  //       tags: this.origin.unit.tags
  //     };
  //   }
  //
  //   return best;
  // }

  getUnit(abbr: TUnits | (string & {})): Conversion<TMeasures, TUnits> | null {
    return this.unitCache.get(abbr) ?? null;
  }

  describe(abbr: TUnits | (string & {})): UnitDescription {
    const result = this.getUnit(abbr);

    if (result != null) {
      return this.describeUnit(result);
    }
    return null;
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

  list(measureName?: TMeasures | (string & {}), unitSystem?: UnitSystem): UnitDescription[] | never {
    const list = [];

    if (isDefinedAndNotNull(measureName)) {
      if (!this.isMeasure(measureName)) {
        console.log(`Measure "${measureName}" not found.`);
        return list;
      }
      const measure = this.measureData[measureName];
      if (isDefinedAndNotNull(unitSystem)) {
        let currentUnitSystem = unitSystem;
        let units = measure[currentUnitSystem];
        if (isUndefinedOrNull(units)) {
          if (currentUnitSystem === UnitSystem.IMPERIAL) {
            currentUnitSystem = UnitSystem.METRIC;
            units = measure[currentUnitSystem];
          }
          if (!units) {
            console.log(`Measure "${measureName}" in ${currentUnitSystem} system is not found.`);
            return list;
          }
        }
        for (const [abbr, unit] of Object.entries(
          units.units
        )) {
          list.push(
            this.describeUnit({
              abbr: abbr as TUnits,
              measure: measureName as TMeasures,
              system: currentUnitSystem,
              unit: unit as Unit,
            })
          );
        }
      } else {
        for (const [systemName, units] of Object.entries(
          measure
        ) as Entries<TbMeasure<TUnits>, UnitSystem>[]) {
          for (const [abbr, unit] of Object.entries(
            units.units as Partial<Record<TUnits, Unit>>
          )) {
            list.push(
              this.describeUnit({
                abbr: abbr as TUnits,
                measure: measureName as TMeasures,
                system: systemName,
                unit: unit as Unit,
              })
            );
          }
        }
      }
    } else {
      for (const [name, measure] of Object.entries(this.measureData)) {
        if (isDefinedAndNotNull(unitSystem)) {
          let currentUnitSystem = unitSystem;
          let units = (measure as TbMeasure<TUnits>)[currentUnitSystem]?.units;
          if (isUndefinedOrNull(units)) {
            if (currentUnitSystem === UnitSystem.IMPERIAL) {
              currentUnitSystem = UnitSystem.METRIC;
              units = (measure as TbMeasure<TUnits>)[currentUnitSystem]?.units;
            }
            if (!units) {
              console.log(`Measure "${measureName}" in ${currentUnitSystem} system is not found.`);
              continue;
            }
          }
          for (const [abbr, unit] of Object.entries(
            units as Partial<Record<TUnits, Unit>>
          )) {
            list.push(
              this.describeUnit({
                abbr: abbr as TUnits,
                measure: name as TMeasures,
                system: currentUnitSystem,
                unit: unit as Unit,
              })
            );
          }
        } else {
          for (const [systemName, units] of Object.entries(
            measure
          ) as Entries<TbMeasure<TUnits>, UnitSystem>[]) {
            for (const [abbr, unit] of Object.entries(
              units.units as Partial<Record<TUnits, Unit>>
            )) {
              list.push(
                this.describeUnit({
                  abbr: abbr as TUnits,
                  measure: name as TMeasures,
                  system: systemName,
                  unit: unit as Unit,
                })
              );
            }
          }
        }
      }
    }

    return list;
  }

  listGroupByMeasure(measureName?: TMeasures | (string & {}), unitSystem?: UnitSystem): UnitDescriptionGroupByMeasure<TMeasures> | never {
    const list: UnitDescriptionGroupByMeasure<TMeasures> = {};

    if (isDefinedAndNotNull(measureName)) {
      if (!this.isMeasure(measureName)) {
        console.log(`Measure "${measureName}" not found.`);
        return list;
      }
      const measure = this.measureData[measureName];
      if (isDefinedAndNotNull(unitSystem)) {
        let currentUnitSystem = unitSystem;
        let units = measure[currentUnitSystem];
        if (isUndefinedOrNull(units)) {
          if (currentUnitSystem === UnitSystem.IMPERIAL) {
            currentUnitSystem = UnitSystem.METRIC;
            units = measure[currentUnitSystem];
          }
          if (!units) {
            console.log(`Measure "${measureName}" in ${currentUnitSystem} system is not found.`);
            return list;
          }
        }
        list[measureName] = [];
        const unitsDescription = list[measureName];
        for (const [abbr, unit] of Object.entries(
          units.units
        )) {
          unitsDescription.push(
            this.describeUnit({
              abbr: abbr as TUnits,
              measure: measureName as TMeasures,
              system: currentUnitSystem,
              unit: unit as Unit,
            })
          );
        }
      } else {
        for (const [systemName, units] of Object.entries(
          measure
        ) as Entries<TbMeasure<TUnits>, UnitSystem>[]) {
          list[measureName] = [];
          const unitsDescription = list[measureName];
          for (const [abbr, unit] of Object.entries(
            units.units as Partial<Record<TUnits, Unit>>
          )) {
            unitsDescription.push(
              this.describeUnit({
                abbr: abbr as TUnits,
                measure: measureName as TMeasures,
                system: systemName,
                unit: unit as Unit,
              })
            );
          }
        }
      }
    } else {
      for (const [name, measure] of Object.entries(this.measureData)) {
        if (isDefinedAndNotNull(unitSystem)) {
          let currentUnitSystem = unitSystem;
          let units = (measure as TbMeasure<TUnits>)[currentUnitSystem]?.units;
          if (isUndefinedOrNull(units)) {
            if (currentUnitSystem === UnitSystem.IMPERIAL) {
              currentUnitSystem = UnitSystem.METRIC;
              units = (measure as TbMeasure<TUnits>)[currentUnitSystem]?.units;
            }
            if (!units) {
              console.log(`Measure "${name}" in ${currentUnitSystem} system is not found.`);
              continue;
            }
          }
          list[name] = [];
          const unitsDescription = list[name];
          for (const [abbr, unit] of Object.entries(
            units as Partial<Record<TUnits, Unit>>
          )) {
            unitsDescription.push(
              this.describeUnit({
                abbr: abbr as TUnits,
                measure: name as TMeasures,
                system: currentUnitSystem,
                unit: unit as Unit,
              })
            );
          }
        } else {
          list[name] = [];
          const unitsDescription = list[name];
          for (const [systemName, units] of Object.entries(
            measure
          ) as Entries<TbMeasure<TUnits>, UnitSystem>[]) {
            for (const [abbr, unit] of Object.entries(
              units.units as Partial<Record<TUnits, Unit>>
            )) {
              unitsDescription.push(
                this.describeUnit({
                  abbr: abbr as TUnits,
                  measure: name as TMeasures,
                  system: systemName,
                  unit: unit as Unit,
                })
              );
            }
          }
        }
      }
    }

    return list;
  }

  private isMeasure(measureName: string): measureName is TMeasures {
    return measureName in this.measureData;
  }

  // possibilities(forMeasure?: TMeasures | (string & {})): TUnits[] {
  //   let possibilities: TUnits[] = [];
  //   let list_measures: TMeasures[] = [];
  //
  //   if (typeof forMeasure == 'string' && this.isMeasure(forMeasure)) {
  //     list_measures.push(forMeasure);
  //   } else if (this.origin != null) {
  //     list_measures.push(this.origin.measure);
  //   } else {
  //     list_measures = Object.keys(this.measureData) as TMeasures[];
  //   }
  //
  //   for (const measure of list_measures) {
  //     const systems = this.measureData[measure].systems;
  //
  //     for (const system of Object.values(systems)) {
  //       possibilities = [
  //         ...possibilities,
  //         ...(Object.keys(system as Record<TUnits, Unit>) as TUnits[]),
  //       ];
  //     }
  //   }
  //
  //   return possibilities;
  // }

  // measures(): TMeasures[] {
  //   return Object.keys(this.measureData) as TMeasures[];
  // }
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

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

import acceleration, { AccelerationUnits } from '@shared/models/units/acceleration';
import angle, { AngleUnits } from '@shared/models/units/angle';
import angularAcceleration, { AngularAccelerationUnits } from '@shared/models/units/angular-acceleration';
import area, { AreaUnits } from '@shared/models/units/area';
import charge, { ChargeUnits } from '@shared/models/units/charge';
import digital, { DigitalUnits } from '@shared/models/units/digital';
import electricCurrent, { ElectricCurrentUnits } from '@shared/models/units/electric-current';
import energy, { EnergyUnits } from '@shared/models/units/energy';
import force, { ForceUnits } from '@shared/models/units/force';
import frequency, { FrequencyUnits } from '@shared/models/units/frequency';
import illuminance, { IlluminanceUnits } from '@shared/models/units/illuminance';
import length, { LengthUnits } from '@shared/models/units/length';
import mass, { MassUnits } from '@shared/models/units/mass';
import partsPer, { PartsPerUnits } from '@shared/models/units/parts-per';
import power, { PowerUnits } from '@shared/models/units/power';
import pressure, { PressureUnits } from '@shared/models/units/pressure';
import speed, { SpeedUnits } from '@shared/models/units/speed';
import temperature, { TemperatureUnits } from '@shared/models/units/temperature';
import time, { TimeUnits } from '@shared/models/units/time';
import torque, { TorqueUnits } from '@shared/models/units/torque';
import voltage, { VoltageUnits } from '@shared/models/units/voltage';
import volume, { VolumeUnits } from '@shared/models/units/volume';
import volumeFlowRate, { VolumeFlowRateUnits } from '@shared/models/units/volume-flow-rate';
import { TranslateService } from '@ngx-translate/core';

export type AllMeasuresUnits =
  | AccelerationUnits
  | AngleUnits
  | AngularAccelerationUnits
  | AreaUnits
  | ChargeUnits
  | DigitalUnits
  | ElectricCurrentUnits
  | EnergyUnits
  | ForceUnits
  | FrequencyUnits
  | IlluminanceUnits
  | LengthUnits
  | MassUnits
  | PartsPerUnits
  | PowerUnits
  | PressureUnits
  | SpeedUnits
  | TemperatureUnits
  | TimeUnits
  | TorqueUnits
  | VoltageUnits
  | VolumeUnits
  | VolumeFlowRateUnits;

export type AllMeasures =
  | 'acceleration'
  | 'angle'
  | 'angular-acceleration'
  | 'area'
  | 'charge'
  | 'digital'
  | 'electric-current'
  | 'energy'
  | 'force'
  | 'frequency'
  | 'illuminance'
  | 'length'
  | 'mass'
  | 'parts-per'
  | 'power'
  | 'pressure'
  | 'speed'
  | 'temperature'
  | 'time'
  | 'torque'
  | 'voltage'
  | 'volume'
  | 'volume-flow-rate';

const allMeasures: Record<
  AllMeasures,
  TbMeasure<AllMeasuresUnits>
> = Object.freeze({
  acceleration,
  angle,
  'angular-acceleration': angularAcceleration,
  area,
  charge,
  digital,
  'electric-current': electricCurrent,
  energy,
  force,
  frequency,
  illuminance,
  length,
  mass,
  'parts-per': partsPer,
  power,
  pressure,
  speed,
  temperature,
  time,
  torque,
  voltage,
  volume,
  'volume-flow-rate': volumeFlowRate,
});

export enum UnitsType {
  capacity = 'capacity'
}

export type TbUnitConverter = (value: number) => number;
export type UnitInfoGroupByMeasure<TMeasure extends string> = Partial<Record<TMeasure, UnitInfo[]>>;

export interface UnitInfo {
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

export type UnitCache = Map<string, {
    system: UnitSystem;
    measure: AllMeasures;
    unit: Unit;
    abbr: AllMeasuresUnits;
  }
>;

const searchUnitTags = (unit: UnitInfo, searchText: string): boolean =>
  !!unit.tags.find(t => t.toUpperCase().includes(searchText));

export const searchUnits = (_units: Array<UnitInfo>, searchText: string): Array<UnitInfo> => _units.filter(
    u => u.abbr.toUpperCase().includes(searchText) ||
      u.name.toUpperCase().includes(searchText) ||
      searchUnitTags(u, searchText)
);

type Entries<T, S extends keyof T> = [S, T[keyof T]];

export class Converter {
  private readonly measureData: Record<AllMeasures, TbMeasure<AllMeasuresUnits>>;
  private unitCache: Map<
    string,
    {
      system: UnitSystem;
      measure: AllMeasures;
      unit: Unit;
      abbr: AllMeasuresUnits;
    }
  >;

  constructor(
    measures: Record<AllMeasures, TbMeasure<AllMeasuresUnits>>,
    unitCache: UnitCache
  ) {
    this.measureData = measures;
    this.unitCache = unitCache;
  }

  getUnitConverter(from: AllMeasuresUnits | string, to: AllMeasuresUnits | string): TbUnitConverter {
    return (value: number) => this.convert(value, from, to);
  }

  convert(value: number, from: AllMeasuresUnits | string, to: AllMeasuresUnits | string): number {
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

  getDefaultUnit(measureName: AllMeasures | (string & {}), unitSystem: UnitSystem): AllMeasuresUnits {
    if (!this.isMeasure(measureName)) {
      return null;
    }
    const units = this.getUnitsForMeasure(measureName, unitSystem);
    if (!units) {
      return null;
    }
    for (const [abbr, unit] of Object.entries(units) as [AllMeasuresUnits, Unit][]) {
      if (unit.to_anchor === 1 && (!unit.anchor_shift || unit.anchor_shift === 0)) {
        return abbr;
      }
    }
    return null;
  }

  getUnit(abbr: AllMeasuresUnits | string): Conversion<AllMeasures, AllMeasuresUnits> | null {
    return this.unitCache.get(abbr) ?? null;
  }

  describe(abbr: AllMeasuresUnits | string): UnitInfo {
    const unit = this.getUnit(abbr);
    return unit ? this.describeUnit(unit) : null;
  }

  listUnits(measureName?: AllMeasures, unitSystem?: UnitSystem): UnitInfo[] {
    const results: UnitInfo[] = [];

    const measures = measureName
      ? { [measureName]: this.measureData[measureName] } as Record<AllMeasures, TbMeasure<AllMeasuresUnits>>
      : this.measureData;

    for (const [name, measure] of Object.entries(measures) as [AllMeasures, TbMeasure<AllMeasuresUnits>][]) {
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

        for (const [abbr, unit] of Object.entries(units) as [AllMeasuresUnits, Unit][]) {
          results.push(
            this.describeUnit({
              abbr,
              measure: name as AllMeasures,
              system,
              unit,
            })
          );
        }
      }
    }
    return results;
  }

  unitsGroupByMeasure(measureName?: AllMeasures, unitSystem?: UnitSystem): UnitInfoGroupByMeasure<AllMeasures> | never {
    const results: UnitInfoGroupByMeasure<AllMeasures> = {};

    const measures = measureName
      ? { [measureName]: this.measureData[measureName]} as Record<AllMeasures, TbMeasure<AllMeasuresUnits>>
      : this.measureData;

    for (const [name, measure] of Object.entries(measures) as [AllMeasures, TbMeasure<AllMeasuresUnits>][]) {
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

        for (const [abbr, unit] of Object.entries(units) as [AllMeasuresUnits, Unit][]) {
          results[name].push(
            this.describeUnit({
              abbr,
              measure: name as AllMeasures,
              system,
              unit,
            })
          );
        }
      }
    }
    return results;
  }

  private describeUnit(unit: Conversion<AllMeasures, AllMeasuresUnits>): UnitInfo {
    return {
      abbr: unit.abbr,
      measure: unit.measure,
      system: unit.system,
      name: unit.unit.name,
      tags: unit.unit.tags
    };
  }

  private isMeasure(measureName: string): boolean {
    return measureName in this.measureData;
  }

  private getUnitsForMeasure(
    measureName: AllMeasures | string,
    unitSystem: UnitSystem
  ): Partial<Record<AllMeasuresUnits, Unit>> | null {
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

function buildUnitCache(measures: Record<AllMeasures, TbMeasure<AllMeasuresUnits>>,
                        translate: TranslateService
) {
  const unitCache: UnitCache = new Map();
  for (const [measureName, measure] of Object.entries(measures) as Entries<
    typeof measures,
    AllMeasures
  >[]) {
    for (const [systemName, system] of Object.entries(
      measure
    ) as Entries<TbMeasure<AllMeasuresUnits>, UnitSystem>[]) {
      for (const [testAbbr, unit] of Object.entries(system.units) as Entries<
        Record<AllMeasuresUnits, Unit>,
        AllMeasuresUnits
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

export function getUnitConverter(translate: TranslateService): Converter {
  const unitCache = buildUnitCache(allMeasures, translate);
  return new Converter(allMeasures, unitCache);
}

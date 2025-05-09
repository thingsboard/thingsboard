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

import absorbedDoseRate, { AbsorbedDoseRateUnits } from '@shared/models/units/absorbed-dose-rate';
import acceleration, { AccelerationUnits } from '@shared/models/units/acceleration';
import acidity, { AcidityUnits } from '@shared/models/units/acidity';
import airQualityIndex, { AirQualityIndexUnits } from '@shared/models/units/air-quality-index';
import amountOfSubstance, { AmountOfSubstanceUnits } from '@shared/models/units/amount-of-substance';
import angle, { AngleUnits } from '@shared/models/units/angle';
import angularAcceleration, { AngularAccelerationUnits } from '@shared/models/units/angular-acceleration';
import area, { AreaUnits } from '@shared/models/units/area';
import areaDensity, { AreaDensityUnits } from '@shared/models/units/area-density';
import capacitance, { CapacitanceUnits } from '@shared/models/units/capacitance';
import catalyticActivity, { CatalyticActivityUnits } from '@shared/models/units/catalytic-activity';
import catalyticConcentration, { CatalyticConcentrationUnits } from '@shared/models/units/catalytic-concentration';
import charge, { ChargeUnits } from '@shared/models/units/charge';
import currentDensity, { CurrentDensityUnits } from '@shared/models/units/current-density';
import dataTransferRate, { DataTransferRateUnits } from '@shared/models/units/data-transfer-rate';
import density, { DensityUnits } from '@shared/models/units/density';
import digital, { DigitalUnits } from '@shared/models/units/digital';
import dimensionRatio, { DimensionRatioUnits } from '@shared/models/units/dimension-ratio';
import dynamicViscosity, { DynamicViscosityUnits } from '@shared/models/units/dynamic-viscosity';
import earthquakeMagnitude, { EarthquakeMagnitudeUnits } from '@shared/models/units/earthquake-magnitude';
import electricChargeDensity, { ElectricChargeDensityUnits } from '@shared/models/units/electric-charge-density';
import electricCurrent, { ElectricCurrentUnits } from '@shared/models/units/electric-current';
import electricDipoleMoment, { ElectricDipoleMomentUnits } from '@shared/models/units/electric-dipole-moment';
import electricFieldStrength, { ElectricFieldStrengthUnits } from '@shared/models/units/electric-field-strength';
import electricFlux, { ElectricFluxUnits } from '@shared/models/units/electric-flux';
import electricPermittivity, { ElectricPermittivityUnits } from '@shared/models/units/electric-permittivity';
import electricPolarizability, { ElectricPolarizabilityUnits } from '@shared/models/units/electric-polarizability';
import electricalConductance, { ElectricalConductanceUnits } from '@shared/models/units/electrical-conductance';
import electricalConductivity, { ElectricalConductivityUnits } from '@shared/models/units/electrical-conductivity';
import energy, { EnergyUnits } from '@shared/models/units/energy';
import energyDensity, { EnergyDensityUnits } from '@shared/models/units/energy-density';
import force, { ForceUnits } from '@shared/models/units/force';
import fuelEfficiency, { FuelEfficiencyUnits } from '@shared/models/units/fuel-efficiency';
import frequency, { FrequencyUnits } from '@shared/models/units/frequency';
import heatCapacity, { HeatCapacityUnits } from '@shared/models/units/heat-capacity';
import illuminance, { IlluminanceUnits } from '@shared/models/units/illuminance';
import inductance, { InductanceUnits } from '@shared/models/units/inductance';
import kinematicViscosity, { KinematicViscosityUnits } from '@shared/models/units/kinematic-viscosity';
import length, { LengthUnits } from '@shared/models/units/length';
import lightExposure, { LightExposureUnits } from '@shared/models/units/light-exposure';
import linerChargeDensity, { LinerChargeDensityUnits } from '@shared/models/units/liner-charge-density';
import logarithmicRatio, { LogarithmicRatioUnits } from '@shared/models/units/logarithmic-ratio';
import luminousEfficacy, { LuminousEfficacyUnits } from '@shared/models/units/luminous-efficacy';
import luminousFlux, { LuminousFluxUnits } from '@shared/models/units/luminous-flux';
import luminousIntensity, { LuminousIntensityUnits } from '@shared/models/units/luminous-intensity';
import magneticFieldGradient, { MagneticFieldGradientUnits } from '@shared/models/units/magnetic-field-gradient';
import magneticFlux, { MagneticFluxUnits } from '@shared/models/units/magnetic-flux';
import magneticFluxDensity, { MagneticFluxDensityUnits } from '@shared/models/units/magnetic-flux-density';
import magneticMoment, { MagneticMomentUnits } from '@shared/models/units/magnetic-moment';
import magneticPermeability, { MagneticPermeabilityUnits } from '@shared/models/units/magnetic-permeability';
import mass, { MassUnits } from '@shared/models/units/mass';
import massFraction, { MassFractionUnits } from '@shared/models/units/mass-fraction';
import molarConcentration, { MolarConcentrationUnits } from '@shared/models/units/molar-concentration';
import molarEnergy, { MolarEnergyUnits } from '@shared/models/units/molar-energy';
import molarHeatCapacity, { MolarHeatCapacityUnits } from '@shared/models/units/molar-heat-capacity';
import molarMass, { MolarMassUnits } from '@shared/models/units/molar-mass';
import numberConcentration, { NumberConcentrationUnits } from '@shared/models/units/number-concentration';
import partsPerMillion, { PartsPerMillionUnits } from '@shared/models/units/parts-per-million';
import power, { PowerUnits } from '@shared/models/units/power';
import powerDensity, { PowerDensityUnits } from '@shared/models/units/power-density';
import pressure, { PressureUnits } from '@shared/models/units/pressure';
import radiance, { RadianceUnits } from '@shared/models/units/radiance';
import radiantIntensity, { RadiantIntensityUnits } from '@shared/models/units/radiant-intensity';
import radiationDose, { RadiationDoseUnits } from '@shared/models/units/radiation-dose';
import radioactiveDecay, { RadioactiveDecayUnits } from '@shared/models/units/radioactive-decay';
import radioactivity, { RadioactivityUnits } from '@shared/models/units/radioactivity';
import radioactivityConcentration, {
  RadioactivityConcentrationUnits
} from '@shared/models/units/radioactivity-concentration';
import reciprocalLength, { ReciprocalLengthUnits } from '@shared/models/units/reciprocal-length';
import resistance, { ResistanceUnits } from '@shared/models/units/resistance';
import reynoldsNumber, { ReynoldsNumberUnits } from '@shared/models/units/reynolds-number';
import signalLevel, { SignalLevelUnits } from '@shared/models/units/signal-level';
import solidAngle, { SolidAngleUnits } from '@shared/models/units/solid-angle';
import specificEnergy, { SpecificEnergyUnits } from '@shared/models/units/specific-energy';
import specificHeatCapacity, { SpecificHeatCapacityUnits } from '@shared/models/units/specific-heat-capacity';
import specificHumidity, { SpecificHumidityUnits } from '@shared/models/units/specific-humidity';
import specificVolume, { SpecificVolumeUnits } from '@shared/models/units/specific-volume';
import speed, { SpeedUnits } from '@shared/models/units/speed';
import surfaceChargeDensity, { SurfaceChargeDensityUnits } from '@shared/models/units/surface-charge-density';
import surfaceTension, { SurfaceTensionUnits } from '@shared/models/units/surface-tension';
import temperature, { TemperatureUnits } from '@shared/models/units/temperature';
import thermalConductivity, { ThermalConductivityUnits } from '@shared/models/units/thermal-conductivity';
import time, { TimeUnits } from '@shared/models/units/time';
import torque, { TorqueUnits } from '@shared/models/units/torque';
import turbidity, { TurbidityUnits } from '@shared/models/units/turbidity';
import voltage, { VoltageUnits } from '@shared/models/units/voltage';
import volume, { VolumeUnits } from '@shared/models/units/volume';
import volumeFlow, { VolumeFlowUnits } from '@shared/models/units/volume-flow';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';

export type AllMeasuresUnits =
  | AbsorbedDoseRateUnits
  | AccelerationUnits
  | AcidityUnits
  | AirQualityIndexUnits
  | AmountOfSubstanceUnits
  | AngleUnits
  | AngularAccelerationUnits
  | AreaUnits
  | AreaDensityUnits
  | CapacitanceUnits
  | CatalyticActivityUnits
  | CatalyticConcentrationUnits
  | ChargeUnits
  | CurrentDensityUnits
  | DataTransferRateUnits
  | DensityUnits
  | DigitalUnits
  | DimensionRatioUnits
  | DynamicViscosityUnits
  | EarthquakeMagnitudeUnits
  | ElectricChargeDensityUnits
  | ElectricCurrentUnits
  | ElectricDipoleMomentUnits
  | ElectricFieldStrengthUnits
  | ElectricFluxUnits
  | ElectricPermittivityUnits
  | ElectricPolarizabilityUnits
  | ElectricalConductanceUnits
  | ElectricalConductivityUnits
  | EnergyUnits
  | EnergyDensityUnits
  | ForceUnits
  | FrequencyUnits
  | FuelEfficiencyUnits
  | HeatCapacityUnits
  | IlluminanceUnits
  | InductanceUnits
  | KinematicViscosityUnits
  | LengthUnits
  | LightExposureUnits
  | LinerChargeDensityUnits
  | LogarithmicRatioUnits
  | LuminousEfficacyUnits
  | LuminousFluxUnits
  | LuminousIntensityUnits
  | MagneticFieldGradientUnits
  | MagneticFluxUnits
  | MagneticFluxDensityUnits
  | MagneticMomentUnits
  | MagneticPermeabilityUnits
  | MassUnits
  | MassFractionUnits
  | MolarConcentrationUnits
  | MolarEnergyUnits
  | MolarHeatCapacityUnits
  | MolarMassUnits
  | NumberConcentrationUnits
  | PartsPerMillionUnits
  | PowerUnits
  | PowerDensityUnits
  | PressureUnits
  | RadianceUnits
  | RadiantIntensityUnits
  | RadiationDoseUnits
  | RadioactiveDecayUnits
  | RadioactivityUnits
  | RadioactivityConcentrationUnits
  | ReciprocalLengthUnits
  | ResistanceUnits
  | ReynoldsNumberUnits
  | SignalLevelUnits
  | SolidAngleUnits
  | SpecificEnergyUnits
  | SpecificHeatCapacityUnits
  | SpecificHumidityUnits
  | SpecificVolumeUnits
  | SpeedUnits
  | SurfaceChargeDensityUnits
  | SurfaceTensionUnits
  | TemperatureUnits
  | ThermalConductivityUnits
  | TimeUnits
  | TorqueUnits
  | TurbidityUnits
  | VoltageUnits
  | VolumeUnits
  | VolumeFlowUnits;

export type AllMeasures =
  | 'absorbed-dose-rate'
  | 'acceleration'
  | 'acidity'
  | 'air-quality-index'
  | 'amount-of-substance'
  | 'angle'
  | 'angular-acceleration'
  | 'area'
  | 'capacitance'
  | 'catalytic-activity'
  | 'catalytic-concentration'
  | 'charge'
  | 'current-density'
  | 'data-transfer-rate'
  | 'density'
  | 'digital'
  | 'dimension-ratio'
  | 'dynamic-viscosity'
  | 'earthquake-magnitude'
  | 'electric-charge-density'
  | 'electric-current'
  | 'electric-dipole-moment'
  | 'electric-field-strength'
  | 'electric-flux'
  | 'electric-permittivity'
  | 'electric-polarizability'
  | 'electrical-conductance'
  | 'electrical-conductivity'
  | 'energy'
  | 'energy-density'
  | 'force'
  | 'frequency'
  | 'fuel-efficiency'
  | 'heat-capacity'
  | 'illuminance'
  | 'inductance'
  | 'kinematic-viscosity'
  | 'length'
  | 'light-exposure'
  | 'linear-charge-density'
  | 'logarithmic-ratio'
  | 'luminous-efficacy'
  | 'luminous-flux'
  | 'luminous-intensity'
  | 'magnetic-field-gradient'
  | 'magnetic-flux'
  | 'magnetic-flux-density'
  | 'magnetic-moment'
  | 'magnetic-permeability'
  | 'mass'
  | 'mass-fraction'
  | 'molar-concentration'
  | 'molar-energy'
  | 'molar-heat-capacity'
  | 'molar-mass'
  | 'number-concentration'
  | 'parts-per-million'
  | 'power'
  | 'power-density'
  | 'pressure'
  | 'radiance'
  | 'radiant-intensity'
  | 'radiation-dose'
  | 'radioactive-decay'
  | 'radioactivity'
  | 'radioactivity-concentration'
  | 'reciprocal-length'
  | 'resistance'
  | 'reynolds-number'
  | 'signal-level'
  | 'solid-angle'
  | 'specific-energy'
  | 'specific-heat-capacity'
  | 'specific-humidity'
  | 'specific-volume'
  | 'surface-charge-density'
  | 'surface-tension'
  | 'speed'
  | 'temperature'
  | 'thermal-conductivity'
  | 'time'
  | 'torque'
  | 'turbidity'
  | 'voltage'
  | 'volume'
  | 'volume-flow';

const allMeasures: Record<
  AllMeasures,
  TbMeasure<AllMeasuresUnits>
> = Object.freeze({
  'absorbed-dose-rate': absorbedDoseRate,
  acceleration,
  acidity,
  'air-quality-index': airQualityIndex,
  'amount-of-substance': amountOfSubstance,
  angle,
  'angular-acceleration': angularAcceleration,
  area,
  'area-density': areaDensity,
  capacitance,
  'catalytic-activity': catalyticActivity,
  'catalytic-concentration': catalyticConcentration,
  charge,
  'current-density': currentDensity,
  'data-transfer-rate': dataTransferRate,
  density,
  digital,
  'dimension-ratio': dimensionRatio,
  'dynamic-viscosity': dynamicViscosity,
  'earthquake-magnitude': earthquakeMagnitude,
  'electric-charge-density': electricChargeDensity,
  'electric-current': electricCurrent,
  'electric-dipole-moment': electricDipoleMoment,
  'electric-field-strength': electricFieldStrength,
  'electric-flux': electricFlux,
  'electric-permittivity': electricPermittivity,
  'electric-polarizability': electricPolarizability,
  'electrical-conductance': electricalConductance,
  'electrical-conductivity': electricalConductivity,
  energy,
  'energy-density': energyDensity,
  force,
  frequency,
  'fuel-efficiency': fuelEfficiency,
  'heat-capacity': heatCapacity,
  illuminance,
  inductance,
  'kinematic-viscosity': kinematicViscosity,
  length,
  'light-exposure': lightExposure,
  'linear-charge-density': linerChargeDensity,
  'logarithmic-ratio': logarithmicRatio,
  'luminous-efficacy': luminousEfficacy,
  'luminous-flux': luminousFlux,
  'luminous-intensity': luminousIntensity,
  'number-concentration': numberConcentration,
  'magnetic-field-gradient': magneticFieldGradient,
  'magnetic-flux': magneticFlux,
  'magnetic-flux-density': magneticFluxDensity,
  'magnetic-moment': magneticMoment,
  'magnetic-permeability': magneticPermeability,
  mass,
  'mass-fraction': massFraction,
  'molar-concentration': molarConcentration,
  'molar-energy': molarEnergy,
  'molar-heat-capacity': molarHeatCapacity,
  'molar-mass': molarMass,
  'parts-per-million': partsPerMillion,
  power,
  'power-density': powerDensity,
  pressure,
  radiance,
  'radiant-intensity': radiantIntensity,
  'radiation-dose': radiationDose,
  'radioactive-decay': radioactiveDecay,
  radioactivity,
  'radioactivity-concentration': radioactivityConcentration,
  'reciprocal-length': reciprocalLength,
  resistance,
  'reynolds-number': reynoldsNumber,
  'signal-level': signalLevel,
  'solid-angle': solidAngle,
  'specific-energy': specificEnergy,
  'specific-heat-capacity': specificHeatCapacity,
  'specific-humidity': specificHumidity,
  'specific-volume': specificVolume,
  speed,
  'surface-charge-density': surfaceChargeDensity,
  'surface-tension': surfaceTension,
  temperature,
  'thermal-conductivity': thermalConductivity,
  time,
  torque,
  turbidity,
  voltage,
  volume,
  'volume-flow': volumeFlow,
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
  searchText: string;
}

export enum UnitSystem {
  METRIC = 'METRIC',
  IMPERIAL = 'IMPERIAL',
  HYBRID = 'HYBRID'
}

export const UnitSystems = Object.values(UnitSystem);

export interface Unit {
  name: string;
  tags?: string[];
  to_anchor: number;
  anchor_shift?: number;
  transform?: (value: number) => number;
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

export interface UnitCacheInfo {
  system: UnitSystem;
  measure: AllMeasures;
  unit: Unit;
  abbr: AllMeasuresUnits;
  searchText: string;
}

export type UnitCache = Map<AllMeasuresUnits | string, UnitCacheInfo>;

type Entries<T, S extends keyof T> = [S, T[keyof T]];

export class Converter {
  private readonly measureData: Record<AllMeasures, TbMeasure<AllMeasuresUnits>>;
  private unitCache: UnitCache;

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
    if (typeof origin.unit.transform === 'function') {
      result = origin.unit.transform(result);
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
    if (typeof destination.unit.transform === 'function') {
      result = destination.unit.transform(result);
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

  getUnit(abbr: AllMeasuresUnits | string): UnitCacheInfo | null {
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

        for (const abbr of Object.keys(units) as AllMeasuresUnits[]) {
          results.push(this.describe(abbr));
        }
      }
    }
    return results;
  }

  unitsGroupByMeasure(measureName?: AllMeasures, unitSystem?: UnitSystem): UnitInfoGroupByMeasure<AllMeasures> {
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

        for (const abbr of Object.keys(units) as AllMeasuresUnits[]) {
          results[name].push(this.describe(abbr));
        }
      }
    }
    return results;
  }

  private describeUnit(unit: UnitCacheInfo): UnitInfo {
    return {
      abbr: unit.abbr,
      measure: unit.measure,
      system: unit.system,
      name: unit.unit.name,
      tags: unit.unit.tags,
      searchText: unit.searchText
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
        const measureNameTranslation = translate.instant('unit.measures.' + measureName);
        unit.tags = unit.tags ?? [];
        unit.tags.push(testAbbr, unit.name, measureNameTranslation);
        unitCache.set(testAbbr, {
          measure: measureName,
          system: systemName,
          abbr: testAbbr,
          searchText: unit.tags.join(';').toUpperCase(),
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

export const getSourceTbUnitSymbol = (value: TbUnit | UnitInfo | null): string => {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'string') {
    return value;
  }
  if ('abbr' in value) {
    return value.abbr;
  }
  return value.from;
}

export const isNotEmptyTbUnits = (unit: any): boolean => {
  if (typeof unit === 'object' && unit !== null && isNotEmptyStr(unit?.from)) {
    return true;
  }
  return isNotEmptyStr(unit);
}

export const isTbUnitMapping = (unit: any): boolean => {
  if (typeof unit !== 'object' || unit === null) return false;
  return isNotEmptyStr(unit.from);
};

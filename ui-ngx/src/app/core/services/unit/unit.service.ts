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
  TbUnitConvertor,
  UnitDescription,
  UnitDescriptionGroupByMeasure,
  UnitSystem
} from '@shared/models/unit.models';
import { isNotEmptyStr } from '@core/utils';
import { configureMeasurements, Converter } from '@core/services/unit/converter-unit';
import allMeasures, { AllMeasures, AllMeasuresUnits } from '@core/services/unit/definitions/all';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Injectable({
  providedIn: 'root'
})
export class UnitService {

  private currentUnitSystem: UnitSystem = UnitSystem.METRIC;
  private converter: Converter<AllMeasures, AllMeasuresUnits>;

  constructor(private store: Store<AppState>,
              private translate: TranslateService) {
    this.translate.onLangChange.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.converter = configureMeasurements<AllMeasures, AllMeasuresUnits>(allMeasures, this.translate);
      console.warn(this.converter?.list());
      console.warn(this.converter?.list('temperature'));
      console.warn(this.converter?.list('temperature', UnitSystem.METRIC));
      console.warn(this.converter?.list(null, UnitSystem.IMPERIAL));
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

  getUnits(measure?: AllMeasures, unitSystem?: UnitSystem): UnitDescription[] {
    return this.converter?.list(measure, unitSystem);
  }

  getUnitsGroupByMeasure(measure?: AllMeasures, unitSystem?: UnitSystem): UnitDescriptionGroupByMeasure<AllMeasures> {
    return this.converter?.listGroupByMeasure(measure, unitSystem);
  }

  getUnitDescription(abbr: AllMeasuresUnits | string): UnitDescription {
    return this.converter.describe(abbr);
  }

  getDefaultUnit(measure: AllMeasures, unitSystem: UnitSystem): AllMeasuresUnits {
    return this.converter.getDefaultUnit(measure, unitSystem);
  }

  geUnitConvertor(from: string, to: string): TbUnitConvertor {
    return this.converter.convertor(from, to);
  }

  convertValue(value: number, from: string, to: string): number {
    return this.converter.convert(value, from, to);
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

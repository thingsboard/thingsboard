///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { PerimeterType, perimeterTypeTranslations, RangeUnit, rangeUnitTranslations } from '../rule-node-config.models';

@Component({
    selector: 'tb-filter-node-gps-geofencing-config',
    templateUrl: './gps-geo-filter-config.component.html',
    styleUrls: ['./gps-geo-filter-config.component.scss'],
    standalone: false
})
export class GpsGeoFilterConfigComponent extends RuleNodeConfigurationComponent {

  geoFilterConfigForm: FormGroup;

  perimeterType = PerimeterType;
  perimeterTypes: Array<PerimeterType> = Object.values(PerimeterType);
  perimeterTypeTranslationMap = perimeterTypeTranslations;

  rangeUnits: Array<RangeUnit> = Object.values(RangeUnit);
  rangeUnitTranslationMap = rangeUnitTranslations;

  public defaultPaddingEnable = true;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.geoFilterConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      latitudeKeyName: isDefinedAndNotNull(configuration?.latitudeKeyName) ? configuration.latitudeKeyName : null,
      longitudeKeyName: isDefinedAndNotNull(configuration?.longitudeKeyName) ? configuration.longitudeKeyName : null,
      perimeterType: isDefinedAndNotNull(configuration?.perimeterType) ? configuration.perimeterType : null,
      fetchPerimeterInfoFromMessageMetadata: isDefinedAndNotNull(configuration?.fetchPerimeterInfoFromMessageMetadata) ?
        configuration.fetchPerimeterInfoFromMessageMetadata : false,
      perimeterKeyName: isDefinedAndNotNull(configuration?.perimeterKeyName) ? configuration.perimeterKeyName : null,
      centerLatitude: isDefinedAndNotNull(configuration?.centerLatitude) ? configuration.centerLatitude : null,
      centerLongitude: isDefinedAndNotNull(configuration?.centerLongitude) ? configuration.centerLongitude : null,
      range: isDefinedAndNotNull(configuration?.range) ? configuration.range : null,
      rangeUnit: isDefinedAndNotNull(configuration?.rangeUnit) ? configuration.rangeUnit : null,
      polygonsDefinition: isDefinedAndNotNull(configuration?.polygonsDefinition) ? configuration.polygonsDefinition : null
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.geoFilterConfigForm = this.fb.group({
      latitudeKeyName: [configuration.latitudeKeyName, [Validators.required]],
      longitudeKeyName: [configuration.longitudeKeyName, [Validators.required]],
      perimeterType: [configuration.perimeterType, [Validators.required]],
      fetchPerimeterInfoFromMessageMetadata: [configuration.fetchPerimeterInfoFromMessageMetadata, []],
      perimeterKeyName: [configuration.perimeterKeyName, []],
      centerLatitude: [configuration.centerLatitude, []],
      centerLongitude: [configuration.centerLongitude, []],
      range: [configuration.range, []],
      rangeUnit: [configuration.rangeUnit, []],
      polygonsDefinition: [configuration.polygonsDefinition, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['fetchPerimeterInfoFromMessageMetadata', 'perimeterType'];
  }

  protected updateValidators(emitEvent: boolean) {
    const fetchPerimeterInfoFromMessageMetadata: boolean = this.geoFilterConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value;
    const perimeterType: PerimeterType = this.geoFilterConfigForm.get('perimeterType').value;
    if (fetchPerimeterInfoFromMessageMetadata) {
      this.geoFilterConfigForm.get('perimeterKeyName').setValidators([Validators.required]);
    } else {
      this.geoFilterConfigForm.get('perimeterKeyName').setValidators([]);
    }
    if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.CIRCLE) {
      this.geoFilterConfigForm.get('centerLatitude').setValidators([Validators.required,
        Validators.min(-90), Validators.max(90)]);
      this.geoFilterConfigForm.get('centerLongitude').setValidators([Validators.required,
        Validators.min(-180), Validators.max(180)]);
      this.geoFilterConfigForm.get('range').setValidators([Validators.required, Validators.min(0)]);
      this.geoFilterConfigForm.get('rangeUnit').setValidators([Validators.required]);

      this.defaultPaddingEnable = false;
    } else {
      this.geoFilterConfigForm.get('centerLatitude').setValidators([]);
      this.geoFilterConfigForm.get('centerLongitude').setValidators([]);
      this.geoFilterConfigForm.get('range').setValidators([]);
      this.geoFilterConfigForm.get('rangeUnit').setValidators([]);

      this.defaultPaddingEnable = true;
    }
    if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.POLYGON) {
      this.geoFilterConfigForm.get('polygonsDefinition').setValidators([Validators.required]);
    } else {
      this.geoFilterConfigForm.get('polygonsDefinition').setValidators([]);
    }
    this.geoFilterConfigForm.get('perimeterKeyName').updateValueAndValidity({emitEvent});
    this.geoFilterConfigForm.get('centerLatitude').updateValueAndValidity({emitEvent});
    this.geoFilterConfigForm.get('centerLongitude').updateValueAndValidity({emitEvent});
    this.geoFilterConfigForm.get('range').updateValueAndValidity({emitEvent});
    this.geoFilterConfigForm.get('rangeUnit').updateValueAndValidity({emitEvent});
    this.geoFilterConfigForm.get('polygonsDefinition').updateValueAndValidity({emitEvent});
  }
}

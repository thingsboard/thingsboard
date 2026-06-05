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
import { deepTrim, isDefinedAndNotNull, isObject } from '@core/public-api';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { aggregationTranslations, AggregationType } from '@app/shared/models/time/time.models';
import {
  deduplicationStrategiesHintTranslations,
  deduplicationStrategiesTranslations,
  FetchMode,
  SamplingOrder,
  samplingOrderTranslations,
  TimeUnit,
  timeUnitTranslations
} from '../rule-node-config.models';

@Component({
    selector: 'tb-enrichment-node-get-telemetry-from-database',
    templateUrl: './get-telemetry-from-database-config.component.html',
    styleUrls: ['./get-telemetry-from-database-config.component.scss'],
    standalone: false
})
export class GetTelemetryFromDatabaseConfigComponent extends RuleNodeConfigurationComponent {

  getTelemetryFromDatabaseConfigForm: FormGroup;

  aggregationTypes = AggregationType;
  aggregations: Array<AggregationType> = Object.values(AggregationType);
  aggregationTypesTranslations = aggregationTranslations;

  fetchMode = FetchMode;

  samplingOrders: Array<SamplingOrder> = Object.values(SamplingOrder);
  samplingOrdersTranslate = samplingOrderTranslations;

  timeUnits: Array<TimeUnit> = Object.values(TimeUnit);
  timeUnitsTranslationMap = timeUnitTranslations;

  public deduplicationStrategiesHintTranslations = deduplicationStrategiesHintTranslations;

  headerOptions = [];


  timeUnitMap = {
    [TimeUnit.MILLISECONDS]: 1,
    [TimeUnit.SECONDS]: 1000,
    [TimeUnit.MINUTES]: 60000,
    [TimeUnit.HOURS]: 3600000,
    [TimeUnit.DAYS]: 86400000,
  };

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
    for (const key of deduplicationStrategiesTranslations.keys()) {
      this.headerOptions.push({
        value: key,
        name: this.translate.instant(deduplicationStrategiesTranslations.get(key))
      });
    }
  }

  protected configForm(): FormGroup {
    return this.getTelemetryFromDatabaseConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.getTelemetryFromDatabaseConfigForm = this.fb.group({
      latestTsKeyNames: [configuration.latestTsKeyNames, [Validators.required]],
      aggregation: [configuration.aggregation, [Validators.required]],
      fetchMode: [configuration.fetchMode, [Validators.required]],
      orderBy: [configuration.orderBy, []],
      limit: [configuration.limit, []],
      useMetadataIntervalPatterns: [configuration.useMetadataIntervalPatterns, []],
      interval: this.fb.group({
        startInterval: [configuration.interval.startInterval, []],
        startIntervalTimeUnit: [configuration.interval.startIntervalTimeUnit, []],
        endInterval: [configuration.interval.endInterval, []],
        endIntervalTimeUnit: [configuration.interval.endIntervalTimeUnit, []],
      }),
      startIntervalPattern: [configuration.startIntervalPattern, []],
      endIntervalPattern: [configuration.endIntervalPattern, []],
    });
  }


  private intervalValidator = () => (control: AbstractControl): ValidationErrors | null => {
    if (control.get('startInterval').value * this.timeUnitMap[control.get('startIntervalTimeUnit').value] <=
      control.get('endInterval').value * this.timeUnitMap[control.get('endIntervalTimeUnit').value]) {
      return {intervalError: true};
    } else {
      return null;
    }
  };


  protected validatorTriggers(): string[] {
    return ['fetchMode', 'useMetadataIntervalPatterns'];
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.startInterval = configuration.interval.startInterval;
    configuration.startIntervalTimeUnit = configuration.interval.startIntervalTimeUnit;
    configuration.endInterval = configuration.interval.endInterval;
    configuration.endIntervalTimeUnit = configuration.interval.endIntervalTimeUnit;
    delete configuration.interval;
    return deepTrim(configuration);
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (isObject(configuration)) {
      configuration.interval = {
        startInterval: configuration.startInterval,
        startIntervalTimeUnit: configuration.startIntervalTimeUnit,
        endInterval: configuration.endInterval,
        endIntervalTimeUnit: configuration.endIntervalTimeUnit
      };
    }

    return {
      latestTsKeyNames: isDefinedAndNotNull(configuration?.latestTsKeyNames) ? configuration.latestTsKeyNames : null,
      aggregation: isDefinedAndNotNull(configuration?.aggregation) ? configuration.aggregation : AggregationType.NONE,
      fetchMode: isDefinedAndNotNull(configuration?.fetchMode) ? configuration.fetchMode : FetchMode.FIRST,
      orderBy: isDefinedAndNotNull(configuration?.orderBy) ? configuration.orderBy : SamplingOrder.ASC,
      limit: isDefinedAndNotNull(configuration?.limit) ? configuration.limit : 1000,
      useMetadataIntervalPatterns: isDefinedAndNotNull(configuration?.useMetadataIntervalPatterns) ?
        configuration.useMetadataIntervalPatterns : false,
      interval: {
        startInterval: isDefinedAndNotNull(configuration?.interval?.startInterval) ? configuration.interval.startInterval : 2,
        startIntervalTimeUnit: isDefinedAndNotNull(configuration?.interval?.startIntervalTimeUnit) ?
          configuration.interval.startIntervalTimeUnit : TimeUnit.MINUTES,
        endInterval: isDefinedAndNotNull(configuration?.interval?.endInterval) ? configuration.interval.endInterval : 1,
        endIntervalTimeUnit: isDefinedAndNotNull(configuration?.interval?.endIntervalTimeUnit) ?
          configuration.interval.endIntervalTimeUnit : TimeUnit.MINUTES,
      },
      startIntervalPattern: isDefinedAndNotNull(configuration?.startIntervalPattern) ? configuration.startIntervalPattern : null,
      endIntervalPattern: isDefinedAndNotNull(configuration?.endIntervalPattern) ? configuration.endIntervalPattern : null
    };
  }

  protected updateValidators(emitEvent: boolean) {
    const fetchMode: FetchMode = this.getTelemetryFromDatabaseConfigForm.get('fetchMode').value;
    const useMetadataIntervalPatterns: boolean = this.getTelemetryFromDatabaseConfigForm.get('useMetadataIntervalPatterns').value;
    if (fetchMode && fetchMode === FetchMode.ALL) {
      this.getTelemetryFromDatabaseConfigForm.get('aggregation').setValidators([Validators.required]);
      this.getTelemetryFromDatabaseConfigForm.get('orderBy').setValidators([Validators.required]);
      this.getTelemetryFromDatabaseConfigForm.get('limit').setValidators([Validators.required, Validators.min(2), Validators.max(1000)]);
    } else {
      this.getTelemetryFromDatabaseConfigForm.get('aggregation').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('orderBy').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('limit').setValidators([]);
    }
    if (useMetadataIntervalPatterns) {
      this.getTelemetryFromDatabaseConfigForm.get('interval.startInterval').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('interval.startIntervalTimeUnit').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('interval.endInterval').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('interval.endIntervalTimeUnit').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('interval').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').setValidators([Validators.required,
        Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]);
      this.getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').setValidators([Validators.required,
        Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]);
    } else {
      this.getTelemetryFromDatabaseConfigForm.get('interval.startInterval').setValidators([Validators.required,
        Validators.min(1), Validators.max(2147483647)]);
      this.getTelemetryFromDatabaseConfigForm.get('interval.startIntervalTimeUnit').setValidators([Validators.required]);
      this.getTelemetryFromDatabaseConfigForm.get('interval.endInterval').setValidators([Validators.required,
        Validators.min(1), Validators.max(2147483647)]);
      this.getTelemetryFromDatabaseConfigForm.get('interval.endIntervalTimeUnit').setValidators([Validators.required]);
      this.getTelemetryFromDatabaseConfigForm.get('interval').setValidators([this.intervalValidator()]);
      this.getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').setValidators([]);
      this.getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').setValidators([]);
    }
    this.getTelemetryFromDatabaseConfigForm.get('aggregation').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('orderBy').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('limit').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('interval.startInterval').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('interval.startIntervalTimeUnit').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('interval.endInterval').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('interval.endIntervalTimeUnit').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('interval').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').updateValueAndValidity({emitEvent});
    this.getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').updateValueAndValidity({emitEvent});
  }

  public defaultPaddingEnable() {
    return this.getTelemetryFromDatabaseConfigForm.get('fetchMode').value === FetchMode.ALL &&
      this.getTelemetryFromDatabaseConfigForm.get('aggregation').value === AggregationType.NONE;
  }
}

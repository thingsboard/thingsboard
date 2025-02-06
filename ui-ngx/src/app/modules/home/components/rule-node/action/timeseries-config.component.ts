///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import {
  defaultAdvancedPersistenceStrategy,
  maxDeduplicateTimeSecs,
  PersistenceSettings,
  PersistenceSettingsForm,
  PersistenceType,
  PersistenceTypeTranslationMap,
  TimeseriesNodeConfiguration,
  TimeseriesNodeConfigurationForm
} from '@home/components/rule-node/action/timeseries-config.models';

@Component({
  selector: 'tb-action-node-timeseries-config',
  templateUrl: './timeseries-config.component.html',
  styleUrls: []
})
export class TimeseriesConfigComponent extends RuleNodeConfigurationComponent {

  timeseriesConfigForm: FormGroup;

  PersistenceType = PersistenceType;
  persistenceStrategies = [PersistenceType.ON_EVERY_MESSAGE, PersistenceType.DEDUPLICATE, PersistenceType.WEBSOCKETS_ONLY];
  PersistenceTypeTranslationMap = PersistenceTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.timeseriesConfigForm;
  }

  protected validatorTriggers(): string[] {
    return ['persistenceSettings.isAdvanced', 'persistenceSettings.type'];
  }

  protected prepareInputConfig(config: TimeseriesNodeConfiguration): TimeseriesNodeConfigurationForm {
    let persistenceSettings: PersistenceSettingsForm;
    if (config?.persistenceSettings) {
      const isAdvanced = config?.persistenceSettings?.type === PersistenceType.ADVANCED;
      persistenceSettings = {
        type: isAdvanced ? PersistenceType.ON_EVERY_MESSAGE : config.persistenceSettings.type,
        isAdvanced: isAdvanced,
        deduplicationIntervalSecs: config.persistenceSettings?.deduplicationIntervalSecs ?? 60,
        advanced: isAdvanced ? config.persistenceSettings : defaultAdvancedPersistenceStrategy
      }
    } else {
      persistenceSettings = {
        type: PersistenceType.ON_EVERY_MESSAGE,
        isAdvanced: false,
        deduplicationIntervalSecs: 60,
        advanced: defaultAdvancedPersistenceStrategy
      };
    }
    return {
      ...config,
      persistenceSettings: persistenceSettings
    }
  }

  protected prepareOutputConfig(config: TimeseriesNodeConfigurationForm): TimeseriesNodeConfiguration {
    let persistenceSettings: PersistenceSettings;
    if (config.persistenceSettings.isAdvanced) {
      persistenceSettings = {
        ...config.persistenceSettings.advanced,
        type: PersistenceType.ADVANCED
      };
    } else {
      persistenceSettings = {
        type: config.persistenceSettings.type,
        deduplicationIntervalSecs: config.persistenceSettings?.deduplicationIntervalSecs
      };
    }
    return {
      ...config,
      persistenceSettings
    };
  }

  protected onConfigurationSet(config: TimeseriesNodeConfigurationForm) {
    this.timeseriesConfigForm = this.fb.group({
      persistenceSettings: this.fb.group({
        isAdvanced: [config?.persistenceSettings?.isAdvanced ?? false],
        type: [config?.persistenceSettings?.type ?? PersistenceType.ON_EVERY_MESSAGE],
        deduplicationIntervalSecs: [
          {value: config?.persistenceSettings?.deduplicationIntervalSecs ?? 60, disabled: true},
          [Validators.required, Validators.max(maxDeduplicateTimeSecs)]
        ],
        advanced: [{value: null, disabled: true}]
      }),
      defaultTTL: [config?.defaultTTL ?? null, [Validators.required, Validators.min(0)]],
      useServerTs: [config?.useServerTs ?? false]
    });
  }

  protected updateValidators(emitEvent: boolean, _trigger?: string) {
    const persistenceForm = this.timeseriesConfigForm.get('persistenceSettings') as FormGroup;
    const isAdvanced: boolean = persistenceForm.get('isAdvanced').value;
    const type: PersistenceType = persistenceForm.get('type').value;
    if (!isAdvanced && type === PersistenceType.DEDUPLICATE) {
      persistenceForm.get('deduplicationIntervalSecs').enable({emitEvent});
    } else {
      persistenceForm.get('deduplicationIntervalSecs').disable({emitEvent});
    }
    if (isAdvanced) {
      persistenceForm.get('advanced').enable({emitEvent});
    } else {
      persistenceForm.get('advanced').disable({emitEvent});
    }
  }
}

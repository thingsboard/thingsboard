// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import {
  defaultAdvancedProcessingStrategy,
  maxDeduplicateTimeSecs,
  ProcessingSettings,
  ProcessingSettingsForm,
  ProcessingType,
  ProcessingTypeTranslationMap,
  TimeseriesNodeConfiguration,
  TimeseriesNodeConfigurationForm
} from '@home/components/rule-node/action/timeseries-config.models';

@Component({
    selector: 'tb-action-node-timeseries-config',
    templateUrl: './timeseries-config.component.html',
    styleUrls: [],
    standalone: false
})
export class TimeseriesConfigComponent extends RuleNodeConfigurationComponent {

  timeseriesConfigForm: FormGroup;

  ProcessingType = ProcessingType;
  processingStrategies = [ProcessingType.ON_EVERY_MESSAGE, ProcessingType.DEDUPLICATE, ProcessingType.WEBSOCKETS_ONLY];
  ProcessingTypeTranslationMap = ProcessingTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.timeseriesConfigForm;
  }

  protected validatorTriggers(): string[] {
    return ['processingSettings.isAdvanced', 'processingSettings.type'];
  }

  protected prepareInputConfig(config: TimeseriesNodeConfiguration): TimeseriesNodeConfigurationForm {
    let processingSettings: ProcessingSettingsForm;
    if (config?.processingSettings) {
      const isAdvanced = config?.processingSettings?.type === ProcessingType.ADVANCED;
      processingSettings = {
        type: isAdvanced ? ProcessingType.ON_EVERY_MESSAGE : config.processingSettings.type,
        isAdvanced: isAdvanced,
        deduplicationIntervalSecs: config.processingSettings?.deduplicationIntervalSecs ?? 60,
        advanced: isAdvanced ? config.processingSettings : defaultAdvancedProcessingStrategy
      }
    } else {
      processingSettings = {
        type: ProcessingType.ON_EVERY_MESSAGE,
        isAdvanced: false,
        deduplicationIntervalSecs: 60,
        advanced: defaultAdvancedProcessingStrategy
      };
    }
    return {
      ...config,
      processingSettings: processingSettings
    }
  }

  protected prepareOutputConfig(config: TimeseriesNodeConfigurationForm): TimeseriesNodeConfiguration {
    let processingSettings: ProcessingSettings;
    if (config.processingSettings.isAdvanced) {
      processingSettings = {
        ...config.processingSettings.advanced,
        type: ProcessingType.ADVANCED
      };
    } else {
      processingSettings = {
        type: config.processingSettings.type,
        deduplicationIntervalSecs: config.processingSettings?.deduplicationIntervalSecs
      };
    }
    return {
      ...config,
      processingSettings
    };
  }

  protected onConfigurationSet(config: TimeseriesNodeConfigurationForm) {
    this.timeseriesConfigForm = this.fb.group({
      processingSettings: this.fb.group({
        isAdvanced: [config?.processingSettings?.isAdvanced ?? false],
        type: [config?.processingSettings?.type ?? ProcessingType.ON_EVERY_MESSAGE],
        deduplicationIntervalSecs: [
          {value: config?.processingSettings?.deduplicationIntervalSecs ?? 60, disabled: true},
          [Validators.required, Validators.max(maxDeduplicateTimeSecs)]
        ],
        advanced: [{value: null, disabled: true}]
      }),
      defaultTTL: [config?.defaultTTL ?? null, [Validators.required, Validators.min(0)]],
      useServerTs: [config?.useServerTs ?? false]
    });
  }

  protected updateValidators(emitEvent: boolean, _trigger?: string) {
    const processingForm = this.timeseriesConfigForm.get('processingSettings') as FormGroup;
    const isAdvanced: boolean = processingForm.get('isAdvanced').value;
    const type: ProcessingType = processingForm.get('type').value;
    if (!isAdvanced && type === ProcessingType.DEDUPLICATE) {
      processingForm.get('deduplicationIntervalSecs').enable({emitEvent});
    } else {
      processingForm.get('deduplicationIntervalSecs').disable({emitEvent});
    }
    if (isAdvanced) {
      processingForm.get('advanced').enable({emitEvent});
    } else {
      processingForm.get('advanced').disable({emitEvent});
    }
  }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { deepTrim, isDefinedAndNotNull } from '@app/core/utils';

@Component({
    selector: 'tb-enrichment-node-calculate-delta-config',
    templateUrl: './calculate-delta-config.component.html',
    standalone: false
})
export class CalculateDeltaConfigComponent extends RuleNodeConfigurationComponent {

  calculateDeltaConfigForm: FormGroup;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.calculateDeltaConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.calculateDeltaConfigForm = this.fb.group({
      inputValueKey: [configuration.inputValueKey, [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      outputValueKey: [configuration.outputValueKey, [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      useCache: [configuration.useCache, []],
      addPeriodBetweenMsgs: [configuration.addPeriodBetweenMsgs, []],
      periodValueKey: [configuration.periodValueKey, []],
      round: [configuration.round, [Validators.min(0), Validators.max(15)]],
      tellFailureIfDeltaIsNegative: [configuration.tellFailureIfDeltaIsNegative, []],
      excludeZeroDeltas: [configuration.excludeZeroDeltas, []]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      inputValueKey: isDefinedAndNotNull(configuration?.inputValueKey) ? configuration.inputValueKey : null,
      outputValueKey: isDefinedAndNotNull(configuration?.outputValueKey) ? configuration.outputValueKey : null,
      useCache: isDefinedAndNotNull(configuration?.useCache) ? configuration.useCache : true,
      addPeriodBetweenMsgs: isDefinedAndNotNull(configuration?.addPeriodBetweenMsgs) ? configuration.addPeriodBetweenMsgs : false,
      periodValueKey: isDefinedAndNotNull(configuration?.periodValueKey) ? configuration.periodValueKey : null,
      round: isDefinedAndNotNull(configuration?.round) ? configuration.round : null,
      tellFailureIfDeltaIsNegative: isDefinedAndNotNull(configuration?.tellFailureIfDeltaIsNegative) ?
        configuration.tellFailureIfDeltaIsNegative : true,
      excludeZeroDeltas: isDefinedAndNotNull(configuration?.excludeZeroDeltas) ? configuration.excludeZeroDeltas : false
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return deepTrim(configuration);
  }

  protected updateValidators(emitEvent: boolean) {
    const addPeriodBetweenMsgs: boolean = this.calculateDeltaConfigForm.get('addPeriodBetweenMsgs').value;
    if (addPeriodBetweenMsgs) {
      this.calculateDeltaConfigForm.get('periodValueKey').setValidators([Validators.required]);
    } else {
      this.calculateDeltaConfigForm.get('periodValueKey').setValidators([]);
    }
    this.calculateDeltaConfigForm.get('periodValueKey').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['addPeriodBetweenMsgs'];
  }
}

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
import { FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
  selector: 'tb-filter-node-check-message-config',
  templateUrl: './check-message-config.component.html',
  styleUrls: []
})
export class CheckMessageConfigComponent extends RuleNodeConfigurationComponent {

  checkMessageConfigForm: FormGroup;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.checkMessageConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      messageNames: isDefinedAndNotNull(configuration?.messageNames) ? configuration.messageNames : [],
      metadataNames: isDefinedAndNotNull(configuration?.metadataNames) ? configuration.metadataNames : [],
      checkAllKeys: isDefinedAndNotNull(configuration?.checkAllKeys) ? configuration.checkAllKeys : false
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      messageNames: isDefinedAndNotNull(configuration?.messageNames) ? configuration.messageNames : [],
      metadataNames: isDefinedAndNotNull(configuration?.metadataNames) ? configuration.metadataNames : [],
      checkAllKeys: configuration.checkAllKeys
    };
  }


  private atLeastOne(validator: ValidatorFn, controls: string[] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!controls) {
        controls = Object.keys(group.controls);
      }
      const hasAtLeastOne = group?.controls && controls.some(k => !validator(group.controls[k]));

      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.checkMessageConfigForm = this.fb.group({
      messageNames: [configuration.messageNames, []],
      metadataNames: [configuration.metadataNames, []],
      checkAllKeys: [configuration.checkAllKeys, []]
    }, {validators: this.atLeastOne(Validators.required, ['messageNames', 'metadataNames'])});
  }

  get touchedValidationControl(): boolean {
    return ['messageNames', 'metadataNames'].some(name => this.checkMessageConfigForm.get(name).touched);
  }
}

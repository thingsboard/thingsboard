// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { deepTrim, isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { allowedOriginatorFields, FetchTo, SvMapOption } from '@home/components/rule-node/rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-enrichment-node-originator-fields-config',
    templateUrl: './originator-fields-config.component.html',
    standalone: false
})
export class OriginatorFieldsConfigComponent extends RuleNodeConfigurationComponent {

  originatorFieldsConfigForm: FormGroup;
  public originatorFields: SvMapOption[] = [];

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const field of allowedOriginatorFields) {
      this.originatorFields.push({
        value: field.value,
        name: this.translate.instant(field.name)
      });
    }
  }

  protected configForm(): FormGroup {
    return this.originatorFieldsConfigForm;
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return deepTrim(configuration);
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      dataMapping: isDefinedAndNotNull(configuration?.dataMapping) ? configuration.dataMapping : null,
      ignoreNullStrings: isDefinedAndNotNull(configuration?.ignoreNullStrings) ? configuration.ignoreNullStrings : null,
      fetchTo: isDefinedAndNotNull(configuration?.fetchTo) ? configuration.fetchTo : FetchTo.METADATA
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.originatorFieldsConfigForm = this.fb.group({
      dataMapping: [configuration.dataMapping, [Validators.required]],
      ignoreNullStrings: [configuration.ignoreNullStrings, []],
      fetchTo: [configuration.fetchTo, []]
    });
  }
}

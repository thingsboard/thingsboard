// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull, isObject, } from '@core/public-api';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { FetchTo } from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-enrichment-node-originator-attributes-config',
    templateUrl: './originator-attributes-config.component.html',
    styleUrls: [],
    standalone: false
})
export class OriginatorAttributesConfigComponent extends RuleNodeConfigurationComponent {

  originatorAttributesConfigForm: FormGroup;

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.originatorAttributesConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.originatorAttributesConfigForm = this.fb.group({
      tellFailureIfAbsent: [configuration.tellFailureIfAbsent, []],
      fetchTo: [configuration.fetchTo, []],
      attributesControl: [configuration.attributesControl, []]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (isObject(configuration)) {
      configuration.attributesControl = {
        clientAttributeNames: isDefinedAndNotNull(configuration?.clientAttributeNames) ? configuration.clientAttributeNames : [],
        latestTsKeyNames: isDefinedAndNotNull(configuration?.latestTsKeyNames) ? configuration.latestTsKeyNames : [],
        serverAttributeNames: isDefinedAndNotNull(configuration?.serverAttributeNames) ? configuration.serverAttributeNames : [],
        sharedAttributeNames: isDefinedAndNotNull(configuration?.sharedAttributeNames) ? configuration.sharedAttributeNames : [],
        getLatestValueWithTs: isDefinedAndNotNull(configuration?.getLatestValueWithTs) ? configuration.getLatestValueWithTs : false
      };
    }

    return {
      fetchTo: isDefinedAndNotNull(configuration?.fetchTo) ? configuration.fetchTo : FetchTo.METADATA,
      tellFailureIfAbsent: isDefinedAndNotNull(configuration?.tellFailureIfAbsent) ? configuration.tellFailureIfAbsent : false,
      attributesControl: isDefinedAndNotNull(configuration?.attributesControl) ? configuration.attributesControl : null
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    for (const key of Object.keys(configuration.attributesControl)) {
      configuration[key] = configuration.attributesControl[key];
    }
    delete configuration.attributesControl;
    return configuration;
  }
}

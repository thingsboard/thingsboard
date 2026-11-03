// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { FetchTo } from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-enrichment-node-fetch-device-credentials-config',
    templateUrl: './fetch-device-credentials-config.component.html',
    standalone: false
})

export class FetchDeviceCredentialsConfigComponent extends RuleNodeConfigurationComponent {

  fetchDeviceCredentialsConfigForm: FormGroup;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.fetchDeviceCredentialsConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      fetchTo: isDefinedAndNotNull(configuration?.fetchTo) ? configuration.fetchTo : FetchTo.METADATA
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.fetchDeviceCredentialsConfigForm = this.fb.group({
      fetchTo: [configuration.fetchTo, []]
    });
  }
}

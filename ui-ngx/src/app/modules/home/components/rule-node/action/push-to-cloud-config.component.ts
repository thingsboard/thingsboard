// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { AttributeScope, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';

@Component({
    selector: 'tb-action-node-push-to-cloud-config',
    templateUrl: './push-to-cloud-config.component.html',
    styleUrls: [],
    standalone: false
})
export class PushToCloudConfigComponent extends RuleNodeConfigurationComponent {

  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationsMap = telemetryTypeTranslations;

  pushToCloudConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.pushToCloudConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.pushToCloudConfigForm = this.fb.group({
      scope: [configuration ? configuration.scope : null, [Validators.required]]
    });
  }

}

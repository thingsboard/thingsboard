// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { AttributeScope, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';

@Component({
    selector: 'tb-action-node-push-to-edge-config',
    templateUrl: './push-to-edge-config.component.html',
    styleUrls: [],
    standalone: false
})
export class PushToEdgeConfigComponent extends RuleNodeConfigurationComponent {

  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationsMap = telemetryTypeTranslations;

  pushToEdgeConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.pushToEdgeConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.pushToEdgeConfigForm = this.fb.group({
      scope: [configuration ? configuration.scope : null, [Validators.required]]
    });
  }

}

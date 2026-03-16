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

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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { AttributeScope, telemetryTypeTranslations } from '@app/shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-action-node-attributes-config',
  templateUrl: './attributes-config.component.html',
  styleUrls: []
})
export class AttributesConfigComponent extends RuleNodeConfigurationComponent {

  attributeScopeMap = AttributeScope;
  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationsMap = telemetryTypeTranslations;

  attributesConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.attributesConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.attributesConfigForm = this.fb.group({
      scope: [configuration ? configuration.scope : null, [Validators.required]],
      notifyDevice: [configuration ? configuration.notifyDevice : true, []],
      sendAttributesUpdatedNotification: [configuration ? configuration.sendAttributesUpdatedNotification : false, []],
      updateAttributesOnlyOnValueChange: [configuration ? configuration.updateAttributesOnlyOnValueChange : false, []]
    });

    this.attributesConfigForm.get('scope').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      if (value !== AttributeScope.SHARED_SCOPE) {
        this.attributesConfigForm.get('notifyDevice').patchValue(false, {emitEvent: false});
      }
      if (value === AttributeScope.CLIENT_SCOPE) {
        this.attributesConfigForm.get('sendAttributesUpdatedNotification').patchValue(false, {emitEvent: false});
      }
      this.attributesConfigForm.get('updateAttributesOnlyOnValueChange').patchValue(false, {emitEvent: false});
    });
  }

}

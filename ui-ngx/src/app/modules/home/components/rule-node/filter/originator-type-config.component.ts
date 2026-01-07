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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
  selector: 'tb-filter-node-originator-type-config',
  templateUrl: './originator-type-config.component.html',
  styleUrls: []
})
export class OriginatorTypeConfigComponent extends RuleNodeConfigurationComponent {

  originatorTypeConfigForm: UntypedFormGroup;

  allowedEntityTypes: EntityType[] = [
    EntityType.DEVICE,
    EntityType.ASSET,
    EntityType.ENTITY_VIEW,
    EntityType.TENANT,
    EntityType.CUSTOMER,
    EntityType.USER,
    EntityType.DASHBOARD,
    EntityType.RULE_CHAIN,
    EntityType.RULE_NODE,
    EntityType.EDGE
  ];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.originatorTypeConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      originatorTypes: isDefinedAndNotNull(configuration?.originatorTypes) ? configuration.originatorTypes : null
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.originatorTypeConfigForm = this.fb.group({
      originatorTypes: [configuration.originatorTypes, [Validators.required]]
    });
  }

}

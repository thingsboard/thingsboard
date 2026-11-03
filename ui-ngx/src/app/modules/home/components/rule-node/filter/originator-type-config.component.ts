// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
    selector: 'tb-filter-node-originator-type-config',
    templateUrl: './originator-type-config.component.html',
    styleUrls: [],
    standalone: false
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

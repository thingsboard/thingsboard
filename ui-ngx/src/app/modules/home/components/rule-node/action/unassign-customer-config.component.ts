// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-un-assign-to-customer-config',
    templateUrl: './unassign-customer-config.component.html',
    styleUrls: [],
    standalone: false
})
export class UnassignCustomerConfigComponent extends RuleNodeConfigurationComponent {

  unassignCustomerConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.unassignCustomerConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      customerNamePattern: isDefinedAndNotNull(configuration?.customerNamePattern) ? configuration.customerNamePattern : null,
      unassignFromCustomer: isDefinedAndNotNull(configuration?.customerNamePattern),
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.unassignCustomerConfigForm = this.fb.group({
      customerNamePattern: [configuration.customerNamePattern , []],
      unassignFromCustomer: [configuration.unassignFromCustomer, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['unassignFromCustomer'];
  }

  protected updateValidators(emitEvent: boolean) {
    const unassignFromCustomer: boolean = this.unassignCustomerConfigForm.get('unassignFromCustomer').value;
    if (unassignFromCustomer) {
      this.unassignCustomerConfigForm.get('customerNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.unassignCustomerConfigForm.get('customerNamePattern').setValidators([]);
    }
    this.unassignCustomerConfigForm.get('customerNamePattern').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      customerNamePattern: configuration.unassignFromCustomer ? configuration.customerNamePattern.trim() : null
    };
  }
}

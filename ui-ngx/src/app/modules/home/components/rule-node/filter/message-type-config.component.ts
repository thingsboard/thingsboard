// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-filter-node-message-type-config',
    templateUrl: './message-type-config.component.html',
    styleUrls: [],
    standalone: false
})
export class MessageTypeConfigComponent extends RuleNodeConfigurationComponent {

  messageTypeConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.messageTypeConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      messageTypes: isDefinedAndNotNull(configuration?.messageTypes) ? configuration.messageTypes : null
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.messageTypeConfigForm = this.fb.group({
      messageTypes: [configuration.messageTypes, [Validators.required]]
    });
  }
}

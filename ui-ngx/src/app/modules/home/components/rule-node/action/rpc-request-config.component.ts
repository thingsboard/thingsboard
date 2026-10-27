// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-rpc-request-config',
    templateUrl: './rpc-request-config.component.html',
    styleUrls: [],
    standalone: false
})
export class RpcRequestConfigComponent extends RuleNodeConfigurationComponent {

  rpcRequestConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.rpcRequestConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.rpcRequestConfigForm = this.fb.group({
      timeoutInSeconds: [configuration ? configuration.timeoutInSeconds : null, [Validators.required, Validators.min(0)]]
    });
  }
}

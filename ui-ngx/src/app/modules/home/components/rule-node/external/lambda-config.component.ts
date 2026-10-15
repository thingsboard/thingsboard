// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-lambda-config',
    templateUrl: './lambda-config.component.html',
    styleUrls: [],
    standalone: false
})
export class LambdaConfigComponent extends RuleNodeConfigurationComponent {

  lambdaConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.lambdaConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.lambdaConfigForm = this.fb.group({
      functionName: [configuration ? configuration.functionName : null, [Validators.required]],
      qualifier: [configuration ? configuration.qualifier : null, []],
      accessKey: [configuration ? configuration.accessKey : null, [Validators.required]],
      secretKey: [configuration ? configuration.secretKey : null, [Validators.required]],
      region: [configuration ? configuration.region : null, [Validators.required]],
      connectionTimeout: [configuration ? configuration.connectionTimeout : null, [Validators.required, Validators.min(0)]],
      requestTimeout: [configuration ? configuration.requestTimeout : null, [Validators.required, Validators.min(0)]],
      tellFailureIfFuncThrowsExc: [configuration ? configuration.tellFailureIfFuncThrowsExc : false, []]
    });
  }
}

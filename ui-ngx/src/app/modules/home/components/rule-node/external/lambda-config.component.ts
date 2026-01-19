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

@Component({
  selector: 'tb-external-node-lambda-config',
  templateUrl: './lambda-config.component.html',
  styleUrls: []
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

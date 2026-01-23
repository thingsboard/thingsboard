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
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  ArgumentTypeResult,
  ArgumentTypeResultMap,
  AttributeScopeMap,
  AttributeScopeResult,
  MathFunction
} from '../rule-node-config.models';


@Component({
    selector: 'tb-action-node-math-function-config',
    templateUrl: './math-function-config.component.html',
    styleUrls: ['./math-function-config.component.scss'],
    standalone: false
})
export class MathFunctionConfigComponent extends RuleNodeConfigurationComponent {

  mathFunctionConfigForm: UntypedFormGroup;

  MathFunction = MathFunction;
  ArgumentTypeResult = ArgumentTypeResult;
  argumentTypeResultMap = ArgumentTypeResultMap;
  attributeScopeMap = AttributeScopeMap;
  argumentsResult = Object.values(ArgumentTypeResult);
  attributeScopeResult = Object.values(AttributeScopeResult);

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.mathFunctionConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.mathFunctionConfigForm = this.fb.group({
      operation: [configuration ? configuration.operation : null, [Validators.required]],
      arguments: [configuration ? configuration.arguments : null, [Validators.required]],
      customFunction: [configuration ? configuration.customFunction : '', [Validators.required]],
      result: this.fb.group({
        type: [configuration ? configuration.result.type: null, [Validators.required]],
        attributeScope: [configuration ? configuration.result.attributeScope : null, [Validators.required]],
        key: [configuration ? configuration.result.key : '', [Validators.required]],
        resultValuePrecision: [configuration ? configuration.result.resultValuePrecision : 0],
        addToBody: [configuration ? configuration.result.addToBody : false],
        addToMetadata: [configuration ? configuration.result.addToMetadata : false]
      })
    });
  }

  protected updateValidators(emitEvent: boolean) {
    const operation: MathFunction = this.mathFunctionConfigForm.get('operation').value;
    const resultType: ArgumentTypeResult = this.mathFunctionConfigForm.get('result.type').value;
    if (operation === MathFunction.CUSTOM) {
      this.mathFunctionConfigForm.get('customFunction').enable({emitEvent: false});
      if (this.mathFunctionConfigForm.get('customFunction').value === null) {
        this.mathFunctionConfigForm.get('customFunction').patchValue('(x - 32) / 1.8', {emitEvent: false});
      }
    } else {
      this.mathFunctionConfigForm.get('customFunction').disable({emitEvent: false});
    }
    if (resultType === ArgumentTypeResult.ATTRIBUTE) {
      this.mathFunctionConfigForm.get('result.attributeScope').enable({emitEvent: false});
    } else {
      this.mathFunctionConfigForm.get('result.attributeScope').disable({emitEvent: false});
    }
    this.mathFunctionConfigForm.get('customFunction').updateValueAndValidity({emitEvent});
    this.mathFunctionConfigForm.get('result.attributeScope').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['operation', 'result.type'];
  }
}

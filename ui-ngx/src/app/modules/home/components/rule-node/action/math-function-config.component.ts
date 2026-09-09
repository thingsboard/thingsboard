// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

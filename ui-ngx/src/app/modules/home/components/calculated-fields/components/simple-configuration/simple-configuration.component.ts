///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import {
  calculatedFieldDefaultScript,
  CalculatedFieldScriptConfiguration,
  CalculatedFieldSimpleConfiguration,
  CalculatedFieldSimpleOutput,
  CalculatedFieldType,
  defaultSimpleCalculatedFieldOutput,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  OutputType
} from '@shared/models/calculated-field.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepClone } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable } from 'rxjs';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { map } from 'rxjs/operators';

type SimpeConfiguration = CalculatedFieldSimpleConfiguration | CalculatedFieldScriptConfiguration;

@Component({
  selector: 'tb-simple-configuration',
  templateUrl: './simple-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SimpleConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SimpleConfigurationComponent),
      multi: true
    }
  ],
})
export class SimpleConfigurationComponent implements ControlValueAccessor, Validator, OnChanges {

  @Input()
  isScript: boolean;

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  ownerId: EntityId;

  @Input({required: true})
  testScript: () => Observable<string>;

  simpleConfiguration = this.fb.group({
    arguments: this.fb.control({}),
    expressionSIMPLE: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    expressionSCRIPT: [calculatedFieldDefaultScript],
    output: this.fb.control<CalculatedFieldSimpleOutput>(defaultSimpleCalculatedFieldOutput),
    useLatestTs: [false]
  });

  readonly ScriptLanguage = ScriptLanguage;
  readonly OutputType = OutputType;

  functionArgs$ = this.simpleConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => ['ctx', ...Object.keys(argumentsObj)])
  );

  argumentsEditorCompleter$ = this.simpleConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj ?? {}))
  );

  argumentsHighlightRules$ = this.simpleConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
  );

  private propagateChange: (config: SimpeConfiguration) => void = () => { };

  constructor(private fb: FormBuilder) {
    this.simpleConfiguration.get('output').valueChanges.pipe(
      takeUntilDestroyed(),
    ).subscribe(() => {
      this.toggleScopeByOutputType();
    });

    this.simpleConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => {
      const { expressionSIMPLE, expressionSCRIPT, ...config } = value;
      const cfConfig = config as SimpeConfiguration;
      cfConfig.expression = this.isScript ? expressionSCRIPT : expressionSIMPLE;
      this.updatedModel(cfConfig);
    })
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'isScript') {
          this.updatedFormWithScript();
          if (!change.firstChange) {
            this.simpleConfiguration.updateValueAndValidity();
          }
        }
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.simpleConfiguration.valid || this.simpleConfiguration.disabled ? null : {invalidSimpleConfig: false};
  }

  writeValue(value: SimpeConfiguration): void {
    const formValue: any = deepClone(value);
    if (this.isScript) {
      formValue.expressionSCRIPT = formValue.expression ?? calculatedFieldDefaultScript;
    } else if (value.type === CalculatedFieldType.SIMPLE) {
      formValue.expressionSIMPLE = formValue.expression;
    }
    this.simpleConfiguration.patchValue(formValue, {emitEvent: false});
    this.updatedFormWithScript();
    setTimeout(() => {
      this.simpleConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: SimpeConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.simpleConfiguration.disable({emitEvent: false});
    } else {
      this.simpleConfiguration.enable({emitEvent: false});
      this.updatedFormWithScript();
    }
  }

  onTestScript() {
    this.testScript().subscribe((expression) => {
      this.simpleConfiguration.get('expressionSCRIPT').setValue(expression);
      this.simpleConfiguration.get('expressionSCRIPT').markAsDirty();
    })
  }

  private updatedModel(value: SimpeConfiguration): void {
    value.type = this.isScript ? CalculatedFieldType.SCRIPT : CalculatedFieldType.SIMPLE;
    this.propagateChange(value);
  }

  private updatedFormWithScript() {
    if (this.isScript) {
      this.simpleConfiguration.get('expressionSIMPLE').disable({emitEvent: false});
      this.simpleConfiguration.get('expressionSCRIPT').enable({emitEvent: false});
    } else {
      this.simpleConfiguration.get('expressionSIMPLE').enable({emitEvent: false});
      this.simpleConfiguration.get('expressionSCRIPT').disable({emitEvent: false});
    }
    this.toggleScopeByOutputType();
  }

  private toggleScopeByOutputType(): void {
    if (this.isScript || this.simpleConfiguration.get('output').value.type === OutputType.Attribute) {
      this.simpleConfiguration.get('useLatestTs').disable({emitEvent: false});
    } else {
      this.simpleConfiguration.get('useLatestTs').enable({emitEvent: false});
    }
  }
}

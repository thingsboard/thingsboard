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

import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable, of } from 'rxjs';
import {
  calculatedFieldDefaultScript,
  CalculatedFieldOutput,
  CalculatedFieldPropagationConfiguration,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  notEmptyObjectValidator,
  OutputType,
  PropagationDirectionTranslations,
  PropagationWithExpression
} from '@shared/models/calculated-field.models';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@app/shared/models/rule-node.models';
import { EntitySearchDirection } from '@shared/models/relation.models';

@Component({
  selector: 'tb-propagation-configuration',
  templateUrl: './propagation-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PropagationConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PropagationConfigurationComponent),
      multi: true
    }
  ],
})
export class PropagationConfigurationComponent implements ControlValueAccessor, Validator {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  testScript: () => Observable<string>;

  propagateConfiguration = this.fb.group({
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    applyExpressionToResolvedArguments: [false],
    relation: this.fb.group({
      direction: [EntitySearchDirection.TO, Validators.required],
      relationType: ['Contains', Validators.required],
    }),
    expression: [calculatedFieldDefaultScript],
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput),
  });

  readonly ScriptLanguage = ScriptLanguage;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly OutputType = OutputType;
  readonly Directions = Object.values(EntitySearchDirection) as Array<EntitySearchDirection>;
  readonly PropagationDirectionTranslations = PropagationDirectionTranslations;

  functionArgs$ = this.propagateConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => ['ctx', ...Object.keys(argumentsObj)])
  );

  argumentsEditorCompleter$ = this.propagateConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj ?? {}))
  );

  argumentsHighlightRules$ = this.propagateConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
  );

  private propagateChange: (config: CalculatedFieldPropagationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder) {
    this.propagateConfiguration.get('applyExpressionToResolvedArguments').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatedFormWithScript();
    })

    this.propagateConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldPropagationConfiguration) => {
      this.updatedModel(value);
    })
  }

  validate(): ValidationErrors | null {
    return this.propagateConfiguration.valid || this.propagateConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: PropagationWithExpression): void {
    value.expression = value.expression ?? calculatedFieldDefaultScript;
    this.propagateConfiguration.patchValue(value, {emitEvent: false});
    this.updatedFormWithScript();
    setTimeout(() => {
      this.propagateConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: CalculatedFieldPropagationConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.propagateConfiguration.disable({emitEvent: false});
    } else {
      this.propagateConfiguration.enable({emitEvent: false});
      this.updatedFormWithScript();
    }
  }

  onTestScript() {
    this.testScript().subscribe((expression) => {
      this.propagateConfiguration.get('expression').setValue(expression);
      this.propagateConfiguration.get('expression').markAsDirty();
    })
  }

  fetchOptions(searchText: string): Observable<Array<string>> {
    const search = searchText ? searchText?.toLowerCase() : '';
    return of(['Contains', 'Manages']).pipe(map(name => name?.filter(option => option.toLowerCase().includes(search))));
  }

  private updatedModel(value: CalculatedFieldPropagationConfiguration): void {
    value.type = CalculatedFieldType.PROPAGATION;
    this.propagateChange(value);
  }

  private updatedFormWithScript() {
    if (this.propagateConfiguration.get('applyExpressionToResolvedArguments').value) {
      this.propagateConfiguration.get('expression').enable({emitEvent: false});
    } else {
      this.propagateConfiguration.get('expression').disable({emitEvent: false});
    }
  }
}

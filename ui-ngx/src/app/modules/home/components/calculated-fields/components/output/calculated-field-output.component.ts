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

import { Component, DestroyRef, forwardRef, inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  CalculatedFieldOutput,
  CalculatedFieldSimpleOutput,
  OutputType,
  OutputTypeTranslations
} from '@shared/models/calculated-field.models';
import { digitsRegex, oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-calculate-field-output',
  templateUrl: './calculated-field-output.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldOutputComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldOutputComponent),
      multi: true
    }
  ],
})
export class CalculatedFieldOutputComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  simpleMode = false;

  @Input({required: true})
  entityId: EntityId;

  readonly outputTypes = Object.values(OutputType) as OutputType[];
  readonly OutputType = OutputType;
  readonly AttributeScope = AttributeScope;
  readonly OutputTypeTranslations = OutputTypeTranslations;
  readonly EntityType = EntityType;

  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  outputForm = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    scope: [{value: AttributeScope.SERVER_SCOPE, disabled: true}],
    type: [OutputType.Timeseries],
    decimalsByDefault: [null as number, [Validators.min(0), Validators.max(15), Validators.pattern(digitsRegex)]],
  });

  private propagateChange: (config: CalculatedFieldOutput | CalculatedFieldSimpleOutput) => void = () => { };

  ngOnInit() {
    this.outputForm.get('type').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(type => this.toggleScopeByOutputType(type));

    this.updatedFormWithMode();

    this.outputForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: CalculatedFieldOutput | CalculatedFieldSimpleOutput) => {
      this.updatedModel(value)
    })
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'simpleMode') {
          this.updatedFormWithMode();
          if (!change.firstChange) {
            this.outputForm.updateValueAndValidity();
          }
        }
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.outputForm.valid ? null : {outputConfig: false};
  }

  writeValue(value: CalculatedFieldOutput | CalculatedFieldSimpleOutput): void {
    this.outputForm.patchValue(value, {emitEvent: false});
    this.outputForm.get('type').updateValueAndValidity({onlySelf: true});
  }

  registerOnChange(fn: (config: CalculatedFieldOutput | CalculatedFieldSimpleOutput) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  private updatedModel(value: CalculatedFieldOutput | CalculatedFieldSimpleOutput) {
    if (this.simpleMode && 'name' in value) {
      value.name = value.name?.trim() ?? '';
    }
    this.propagateChange(value);
  }

  private toggleScopeByOutputType(type: OutputType): void {
    if (type === OutputType.Attribute) {
      this.outputForm.get('scope').enable({emitEvent: false});
    } else {
      this.outputForm.get('scope').disable({emitEvent: false});
    }
  }

  private updatedFormWithMode(): void {
    if (this.simpleMode) {
      this.outputForm.get('name').enable({emitEvent: false});
      this.outputForm.get('decimalsByDefault').enable({emitEvent: false});
    } else {
      this.outputForm.get('name').disable({emitEvent: false});
      this.outputForm.get('decimalsByDefault').disable({emitEvent: false});
    }
  }
}

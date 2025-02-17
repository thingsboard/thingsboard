///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { AfterViewInit, Component, forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  ValidationErrors,
  FormBuilder,
  FormGroup
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import {
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgumentEventValue,
  CalculatedFieldRollingTelemetryArgumentValue,
  CalculatedFieldSingleArgumentValue,
  CalculatedFieldEventArguments,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import {
  JsonObjectEditDialogComponent,
  JsonObjectEditDialogData
} from '@shared/components/dialog/json-object-edit-dialog.component';
import { filter } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-calculated-field-test-arguments',
  templateUrl: './calculated-field-test-arguments.component.html',
  styleUrls: ['./calculated-field-test-arguments.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldTestArgumentsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldTestArgumentsComponent),
      multi: true,
    }
  ]
})
export class CalculatedFieldTestArgumentsComponent extends PageComponent implements ControlValueAccessor, Validator, AfterViewInit {

  argumentsFormArray = this.fb.array<FormGroup>([]);

  readonly entityTypeTranslations = entityTypeTranslations;
  readonly ArgumentTypeTranslations = ArgumentTypeTranslations;
  readonly ArgumentType = ArgumentType;
  readonly CalculatedFieldType = CalculatedFieldType;

  private propagateChange: (value: CalculatedFieldEventArguments) => void = () => {};

  constructor(private fb: FormBuilder, private dialog: MatDialog) {
    super();
    this.argumentsFormArray.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.propagateChange(this.getValue()));
  }

  ngAfterViewInit(): void {
    this.argumentsFormArray.updateValueAndValidity();
  }

  registerOnChange(propagateChange: (value: CalculatedFieldEventArguments) => void): void {
    this.propagateChange = propagateChange;
  }

  registerOnTouched(_): void {
  }

  writeValue(argumentsObj: CalculatedFieldEventArguments): void {
    this.argumentsFormArray.clear();
    Object.keys(argumentsObj).forEach(key => {
      const value = { ...argumentsObj[key], argumentName: key } as CalculatedFieldArgumentEventValue;
      this.argumentsFormArray.push((value).type === ArgumentType.Rolling
        ? this.getRollingArgumentFormGroup(value as CalculatedFieldRollingTelemetryArgumentValue)
        : this.getSimpleArgumentFormGroup(value as CalculatedFieldSingleArgumentValue)
      );
    });
  }

  validate(): ValidationErrors | null {
    return this.argumentsFormArray.valid ? null : { arguments: { valid: false } };
  }

  openEditJSONDialog(group: FormGroup): void {
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, CalculatedFieldArgumentEventValue>(JsonObjectEditDialogComponent, {
      disableClose: true,
      height: '760px',
      minWidth: 'min(700px, 100%)',
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: group.value,
        required: true,
        fillHeight: true
      }
    }).afterClosed()
      .pipe(filter(Boolean))
      .subscribe(result => group.get('type').value === ArgumentType.Rolling
        ? group.patchValue({ timewindow: (result as CalculatedFieldRollingTelemetryArgumentValue).timewindow, values: (result as CalculatedFieldRollingTelemetryArgumentValue).values })
        : group.patchValue({ ts: (result as CalculatedFieldSingleArgumentValue).ts, value: (result as CalculatedFieldSingleArgumentValue).value }) );
  }

  private getSimpleArgumentFormGroup({ argumentName, type, ts, value }: CalculatedFieldSingleArgumentValue): FormGroup {
    return this.fb.group({
      argumentName: [{ value: argumentName, disabled: true}],
      type: [{ value: type , disabled: true }],
      ts: [ts],
      value: [value]
    }) as FormGroup;
  }

  private getRollingArgumentFormGroup({ argumentName, type, timewindow, values }: CalculatedFieldRollingTelemetryArgumentValue): FormGroup {
    return this.fb.group({
      ...timewindow ?? {},
      argumentName: [{ value: argumentName, disabled: true }],
      type: [{ value: type , disabled: true }],
      values: [values]
    }) as FormGroup;
  }

  private getValue(): CalculatedFieldEventArguments {
    return this.argumentsFormArray.getRawValue().reduce((acc, rowItem) => {
      const { argumentName, type, ...value } = rowItem;
      acc[argumentName] = value;
      return acc;
    }, {});
  }
}

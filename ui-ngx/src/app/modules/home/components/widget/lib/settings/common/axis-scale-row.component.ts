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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor, NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup, ValidationErrors, Validator,
  Validators,
} from '@angular/forms';
import { ValueSourceType, ValueSourceTypes, ValueSourceTypeTranslation } from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AxisLimitConfig } from '@home/components/widget/lib/chart/time-series-chart.models';
import { Datasource, DatasourceType, } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-axis-scale-row',
  templateUrl: './axis-scale-row.component.html',
  styleUrl: './axis-scale-row.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AxisScaleRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AxisScaleRowComponent),
      multi: true
    },
  ]
})
export class AxisScaleRowComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  isPanelView = false;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  labelKey: string;

  ValueSourceType = ValueSourceType;

  DataKeyType = DataKeyType;

  DatasourceType = DatasourceType;

  ValueSourceTypeTranslation = ValueSourceTypeTranslation;

  ValueSourceTypes = ValueSourceTypes;

  limitForm: UntypedFormGroup;

  private propagateChanges: (value: any) => void = () => {};

  private modelValue: AxisLimitConfig | null = null;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.limitForm = this.fb.group({
      type: [ValueSourceType.constant],
      value: [null],
      entityAlias: [null]
    });
    this.subscribeToTypeChanges();

    this.limitForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.updateValidators();
        this.updateView(value);
      });
  }

  writeValue(value: AxisLimitConfig) {
    this.modelValue = value;

    if (!this.limitForm) {
      return;
    }

    if (value) {
      this.limitForm.patchValue({
          type: value.type ?? ValueSourceType.constant,
          value: value.value ?? null,
          entityAlias: value.entityAlias ?? null
        },
        { emitEvent: false }
      );
    } else {
      this.limitForm.patchValue({
          type: ValueSourceType.constant,
          value: null,
          entityAlias: null
        },
        { emitEvent: false }
      );
    }

    this.updateValidators();
  }

  registerOnChange(fn: any) {
    this.propagateChanges = fn;
  }

  registerOnTouched(fn: any) {
  }

  validate(): ValidationErrors | null {
    return this.limitForm.valid ? null : {
      axisLimitForm: {
        valid: false,
        errors: this.getFormErrors()
      }
    };
  }

  private getFormErrors(): any {
    const errors: any = {};
    Object.keys(this.limitForm.controls).forEach(key => {
      const control = this.limitForm.get(key);
      if (control && control.errors) {
        errors[key] = control.errors;
      }
    });
    return errors;
  }

  private updateValidators() {
    const axisTypeControl = this.limitForm.get('type');
    const axisValueControl = this.limitForm.get('value');
    const axisEntityAliasControl = this.limitForm.get('entityAlias');

    if (axisTypeControl && axisValueControl && axisEntityAliasControl) {
      const type = axisTypeControl.value;
      if (type === ValueSourceType.latestKey || type === ValueSourceType.entity) {
        axisValueControl.setValidators([Validators.required]);
      } else {
        axisValueControl.clearValidators();
      }

      if (type === ValueSourceType.entity) {
        axisEntityAliasControl.setValidators([Validators.required]);
      } else {
        axisEntityAliasControl.clearValidators();
      }

      axisValueControl.updateValueAndValidity({ emitEvent: false });
      axisEntityAliasControl.updateValueAndValidity({ emitEvent: false });
    }
  }

  private subscribeToTypeChanges() {
    this.limitForm.controls.type.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const axisValueControl = this.limitForm.get('value');
        const axisEntityAliasControl = this.limitForm.get('entityAlias');
        if (axisValueControl) {
          axisValueControl.setValue(null, { emitEvent: true });
        }
        if (axisEntityAliasControl) {
          axisEntityAliasControl.setValue(null, { emitEvent: true });
        }
      });
  }

  private updateView(value: AxisLimitConfig) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChanges(value);
    }
  }
}

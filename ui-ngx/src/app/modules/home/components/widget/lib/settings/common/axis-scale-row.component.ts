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
  UntypedFormBuilder, UntypedFormControl,
  UntypedFormGroup, ValidationErrors, Validator,
  Validators,
} from '@angular/forms';
import {
  ValueSourceConfig,
  ValueSourceType,
  ValueSourceTypes,
  ValueSourceTypeTranslation
} from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataKey, Datasource, DatasourceType, } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { merge } from 'rxjs';

@Component({
  selector: 'tb-axis-scale-row',
  templateUrl: './axis-scale-row.component.html',
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

  latestKeyFormControl: UntypedFormControl;

  entityKeyFormControl: UntypedFormControl;

  private propagateChanges: (value: any) => void = () => {};

  private modelValue: ValueSourceConfig | null = null;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.limitForm = this.fb.group({
      type: [ValueSourceType.constant],
      value: [null],
      entityAlias: [null]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    merge(
      this.latestKeyFormControl.valueChanges,
      this.entityKeyFormControl.valueChanges,
      this.limitForm.valueChanges
    ).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.updateValidators();
        this.updateModel();
      });
  }

  writeValue(value: ValueSourceConfig) {
    this.modelValue = value;
    this.limitForm.patchValue(
      {
        type: value.type || ValueSourceType.constant,
        value: value.value,
        entityAlias: value.entityAlias,
      }, {emitEvent: false}
    );
    if (value.type === ValueSourceType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === ValueSourceType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }

    this.updateValidators();
    this.limitForm.markAllAsTouched();
  }

  registerOnChange(fn: any) {
    this.propagateChanges = fn;
  }

  registerOnTouched(fn: any) {
  }

  validate(): ValidationErrors | null {
    const type = this.limitForm.get('type')?.value;
    const errors: any = {};

    if (this.limitForm.invalid) {
      errors.form = false;
    }

    if (type === ValueSourceType.latestKey) {
      if (!this.latestKeyFormControl.value || this.latestKeyFormControl.invalid) {
        errors.latestKey = false;
      }
    } else if (type === ValueSourceType.entity) {
      if (!this.limitForm.get('entityAlias')?.value) {
        errors.entityAlias = false;
      }
      if (!this.entityKeyFormControl.value || this.entityKeyFormControl.invalid) {
        errors.entityKey = false;
      }
    }

    return Object.keys(errors).length ? { axisLimitForm: errors } : null;
  }

  private updateValidators() {
    const axisTypeControl = this.limitForm.get('type');
    if (axisTypeControl && this.entityKeyFormControl && this.latestKeyFormControl) {
      const type = axisTypeControl.value;
      if (type === ValueSourceType.latestKey) {
        this.latestKeyFormControl.setValidators([Validators.required]);
        this.entityKeyFormControl.clearValidators();
      } else if (type === ValueSourceType.entity) {
        this.latestKeyFormControl.clearValidators();
        this.limitForm.get('entityAlias').setValidators([Validators.required]);
        this.entityKeyFormControl.setValidators([Validators.required]);
      } else {
        this.latestKeyFormControl.clearValidators();
        this.entityKeyFormControl.clearValidators();
      }
      this.latestKeyFormControl.updateValueAndValidity({ emitEvent: false });
      this.entityKeyFormControl.updateValueAndValidity({ emitEvent: false });
    }
  }

  private updateModel() {
    const value = this.limitForm.value;
    const type = value.type;
    let updates: Partial<ValueSourceConfig> = { type };
    if (type === ValueSourceType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      updates.latestKey = latestKey?.name;
      updates.latestKeyType = latestKey?.type as any;
    } else if (type === ValueSourceType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      updates.entityKey = entityKey?.name;
      updates.entityKeyType = entityKey?.type as any;
      updates.entityAlias = value?.entityAlias;
    } else {
      updates.value = value?.value;
    }
    this.modelValue = updates as ValueSourceConfig;
    this.propagateChanges(this.modelValue);
  }
}

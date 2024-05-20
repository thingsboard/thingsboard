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

import { ChangeDetectorRef, Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKey, Datasource, DatasourceType } from '@app/shared/models/widget.models';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import {
  ValueSourceDataKeyType,
  ValueSourceDataKeyTypes,
  ValueSourceDataKeyTypeTranslation
} from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-value-source-data-key',
  templateUrl: './value-source-data-key.component.html',
  styleUrls: ['value-source-data-key.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ValueSourceDataKeyComponent),
      multi: true
    }
  ]
})
export class ValueSourceDataKeyComponent extends PageComponent implements OnInit, ControlValueAccessor {

  dataKeyType = DataKeyType;
  datasourceType = DatasourceType;

  valueSourceDataKeyType = ValueSourceDataKeyType;
  valueSourceDataKeyTypes = ValueSourceDataKeyTypes;
  valueSourceDataKeyTypeTranslation = ValueSourceDataKeyTypeTranslation;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  valueSourceFormGroup: UntypedFormGroup;

  modelValue;

  latestKeyFormControl: UntypedFormControl;
  entityKeyFormControl: UntypedFormControl;

  private propagateChange = (_val: any) => {};

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.valueSourceFormGroup = this.fb.group({
      type: [ValueSourceDataKeyType.constant, []],
      value: [null, [Validators.required]],
      entityAlias: [null, [Validators.required]]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    this.valueSourceFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.latestKeyFormControl.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.entityKeyFormControl.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.valueSourceFormGroup.get('type').valueChanges.subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.valueSourceFormGroup.disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else {
      this.valueSourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value): void {
    this.modelValue = value;
    this.valueSourceFormGroup.patchValue(
      {
        type: value.type,
        value: value.value,
        entityAlias: value.entityAlias
      }, {emitEvent: false}
    );
    if (value.type === ValueSourceDataKeyType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === ValueSourceDataKeyType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }

    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateModel() {
    const value = this.valueSourceFormGroup.value;
    this.modelValue.type = value.type;
    this.modelValue.value = value.value;
    this.modelValue.entityAlias = value.entityAlias;

    if (value.type === ValueSourceDataKeyType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      this.modelValue.latestKey = latestKey?.name;
      this.modelValue.latestKeyType = (latestKey?.type as any);
    } else if (value.type === ValueSourceDataKeyType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      this.modelValue.entityKey = entityKey?.name;
      this.modelValue.entityKeyType = (entityKey?.type as any);
    }
    this.propagateChange(this.modelValue);
  }

  private updateValidators(): void {
    const type: ValueSourceDataKeyType = this.valueSourceFormGroup.get('type').value;
    if (type === ValueSourceDataKeyType.constant) {
      this.valueSourceFormGroup.get('value').enable({emitEvent: false});
      this.valueSourceFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceDataKeyType.latestKey) {
      this.valueSourceFormGroup.get('value').disable({emitEvent: false});
      this.valueSourceFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.enable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceDataKeyType.entity) {
      this.valueSourceFormGroup.get('value').disable({emitEvent: false});
      this.valueSourceFormGroup.get('entityAlias').enable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.enable({emitEvent: false});
    }
  }

}

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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  ApiUsageDataKeysSettings,
  ApiUsageSettingsContext
} from "@home/components/widget/lib/settings/cards/api-usage-settings.component.models";
import { Observable, of } from "rxjs";

@Component({
  selector: 'tb-api-usage-data-key-row',
  templateUrl: './api-usage-data-key-row.component.html',
  styleUrls: ['./api-usage-data-key-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ApiUsageDataKeyRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ApiUsageDataKeyRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;
  DataKeyType = DataKeyType;

  widgetType = widgetType;

  @Input()
  disabled: boolean;

  @Input()
  dsEntityAliasId: string;

  @Input()
  context: ApiUsageSettingsContext;

  @Output()
  dataKeyRemoved = new EventEmitter();

  dataKeyFormGroup: UntypedFormGroup;

  modelValue: ApiUsageDataKeysSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.dataKeyFormGroup = this.fb.group({
      label: [null, [Validators.required]],
      state: [null, []],
      status: [null, [Validators.required]],
      maxLimit: [null, [Validators.required]],
      current: [null, [Validators.required]]
    });
    this.dataKeyFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataKeyFormGroup.disable({emitEvent: false});
    } else {
      this.dataKeyFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ApiUsageDataKeysSettings): void {
    this.modelValue = value;
    this.dataKeyFormGroup.patchValue(
      {
        label: value?.label,
        state: value?.state,
        status: value?.status,
        maxLimit: value?.maxLimit,
        current: value?.current
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  editKey(keyType: 'status' | 'maxLimit' | 'current') {
    const targetDataKey: DataKey = this.dataKeyFormGroup.get(keyType).value;
    this.context.editKey(targetDataKey, this.dsEntityAliasId).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.dataKeyFormGroup.get(keyType).patchValue(updatedDataKey);
        }
      }
    );
  }

  private updateValidators() {
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataKeyFormGroup.value};
    this.propagateChange(this.modelValue);
  }

  fetchDashboardStates(searchText?: string): Observable<Array<string>> {
    return of(this.context.callbacks.fetchDashboardStates(searchText));
  }
}

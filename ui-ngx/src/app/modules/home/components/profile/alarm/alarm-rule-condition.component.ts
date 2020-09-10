///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { KeyFilter } from '@shared/models/query/query.models';
import { deepClone } from '@core/utils';
import {
  AlarmRuleKeyFiltersDialogComponent,
  AlarmRuleKeyFiltersDialogData
} from './alarm-rule-key-filters-dialog.component';

@Component({
  selector: 'tb-alarm-rule-condition',
  templateUrl: './alarm-rule-condition.component.html',
  styleUrls: ['./alarm-rule-condition.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleConditionComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleConditionComponent),
      multi: true,
    }
  ]
})
export class AlarmRuleConditionComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  alarmRuleConditionControl: FormControl;

  private modelValue: Array<KeyFilter>;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleConditionControl = this.fb.control(null);
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Array<KeyFilter>): void {
    this.modelValue = value;
    this.updateConditionInfo();
  }

  public validate(c: FormControl) {
    return (this.modelValue && this.modelValue.length) ? null : {
      alarmRuleCondition: {
        valid: false,
      },
    };
  }

  public openFilterDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AlarmRuleKeyFiltersDialogComponent, AlarmRuleKeyFiltersDialogData,
      Array<KeyFilter>>(AlarmRuleKeyFiltersDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        keyFilters: this.disabled ? this.modelValue : deepClone(this.modelValue)
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.modelValue = result;
        this.updateModel();
      }
    });
  }

  private updateConditionInfo() {
    if (this.modelValue && this.modelValue.length) {
      this.alarmRuleConditionControl.patchValue('Condition set');
    } else {
      this.alarmRuleConditionControl.patchValue(null);
    }
  }

  private updateModel() {
    this.updateConditionInfo();
    this.propagateChange(this.modelValue);
  }
}

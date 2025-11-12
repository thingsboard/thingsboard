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
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormControl,
  Validator,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { isDefinedAndNotNull } from '@core/utils';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AlarmRule } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import {
  AlarmRuleDetailsDialogComponent,
  AlarmRuleDetailsDialogData
} from "@home/components/alarm-rules/alarm-rule-details-dialog.component";
import { coerceBoolean } from "@shared/decorators/coercion";

@Component({
  selector: 'tb-cf-alarm-rule',
  templateUrl: './cf-alarm-rule.component.html',
  styleUrls: ['./cf-alarm-rule.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CfAlarmRuleComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CfAlarmRuleComponent),
      multi: true,
    }
  ]
})
export class CfAlarmRuleComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  private modelValue: AlarmRule;

  alarmRuleFormGroup = this.fb.group({
    condition: this.fb.control({}, Validators.required),
    alarmDetails: [null],
    dashboardId: [null]
  });

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmRule): void {
    this.modelValue = value;
    const model = this.modelValue ? {
      ...this.modelValue,
      dashboardId: this.modelValue.dashboardId?.id
    } : null;
    this.alarmRuleFormGroup.patchValue(model, {emitEvent: false});
  }

  public openEditDetailsDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AlarmRuleDetailsDialogComponent, AlarmRuleDetailsDialogData,
          string>(AlarmRuleDetailsDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            alarmDetails: this.alarmRuleFormGroup.get('alarmDetails').value,
            readonly: this.disabled
          }
        }).afterClosed().subscribe((alarmDetails) => {
          if (isDefinedAndNotNull(alarmDetails)) {
            this.alarmRuleFormGroup.patchValue({alarmDetails});
          }
    });
  }

  public validate(c: UntypedFormControl) {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    this.modelValue = {...value, dashboardId: value.dashboardId ? new DashboardId(value.dashboardId) : null} as AlarmRule;
    this.propagateChange(this.modelValue);
  }
}

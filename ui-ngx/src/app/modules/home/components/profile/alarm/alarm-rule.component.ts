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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { DeviceProfileAlarmRule } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import {
  EditAlarmDetailsDialogComponent,
  EditAlarmDetailsDialogData
} from '@home/components/profile/alarm/edit-alarm-details-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { UtilsService } from '@core/services/utils.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-alarm-rule',
    templateUrl: './alarm-rule.component.html',
    styleUrls: ['./alarm-rule.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AlarmRuleComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AlarmRuleComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class AlarmRuleComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  deviceProfileId: EntityId;

  private modelValue: DeviceProfileAlarmRule;

  alarmRuleFormGroup: UntypedFormGroup;

  expandAlarmDetails = false;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private utils: UtilsService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleFormGroup = this.fb.group({
      condition: [null, [Validators.required]],
      schedule: [null],
      alarmDetails: [null],
      dashboardId: [null]
    });
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

  writeValue(value: DeviceProfileAlarmRule): void {
    this.modelValue = value;
    const model = this.modelValue ? {
      ...this.modelValue,
      dashboardId: this.modelValue.dashboardId?.id
    } : null;
    this.alarmRuleFormGroup.reset(model || undefined, {emitEvent: false});
  }

  public openEditDetailsDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EditAlarmDetailsDialogComponent, EditAlarmDetailsDialogData,
          string>(EditAlarmDetailsDialogComponent, {
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

  get alarmDetailsText(): string {
    const alarmType = this.alarmRuleFormGroup.get('alarmDetails').value;
    return this.utils.customTranslation(alarmType, alarmType);
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    if (this.modelValue) {
      this.modelValue = {...this.modelValue, ...value, dashboardId: value.dashboardId ? new DashboardId(value.dashboardId) : null};
      this.propagateChange(this.modelValue);
    }
  }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DeviceProfileAlarm, deviceProfileAlarmValidator } from '@shared/models/device.models';
import { guid } from '@core/utils';
import { Subject } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-device-profile-alarms',
    templateUrl: './device-profile-alarms.component.html',
    styleUrls: ['./device-profile-alarms.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceProfileAlarmsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DeviceProfileAlarmsComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class DeviceProfileAlarmsComponent implements ControlValueAccessor, OnInit, Validator, OnDestroy {

  deviceProfileAlarmsFormGroup: UntypedFormGroup;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @Input()
  deviceProfileId: EntityId;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.deviceProfileAlarmsFormGroup = this.fb.group({
      alarms: this.fb.array([])
    });
    this.deviceProfileAlarmsFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get alarmsFormArray(): UntypedFormArray {
    return this.deviceProfileAlarmsFormGroup.get('alarms') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceProfileAlarmsFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileAlarmsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(alarms: Array<DeviceProfileAlarm> | null): void {
    if (alarms?.length === this.alarmsFormArray.length) {
      this.alarmsFormArray.patchValue(alarms, {emitEvent: false});
    } else {
      const alarmsControls: Array<AbstractControl> = [];
      if (alarms) {
        alarms.forEach((alarm) => {
          alarmsControls.push(this.fb.control(alarm, [Validators.required]));
        });
      }
      this.deviceProfileAlarmsFormGroup.setControl('alarms', this.fb.array(alarmsControls), {emitEvent: false});
      if (this.disabled) {
        this.deviceProfileAlarmsFormGroup.disable({emitEvent: false});
      } else {
        this.deviceProfileAlarmsFormGroup.enable({emitEvent: false});
      }
    }
  }

  public trackByAlarm(index: number, alarmControl: AbstractControl): string {
    if (alarmControl) {
      return alarmControl.value.id;
    } else {
      return null;
    }
  }

  public removeAlarm(index: number) {
    (this.deviceProfileAlarmsFormGroup.get('alarms') as UntypedFormArray).removeAt(index);
  }

  public addAlarm() {
    const alarm: DeviceProfileAlarm = {
      id: guid(),
      alarmType: '',
      createRules: {
        CRITICAL: {
          condition: {
            condition: []
          }
        }
      }
    };
    const alarmsArray = this.deviceProfileAlarmsFormGroup.get('alarms') as UntypedFormArray;
    alarmsArray.push(this.fb.control(alarm, [deviceProfileAlarmValidator]));
    this.deviceProfileAlarmsFormGroup.updateValueAndValidity();
    if (!this.deviceProfileAlarmsFormGroup.valid) {
      this.updateModel();
    }
  }

  public validate(c: UntypedFormControl) {
    return (this.deviceProfileAlarmsFormGroup.valid) ? null : {
      alarms: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const alarms: Array<DeviceProfileAlarm> = this.deviceProfileAlarmsFormGroup.get('alarms').value;
    this.propagateChange(alarms);
  }
}

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
import { DeviceProfileAlarmRule, alarmRuleValidator } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-create-alarm-rules',
    templateUrl: './create-alarm-rules.component.html',
    styleUrls: ['./create-alarm-rules.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CreateAlarmRulesComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CreateAlarmRulesComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class CreateAlarmRulesComponent implements ControlValueAccessor, OnInit, Validator, OnDestroy {

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;

  alarmSeverityTranslationMap = alarmSeverityTranslations;

  @Input()
  disabled: boolean;

  @Input()
  deviceProfileId: EntityId;

  createAlarmRulesFormGroup: UntypedFormGroup;

  private usedSeverities: AlarmSeverity[] = [];

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.createAlarmRulesFormGroup = this.fb.group({
      createAlarmRules: this.fb.array([])
    });
    this.createAlarmRulesFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  createAlarmRulesFormArray(): UntypedFormArray {
    return this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(createAlarmRules: {[severity: string]: DeviceProfileAlarmRule}): void {
    const createAlarmRulesControls: Array<AbstractControl> = [];
    if (createAlarmRules) {
      Object.keys(createAlarmRules).forEach((severity) => {
        const createAlarmRule = createAlarmRules[severity];
        if (severity === 'empty') {
          severity = null;
        }
        createAlarmRulesControls.push(this.fb.group({
          severity: [severity, Validators.required],
          alarmRule: [createAlarmRule, Validators.required]
        }));
      });
    }
    this.createAlarmRulesFormGroup.setControl('createAlarmRules', this.fb.array(createAlarmRulesControls), {emitEvent: false});
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
    this.updateUsedSeverities();
    if (!this.disabled && !this.createAlarmRulesFormGroup.valid) {
      this.updateModel();
    }
  }

  public removeCreateAlarmRule(index: number) {
    (this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray).removeAt(index);
  }

  public addCreateAlarmRule() {
    const createAlarmRule: DeviceProfileAlarmRule = {
      condition: {
        condition: []
      }
    };
    const createAlarmRulesArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray;
    createAlarmRulesArray.push(this.fb.group({
      severity: [this.getFirstUnusedSeverity(), Validators.required],
      alarmRule: [createAlarmRule, alarmRuleValidator]
    }));
    this.createAlarmRulesFormGroup.updateValueAndValidity();
    if (!this.createAlarmRulesFormGroup.valid) {
      this.updateModel();
    }
  }

  private getFirstUnusedSeverity(): AlarmSeverity {
    for (const severityKey of Object.keys(AlarmSeverity)) {
      const severity = AlarmSeverity[severityKey];
      if (this.usedSeverities.indexOf(severity) === -1) {
        return severity;
      }
    }
    return null;
  }

  public validate(c: UntypedFormControl) {
    return (this.createAlarmRulesFormGroup.valid) ? null : {
      createAlarmRules: {
        valid: false,
      },
    };
  }

  public isDisabledSeverity(severity: AlarmSeverity, index: number): boolean {
    const usedIndex = this.usedSeverities.indexOf(severity);
    return usedIndex > -1 && usedIndex !== index;
  }

  private updateUsedSeverities() {
    this.usedSeverities = [];
    const value: {severity: string, alarmRule: DeviceProfileAlarmRule}[] = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    value.forEach((rule, index) => {
      this.usedSeverities[index] = AlarmSeverity[rule.severity];
    });
  }

  private updateModel() {
    const value: {severity: string, alarmRule: DeviceProfileAlarmRule}[] = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    const createAlarmRules: {[severity: string]: DeviceProfileAlarmRule} = {};
    value.forEach(v => {
      createAlarmRules[v.severity] = v.alarmRule;
    });
    this.updateUsedSeverities();
    this.propagateChange(createAlarmRules);
  }
}

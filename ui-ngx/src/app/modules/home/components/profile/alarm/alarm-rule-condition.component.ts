///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { AlarmCondition, AlarmConditionType } from '@shared/models/device.models';
import {
  AlarmRuleConditionDialogComponent,
  AlarmRuleConditionDialogData
} from '@home/components/profile/alarm/alarm-rule-condition-dialog.component';
import { TimeUnit } from '@shared/models/time/time.models';
import { EntityId } from '@shared/models/id/entity-id';
import { dynamicValueSourceTypeTranslationMap } from '@shared/models/query/query.models';

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

  @Input()
  deviceProfileId: EntityId;

  alarmRuleConditionFormGroup: FormGroup;

  specText = '';

  private modelValue: AlarmCondition;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder,
              private translate: TranslateService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleConditionFormGroup = this.fb.group({
      condition: [null, Validators.required],
      spec: [null, Validators.required]
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleConditionFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleConditionFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmCondition): void {
    this.modelValue = value;
    if (this.modelValue !== null && !isDefinedAndNotNull(this.modelValue?.spec?.predicate)) {
      this.modelValue = Object.assign(this.modelValue, {spec: {type: AlarmConditionType.SIMPLE}});
    }
    this.updateConditionInfo();
  }

  public conditionSet() {
    return this.modelValue && this.modelValue.condition.length;
  }

  public validate(c: FormControl) {
    return this.conditionSet() ? null : {
      alarmRuleCondition: {
        valid: false,
      },
    };
  }

  public openFilterDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AlarmRuleConditionDialogComponent, AlarmRuleConditionDialogData,
      AlarmCondition>(AlarmRuleConditionDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        condition: this.disabled ? this.modelValue : deepClone(this.modelValue),
        entityId: this.deviceProfileId
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.modelValue = result;
        this.updateModel();
      }
    });
  }

  private updateConditionInfo() {
    this.alarmRuleConditionFormGroup.patchValue(
      {
        condition: this.modelValue?.condition,
        spec: this.modelValue?.spec
      }
    );
    this.updateSpecText();
  }

  private updateSpecText() {
    this.specText = '';
    if (this.modelValue && this.modelValue.spec) {
      const spec = this.modelValue.spec;
      switch (spec.type) {
        case AlarmConditionType.SIMPLE:
          break;
        case AlarmConditionType.DURATION:
          let duringText = '';
          switch (spec.unit) {
            case TimeUnit.SECONDS:
              duringText = this.translate.instant('timewindow.seconds', {seconds: spec.predicate.defaultValue});
              break;
            case TimeUnit.MINUTES:
              duringText = this.translate.instant('timewindow.minutes', {minutes: spec.predicate.defaultValue});
              break;
            case TimeUnit.HOURS:
              duringText = this.translate.instant('timewindow.hours', {hours: spec.predicate.defaultValue});
              break;
            case TimeUnit.DAYS:
              duringText = this.translate.instant('timewindow.days', {days: spec.predicate.defaultValue});
              break;
          }
          if (spec.predicate.dynamicValue && spec.predicate.dynamicValue.sourceAttribute) {
            const attributeSource =
              this.translate.instant(dynamicValueSourceTypeTranslationMap.get(spec.predicate.dynamicValue.sourceType));
            this.specText = this.translate.instant('device-profile.condition-during-dynamic', {
              during: duringText,
              attribute: `${attributeSource}.${spec.predicate.dynamicValue.sourceAttribute}`
            });
          } else {
            this.specText = this.translate.instant('device-profile.condition-during', {
              during: duringText
            });
          }
          break;
        case AlarmConditionType.REPEATING:
          if (spec.predicate.dynamicValue && spec.predicate.dynamicValue.sourceAttribute) {
            const attributeSource =
              this.translate.instant(dynamicValueSourceTypeTranslationMap.get(spec.predicate.dynamicValue.sourceType));
            this.specText = this.translate.instant('device-profile.condition-repeat-times-dynamic', {
              count: spec.predicate.defaultValue,
              attribute: `${attributeSource}.${spec.predicate.dynamicValue.sourceAttribute}`
            });
          } else {
            this.specText = this.translate.instant('device-profile.condition-repeat-times',
              {count: spec.predicate.defaultValue});
          }
          break;
      }
    }
  }

  private updateModel() {
    this.updateConditionInfo();
    this.propagateChange(this.modelValue);
  }
}

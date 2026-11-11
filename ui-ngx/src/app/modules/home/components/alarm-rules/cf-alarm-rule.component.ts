// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { isDefinedAndNotNull } from '@core/utils';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AlarmRule, AlarmRuleCondition } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import {
  AlarmRuleDetailsDialogComponent,
  AlarmRuleDetailsDialogData
} from "@home/components/alarm-rules/alarm-rule-details-dialog.component";
import { coerceBoolean } from "@shared/decorators/coercion";
import { Observable } from "rxjs";

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
    ],
    standalone: false
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

  @Input()
  @coerceBoolean()
  isClearCondition = false;

  @Input({required: true})
  testScript: (expression: string) => Observable<string>;

  private modelValue: AlarmRule;

  alarmRuleFormGroup = this.fb.group({
    condition: this.fb.control<AlarmRuleCondition | null>(null, Validators.required),
    alarmDetails: this.fb.control<string | null>(null),
    dashboardId: this.fb.control<string | null>(null)
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
    ).subscribe((value) => {
      this.updateModel(value);
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
    this.alarmRuleFormGroup.reset(undefined, {emitEvent: false});
    if (value) {
      this.alarmRuleFormGroup.patchValue({
        ...value,
        dashboardId: value.dashboardId?.id
      }, {emitEvent: false});
    }
    this.modelValue = value ? this.toModel(this.alarmRuleFormGroup.value) : null;
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

  public validate(): ValidationErrors | null {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  private updateModel(value: typeof this.alarmRuleFormGroup.value): void {
    this.modelValue = this.toModel(value);
    this.propagateChange(this.modelValue);
  }

  private toModel(value: typeof this.alarmRuleFormGroup.value): AlarmRule {
    return {...value, dashboardId: value.dashboardId ? new DashboardId(value.dashboardId) : null} as AlarmRule;
  }
}

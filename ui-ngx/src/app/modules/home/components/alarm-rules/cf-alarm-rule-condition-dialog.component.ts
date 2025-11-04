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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroup,
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { TimeUnit, timeUnitTranslationMap } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from "@shared/models/rule-node.models";
import {
  AlarmRuleCondition,
  AlarmRuleConditionType,
  AlarmRuleConditionTypeTranslationMap,
  AlarmRuleExpressionType
} from "@shared/models/alarm-rule.models";
import {
  CalculatedFieldArgument,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights
} from "@shared/models/calculated-field.models";
import { TbEditorCompleter } from "@shared/models/ace/completion.models";
import { AceHighlightRules } from "@shared/models/ace/ace.models";
import { ComplexOperation, complexOperationTranslationMap } from "@shared/models/query/query.models";
import { FormControlsFrom } from "@shared/models/tenant.model";

export interface CfAlarmRuleConditionDialogData {
  readonly: boolean;
  condition: AlarmRuleCondition;
  arguments?: Record<string, CalculatedFieldArgument>;
}

@Component({
  selector: 'tb-cf-alarm-rule-condition-dialog',
  templateUrl: './cf-alarm-rule-condition-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: CfAlarmRuleConditionDialogComponent}],
  styleUrls: ['./cf-alarm-rules-dialog.component.scss'],
})
export class CfAlarmRuleConditionDialogComponent extends DialogComponent<CfAlarmRuleConditionDialogComponent, AlarmRuleCondition>
  implements OnInit, ErrorStateMatcher {

  AlarmRuleExpressionType = AlarmRuleExpressionType;

  timeUnits = Object.values(TimeUnit);
  timeUnitTranslations = timeUnitTranslationMap;
  alarmConditionTypes = Object.values(AlarmRuleConditionType);
  AlarmConditionType = AlarmRuleConditionType;
  alarmConditionTypeTranslation = AlarmRuleConditionTypeTranslationMap;
  readonly = this.data.readonly;
  condition = this.data.condition;

  conditionFormGroup: FormGroup<FormControlsFrom<AlarmRuleCondition>>;

  submitted = false;

  readonly scriptLanguage = ScriptLanguage;

  defaultValuePlaceholder = '';
  defaultValueRequiredError = '';
  defaultValueRangeError = '';
  defaultValuePatternError = '';

  durationDynamicMode = !!this.condition?.value?.dynamicValueArgument;
  repeatingDynamicMode = !!this.condition?.count?.dynamicValueArgument;

  ComplexOperation = ComplexOperation;
  complexOperationTranslationMap = complexOperationTranslationMap;

  functionArgs: Array<string>;
  argumentsEditorCompleter: TbEditorCompleter;
  argumentsHighlightRules: AceHighlightRules;

  arguments = this.data.arguments;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CfAlarmRuleConditionDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<CfAlarmRuleConditionDialogComponent, AlarmRuleCondition>,
              private fb: UntypedFormBuilder,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.functionArgs = ['ctx', ...Object.keys(this.data.arguments)];
    this.argumentsEditorCompleter = getCalculatedFieldArgumentsEditorCompleter(this.data.arguments);
    this.argumentsHighlightRules = getCalculatedFieldArgumentsHighlights(this.data.arguments);

    this.conditionFormGroup = this.fb.group({
      expression: this.fb.group({
        type: [this.condition?.expression?.type ?? AlarmRuleExpressionType.SIMPLE],
        expression: [this.condition?.expression?.expression ?? null, [Validators.required]],
        operation: [this.condition?.expression?.operation ?? ComplexOperation.AND],
        filters: [this.condition?.expression?.filters],
      }),
      type: [this.condition?.type ?? AlarmRuleConditionType.SIMPLE, Validators.required],
      unit: [this.condition?.unit ?? TimeUnit.SECONDS, Validators.required],
      value: this.fb.group({
        staticValue: [this.condition?.value?.staticValue ?? null, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]],
        dynamicValueArgument: [this.condition?.value?.dynamicValueArgument ?? null, Validators.required],
      }),
      count: this.fb.group({
        staticValue: [this.condition?.count?.staticValue ?? null, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]],
        dynamicValueArgument: [this.condition?.count?.dynamicValueArgument ?? null, Validators.required],
      }),
    });

    this.conditionFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type) => {
      this.updateValidators(type, true);
    });

    this.conditionFormGroup.get('expression.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type) => {
      this.updateExpressionTypeValidator(type);
      this.updateValidators(this.conditionFormGroup.get('type').value ?? AlarmRuleConditionType.SIMPLE);
    });

    this.updateValidators(this.conditionFormGroup.get('type').value ?? AlarmRuleConditionType.SIMPLE);
    this.updateExpressionTypeValidator(this.condition?.expression?.type ?? 'SIMPLE');
  }

  ngOnInit(): void {
  }

  toggleDynamicMode(type: AlarmRuleConditionType): void {
    if (type === AlarmRuleConditionType.DURATION) {
      this.durationDynamicMode = !this.durationDynamicMode;
      this.updateStaticValueValidator(type, this.durationDynamicMode);
    } else {
      this.repeatingDynamicMode = !this.repeatingDynamicMode;
      this.updateStaticValueValidator(type, this.repeatingDynamicMode);
    }
  }

  updateStaticValueValidator(type: AlarmRuleConditionType, dynamicValue: boolean) {
    const control = type === AlarmRuleConditionType.DURATION ? 'value' : 'count';
    if (dynamicValue) {
      this.conditionFormGroup.get(`${control}.staticValue`).disable({emitEvent: false});
      this.conditionFormGroup.get(`${control}.dynamicValueArgument`).enable({emitEvent: false});
    } else {
      this.conditionFormGroup.get(`${control}.staticValue`).enable({emitEvent: false});
      this.conditionFormGroup.get(`${control}.dynamicValueArgument`).disable({emitEvent: false});
    }
    this.conditionFormGroup.get(`${control}.staticValue`).updateValueAndValidity({emitEvent: false})
    this.conditionFormGroup.get(`${control}.dynamicValueArgument`).updateValueAndValidity({emitEvent: false})
  }

  updateExpressionTypeValidator(type: 'SIMPLE' | 'TBEL') {
    if (type === 'SIMPLE') {
      this.conditionFormGroup.get(`expression.expression`).disable();
      this.conditionFormGroup.get(`expression.filters`).enable();
    } else {
      this.conditionFormGroup.get(`expression.expression`).enable();
      this.conditionFormGroup.get(`expression.filters`).disable();
    }
    this.conditionFormGroup.get(`expression.expression`).updateValueAndValidity({emitEvent: false});
    this.conditionFormGroup.get(`expression.filters`).updateValueAndValidity({emitEvent: false});
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  private updateValidators(type: AlarmRuleConditionType, emitEvent = false) {
    switch (type) {
      case AlarmRuleConditionType.DURATION:
        this.conditionFormGroup.get('unit').enable();
        this.conditionFormGroup.get('value').enable();
        this.conditionFormGroup.get('count').disable();

        this.updateStaticValueValidator(type, this.durationDynamicMode);

        this.defaultValuePlaceholder = 'alarm-rule.condition-duration-value';
        this.defaultValueRequiredError = 'alarm-rule.condition-duration-value-required';
        this.defaultValueRangeError = 'alarm-rule.condition-duration-value-range';
        this.defaultValuePatternError = 'alarm-rule.condition-duration-value-pattern';
        break;
      case AlarmRuleConditionType.REPEATING:
        this.conditionFormGroup.get('count').enable();
        this.conditionFormGroup.get('value').disable();
        this.conditionFormGroup.get('unit').disable();

        this.updateStaticValueValidator(type, this.repeatingDynamicMode);

        this.defaultValuePlaceholder = 'alarm-rule.condition-repeating-value';
        this.defaultValueRequiredError = 'alarm-rule.condition-repeating-value-required';
        this.defaultValueRangeError = 'alarm-rule.condition-repeating-value-range';
        this.defaultValuePatternError = 'alarm-rule.condition-repeating-value-pattern';
        break;
      case AlarmRuleConditionType.SIMPLE:
        this.conditionFormGroup.get('value').disable();
        this.conditionFormGroup.get('count').disable();
        this.conditionFormGroup.get('unit').disable();
        break;
    }
    this.conditionFormGroup.get('value').updateValueAndValidity({emitEvent});
    this.conditionFormGroup.get('count').updateValueAndValidity({emitEvent});
    this.conditionFormGroup.get('unit').updateValueAndValidity({emitEvent});
  }

  get argumentsList(): Array<string> {
    return this.arguments ? Object.keys(this.arguments): [];
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.condition = this.conditionFormGroup.value as AlarmRuleCondition;
    this.dialogRef.close(this.condition);
  }

}

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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TimeUnit, timeUnitTranslationMap } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from "@shared/models/rule-node.models";
import {
  AlarmRuleCondition,
  AlarmRuleConditionType,
  AlarmRuleConditionTypeTranslationMap,
  AlarmRuleExpressionType,
  AlarmRuleFilter
} from "@shared/models/alarm-rule.models";
import {
  CalculatedFieldArgument,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights
} from "@shared/models/calculated-field.models";
import { TbEditorCompleter } from "@shared/models/ace/completion.models";
import { AceHighlightRules } from "@shared/models/ace/ace.models";
import { ComplexOperation, complexOperationTranslationMap } from "@shared/models/query/query.models";

export interface CfAlarmRuleConditionDialogData {
  readonly: boolean;
  condition: AlarmRuleCondition;
  arguments?: Record<string, CalculatedFieldArgument>;
}

@Component({
  selector: 'tb-cf-alarm-rule-condition-dialog',
  templateUrl: './cf-alarm-rule-condition-dialog.component.html',
  providers: [],
  styleUrls: ['./cf-alarm-rules-dialog.component.scss'],
})
export class CfAlarmRuleConditionDialogComponent extends DialogComponent<CfAlarmRuleConditionDialogComponent, AlarmRuleCondition> {

  AlarmRuleExpressionType = AlarmRuleExpressionType;

  timeUnits = Object.values(TimeUnit);
  timeUnitTranslations = timeUnitTranslationMap;
  alarmConditionTypes = Object.values(AlarmRuleConditionType);
  AlarmConditionType = AlarmRuleConditionType;
  alarmConditionTypeTranslation = AlarmRuleConditionTypeTranslationMap;
  readonly = this.data.readonly;
  condition = this.data.condition;

  conditionFormGroup = this.fb.group({
    expression: this.fb.group({
      type: [AlarmRuleExpressionType.SIMPLE],
      expression: ['', [Validators.required]],
      operation: [ComplexOperation.AND],
      filters: [null],
    }),
    type: this.fb.control(AlarmRuleConditionType.SIMPLE, Validators.required),
    unit: this.fb.control(TimeUnit.SECONDS, Validators.required),
    value: this.fb.group({
      staticValue: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]),
      dynamicValueArgument: this.fb.control<string>('', Validators.required),
    }),
    count: this.fb.group({
      staticValue: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]),
      dynamicValueArgument: this.fb.control<string>('', Validators.required),
    }),
  });

  readonly scriptLanguage = ScriptLanguage;

  defaultValuePlaceholder = '';
  defaultValueRequiredError = '';
  defaultValueRangeError = '';
  defaultValuePatternError = '';

  durationDynamicModeControl = this.fb.control<boolean>(false);
  repeatingDynamicModeControl = this.fb.control<boolean>(false);

  ComplexOperation = ComplexOperation;
  complexOperationTranslationMap = complexOperationTranslationMap;

  functionArgs: Array<string>;
  argumentsEditorCompleter: TbEditorCompleter;
  argumentsHighlightRules: AceHighlightRules;

  arguments = this.data.arguments;
  argumentsList: Array<string>;

  isNoData: boolean = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CfAlarmRuleConditionDialogData,
              public dialogRef: MatDialogRef<CfAlarmRuleConditionDialogComponent, AlarmRuleCondition>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.functionArgs = ['ctx', ...Object.keys(this.data.arguments)];
    this.argumentsEditorCompleter = getCalculatedFieldArgumentsEditorCompleter(this.data.arguments);
    this.argumentsHighlightRules = getCalculatedFieldArgumentsHighlights(this.data.arguments);
    this.argumentsList = this.arguments ? Object.keys(this.arguments): [];

    this.conditionFormGroup.patchValue({
      expression: {
        type: this.condition?.expression?.type ?? AlarmRuleExpressionType.SIMPLE,
        expression: this.condition?.expression?.expression ?? null,
        filters: this.condition?.expression?.filters ?? [],
        operation: this.condition?.expression?.operation ?? ComplexOperation.AND
      },
      type: this.condition?.type ?? AlarmRuleConditionType.SIMPLE,
      unit: this.condition?.unit ?? TimeUnit.SECONDS,
      value: {
        staticValue: this.condition?.value?.staticValue,
        dynamicValueArgument: this.condition?.value?.dynamicValueArgument,
      },
      count: {
        staticValue: this.condition?.count?.staticValue ?? null,
        dynamicValueArgument: this.condition?.count?.dynamicValueArgument
      }
    }, {emitEvent: false});

    this.durationDynamicModeControl.patchValue(!!this.condition?.value?.dynamicValueArgument, {emitEvent: false});
    this.repeatingDynamicModeControl.patchValue(!!this.condition?.count?.dynamicValueArgument, {emitEvent: false});

    this.checkIsNoData(this.condition?.expression?.filters);

    this.conditionFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type) => {
      this.updateValidators(type, true);
    });

    this.conditionFormGroup.get('expression.filters').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((filters) => {
      this.checkIsNoData(filters);
    });

    this.conditionFormGroup.get('expression.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type) => {
      this.updateExpressionTypeValidator(type);
      this.updateValidators(this.conditionFormGroup.get('type').value ?? AlarmRuleConditionType.SIMPLE);
    });

    this.durationDynamicModeControl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((mode) => {
      this.updateStaticValueValidator(AlarmRuleConditionType.DURATION, mode);
    });
    this.repeatingDynamicModeControl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((mode) => {
      this.updateStaticValueValidator(AlarmRuleConditionType.REPEATING, mode);
    });

    this.updateValidators(this.conditionFormGroup.get('type').value ?? AlarmRuleConditionType.SIMPLE);
    this.updateExpressionTypeValidator(this.condition?.expression?.type ?? 'SIMPLE');
  }

  updateStaticValueValidator(type: AlarmRuleConditionType, dynamicValue: boolean) {
    const control = this.conditionFormGroup.get(type === AlarmRuleConditionType.DURATION ? 'value' : 'count');
    if (dynamicValue) {
      control.get('staticValue').disable({emitEvent: false});
      control.get('dynamicValueArgument').enable({emitEvent: false});
    } else {
      control.get('staticValue').enable({emitEvent: false});
      control.get('dynamicValueArgument').disable({emitEvent: false});
    }
  }

  updateExpressionTypeValidator(type: 'SIMPLE' | 'TBEL') {
    if (type === 'SIMPLE') {
      this.conditionFormGroup.get(`expression.expression`).disable({emitEvent: false});
      this.conditionFormGroup.get(`expression.filters`).enable({emitEvent: false});
    } else {
      this.conditionFormGroup.get(`expression.expression`).enable({emitEvent: false});
      this.conditionFormGroup.get(`expression.filters`).disable({emitEvent: false});
    }
  }

  private checkIsNoData(filters: Array<AlarmRuleFilter>) {
    this.isNoData = this.hasNoData(filters);
    if (this.isNoData && this.conditionFormGroup.get('type').value !== AlarmRuleConditionType.SIMPLE) {
      this.conditionFormGroup.get('type').patchValue(AlarmRuleConditionType.SIMPLE);
    }
  }

  private hasNoData(data: Array<AlarmRuleFilter>) {
    const search = (filter) => {
      if (!filter) return false;
      if (Array.isArray(filter)) return filter.some(search);
      if (typeof filter !== 'object') return false;
      if (filter.type === 'NO_DATA') return true;
      if (filter.predicates?.length) return filter.predicates.some(search);
      return false;
    };
    return search(data);
  };

  private updateValidators(type: AlarmRuleConditionType, emitEvent = false) {
    switch (type) {
      case AlarmRuleConditionType.DURATION:
        this.conditionFormGroup.get('unit').enable({emitEvent: false});
        this.conditionFormGroup.get('value').enable({emitEvent: false});
        this.conditionFormGroup.get('count').disable({emitEvent: false});
        this.updateStaticValueValidator(type, this.durationDynamicModeControl.value);
        this.defaultValuePlaceholder = 'alarm-rule.condition-duration-value';
        this.defaultValueRequiredError = 'alarm-rule.condition-duration-value-required';
        this.defaultValueRangeError = 'alarm-rule.condition-duration-value-range';
        this.defaultValuePatternError = 'alarm-rule.condition-duration-value-pattern';
        break;
      case AlarmRuleConditionType.REPEATING:
        this.conditionFormGroup.get('count').enable({emitEvent: false});
        this.conditionFormGroup.get('value').disable({emitEvent: false});
        this.conditionFormGroup.get('unit').disable({emitEvent: false});
        this.updateStaticValueValidator(type, this.repeatingDynamicModeControl.value);
        this.defaultValuePlaceholder = 'alarm-rule.condition-repeating-value';
        this.defaultValueRequiredError = 'alarm-rule.condition-repeating-value-required';
        this.defaultValueRangeError = 'alarm-rule.condition-repeating-value-range';
        this.defaultValuePatternError = 'alarm-rule.condition-repeating-value-pattern';
        break;
      case AlarmRuleConditionType.SIMPLE:
        this.conditionFormGroup.get('value').disable({emitEvent: false});
        this.conditionFormGroup.get('count').disable({emitEvent: false});
        this.conditionFormGroup.get('unit').disable({emitEvent: false});
        break;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.conditionFormGroup.value as AlarmRuleCondition);
  }

}

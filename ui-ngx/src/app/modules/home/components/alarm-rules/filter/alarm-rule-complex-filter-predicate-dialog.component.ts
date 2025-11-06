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
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType,
  FilterPredicateType
} from '@shared/models/query/query.models';
import { AlarmRuleFilterPredicate, ComplexAlarmRuleFilterPredicate } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";

export interface AlarmRuleComplexFilterPredicateDialogData {
  complexPredicate: ComplexAlarmRuleFilterPredicate;
  isAdd: boolean;
  valueType: EntityKeyValueType;
  arguments: Record<string, CalculatedFieldArgument>;
}

@Component({
  selector: 'tb-alarm-rule-complex-filter-predicate-dialog',
  templateUrl: './alarm-rule-complex-filter-predicate-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AlarmRuleComplexFilterPredicateDialogComponent}],
  styleUrls: []
})

export class AlarmRuleComplexFilterPredicateDialogComponent extends
  DialogComponent<AlarmRuleComplexFilterPredicateDialogComponent, ComplexAlarmRuleFilterPredicate> {

  complexFilterFormGroup = this.fb.group(
    {
      operation: [ComplexOperation.AND, [Validators.required]],
      predicates: this.fb.control<AlarmRuleFilterPredicate[] | null>(null, Validators.required)
    }
  );

  complexOperations = Object.keys(ComplexOperation);
  complexOperationEnum = ComplexOperation;
  complexOperationTranslations = complexOperationTranslationMap;

  isAdd: boolean;

  arguments = this.data.arguments;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleComplexFilterPredicateDialogData,
              public dialogRef: MatDialogRef<AlarmRuleComplexFilterPredicateDialogComponent, ComplexAlarmRuleFilterPredicate>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.isAdd = this.data.isAdd;

    this.complexFilterFormGroup.patchValue(this.data.complexPredicate, {emitEvent: false});
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const predicate = this.complexFilterFormGroup.value as ComplexAlarmRuleFilterPredicate;
    predicate.type = FilterPredicateType.COMPLEX;
    this.dialogRef.close(predicate);
  }
}

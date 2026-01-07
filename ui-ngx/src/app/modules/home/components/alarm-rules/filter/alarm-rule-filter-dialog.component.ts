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

 import { Component, DestroyRef, Inject } from '@angular/core';
 import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
 import { Store } from '@ngrx/store';
 import { AppState } from '@core/core.state';
 import { FormBuilder, FormGroup, Validators } from '@angular/forms';
 import { Router } from '@angular/router';
 import { DialogComponent } from '@app/shared/components/dialog.component';
 import { ComplexOperation, EntityKeyValueType, entityKeyValueTypesMap } from '@shared/models/query/query.models';
 import { DialogService } from '@core/services/dialog.service';
 import { TranslateService } from '@ngx-translate/core';
 import {
   AlarmRuleFilter,
   AlarmRuleFilterPredicate,
   filterOperationTranslationMap,
   isPredicateArgumentsValid
 } from "@shared/models/alarm-rule.models";
 import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
 import { FormControlsFrom } from "@shared/models/tenant.model";
 import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

 export interface AlarmRuleFilterDialogData {
  filter: AlarmRuleFilter;
  isAdd: boolean;
  arguments: Record<string, CalculatedFieldArgument>;
  usedArguments: Array<string>;
}

@Component({
  selector: 'tb-alarm-rule-filter-dialog',
  templateUrl: './alarm-rule-filter-dialog.component.html',
  providers: [],
  styleUrls: ['./alarm-rule-filter-dialog.component.scss']
})
export class AlarmRuleFilterDialogComponent extends DialogComponent<AlarmRuleFilterDialogComponent, AlarmRuleFilter> {

  filterFormGroup: FormGroup<FormControlsFrom<AlarmRuleFilter>>;

  entityKeyValueTypesKeys = Object.keys(EntityKeyValueType);

  entityKeyValueTypeEnum = EntityKeyValueType;

  entityKeyValueTypes = entityKeyValueTypesMap;

  complexOperationTranslationMap = filterOperationTranslationMap;

  predicatesValid: boolean = false;

  ComplexOperation = ComplexOperation;

  arguments = this.data.arguments;
  argumentsList: Array<string>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleFilterDialogData,
              public dialogRef: MatDialogRef<AlarmRuleFilterDialogComponent, AlarmRuleFilter>,
              private dialogs: DialogService,
              private translate: TranslateService,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);

    this.argumentsList = this.arguments ? Object.keys(this.arguments) : [];

    this.filterFormGroup = this.fb.group(
      {
        argument: [this.argumentsList.includes(this.data.filter.argument) ? this.data.filter.argument : '' , [Validators.required]],
        valueType: [this.data.filter.valueType ?? EntityKeyValueType.STRING, [Validators.required]],
        predicates: [this.data.filter.predicates, [Validators.required]],
        operation: [this.data.filter.operation ?? ComplexOperation.AND]
      }
    );

    this.predicatesValid = isPredicateArgumentsValid(this.data.filter.predicates, this.arguments);
    this.filterFormGroup.get('predicates').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(predicates => {
      this.predicatesValid = isPredicateArgumentsValid(predicates, this.arguments);
    });

    this.filterFormGroup.get('valueType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((valueType: EntityKeyValueType) => {
      const prevValueType: EntityKeyValueType = this.filterFormGroup.value.valueType;
      const predicates: AlarmRuleFilterPredicate[] = this.filterFormGroup.get('predicates').value;
      if (prevValueType && prevValueType !== valueType) {
        if (predicates && predicates.length) {
          this.dialogs.confirm(this.translate.instant('filter.key-value-type-change-title'),
            this.translate.instant('filter.key-value-type-change-message')).subscribe(
            (result) => {
              if (result) {
                this.filterFormGroup.get('predicates').setValue([]);
              } else {
                this.filterFormGroup.get('valueType').setValue(prevValueType, {emitEvent: false});
              }
            }
          );
        }
      }
    });
  }

  argumentInUse(argument: string): boolean {
    return this.data.usedArguments.includes(argument);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.filterFormGroup.value as AlarmRuleFilter);
  }
}

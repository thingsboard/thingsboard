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

import { Component, DestroyRef, Inject, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormArray, FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {
  EntityKeyValueType,
  Filter,
  FilterPredicateValue,
  filterToUserFilterInfoList,
  UserFilterInputInfo
} from '@shared/models/query/query.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UnitService } from '@core/services/unit.service';
import { getSourceTbUnitSymbol, TbUnitConverter } from '@shared/models/unit.models';

export interface UserFilterDialogData {
  filter: Filter;
}

@Component({
  selector: 'tb-user-filter-dialog',
  templateUrl: './user-filter-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: UserFilterDialogComponent}],
  styleUrls: ['./user-filter-dialog.component.scss'],
})
export class UserFilterDialogComponent extends DialogComponent<UserFilterDialogComponent, Filter>
  implements ErrorStateMatcher {

  filter: Filter;

  userFilterFormGroup: FormGroup;

  valueTypeEnum = EntityKeyValueType;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: UserFilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<UserFilterDialogComponent, Filter>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef,
              private unitService: UnitService) {
    super(store, router, dialogRef);
    this.filter = data.filter;
    const userInputs = filterToUserFilterInfoList(this.filter, this.translate);

    const userInputControls: Array<FormGroup> = [];
    for (const userInput of userInputs) {
      userInputControls.push(this.createUserInputFormControl(userInput));
    }

    this.userFilterFormGroup = this.fb.group({
      userInputs: this.fb.array(userInputControls)
    });
  }

  private createUserInputFormControl(userInput: UserFilterInputInfo): FormGroup {
    const predicateValue: FilterPredicateValue<string | number | boolean> = (userInput.info.keyFilterPredicate as any).value;
    let value = isDefinedAndNotNull(predicateValue.userValue) ? predicateValue.userValue : predicateValue.defaultValue;
    let unitSymbol = '';
    let valueConvertor: TbUnitConverter;
    if (userInput.valueType === EntityKeyValueType.NUMERIC) {
      unitSymbol = this.unitService.getTargetUnitSymbol(userInput.unit);
      const sourceUnit = getSourceTbUnitSymbol(userInput.unit);
      value = this.unitService.convertUnitValue(value as number, userInput.unit);
      valueConvertor = this.unitService.geUnitConverter(unitSymbol, sourceUnit);
    }
    const userInputControl = this.fb.group({
      label: [userInput.label],
      valueType: [userInput.valueType],
      unitSymbol: [unitSymbol],
      value: [value,
        userInput.valueType === EntityKeyValueType.NUMERIC ||
        userInput.valueType === EntityKeyValueType.DATE_TIME  ? [Validators.required] : []]
    });
    userInputControl.get('value').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(userValue => {
      let value = userValue;
      if (valueConvertor) {
        value = valueConvertor(value as number);
      }
      (userInput.info.keyFilterPredicate as any).value.userValue = value;
    });
    return userInputControl;
  }

  userInputsFormArray(): FormArray {
    return this.userFilterFormGroup.get('userInputs') as FormArray;
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.filter);
  }
}

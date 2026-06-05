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
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
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

interface UserInputForm {
  label: FormControl<string>;
  valueType: FormControl<EntityKeyValueType>;
  unitSymbol: FormControl<string>;
  value: FormControl<string | number | boolean>;
}

@Component({
    selector: 'tb-user-filter-dialog',
    templateUrl: './user-filter-dialog.component.html',
    styleUrls: ['./user-filter-dialog.component.scss'],
    standalone: false
})
export class UserFilterDialogComponent extends DialogComponent<UserFilterDialogComponent, Filter> {

  filter: Filter;

  userInputsFormArray: FormArray<FormGroup<UserInputForm>>;

  valueTypeEnum = EntityKeyValueType;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: UserFilterDialogData,
              protected dialogRef: MatDialogRef<UserFilterDialogComponent, Filter>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef,
              private unitService: UnitService) {
    super(store, router, dialogRef);
    this.filter = data.filter;
    const userInputs = filterToUserFilterInfoList(this.filter, this.translate);

    const userInputControls = userInputs.map(input => this.createUserInputFormControl(input));
    this.userInputsFormArray = this.fb.array(userInputControls);
  }

  private createUserInputFormControl(userInput: UserFilterInputInfo): FormGroup<UserInputForm> {
    const predicateValue: FilterPredicateValue<string | number | boolean> = (userInput.info.keyFilterPredicate as any).value;
    let value: string | number | boolean = isDefinedAndNotNull(predicateValue.userValue) ? predicateValue.userValue : predicateValue.defaultValue;
    let unitSymbol = '';
    let valueConvertor: TbUnitConverter;
    if (userInput.valueType === EntityKeyValueType.NUMERIC) {
      unitSymbol = this.unitService.getTargetUnitSymbol(userInput.unit);
      const sourceUnit = getSourceTbUnitSymbol(userInput.unit);
      value = this.unitService.convertUnitValue(value as number, userInput.unit);
      valueConvertor = this.unitService.geUnitConverter(unitSymbol, sourceUnit);
    }
    const userInputControl = this.fb.group<UserInputForm>({
      label: this.fb.control(userInput.label),
      valueType: this.fb.control(userInput.valueType),
      unitSymbol: this.fb.control(unitSymbol),
      value: this.fb.control(value,
        userInput.valueType === EntityKeyValueType.NUMERIC ||
        userInput.valueType === EntityKeyValueType.DATE_TIME ? [Validators.required] : [])
    });
    userInputControl.controls.value.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(userValue => {
      let val = userValue;
      if (valueConvertor) {
        val = valueConvertor(val as number);
      }
      (userInput.info.keyFilterPredicate as any).value.userValue = val;
    });
    return userInputControl;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.filter);
  }
}

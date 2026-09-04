// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  BooleanOperation,
  EntityKeyValueType,
  KeyFilterPredicateUserInfo, NumericOperation,
  StringOperation
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import {
  FilterUserInfoDialogComponent,
  FilterUserInfoDialogData
} from '@home/components/filter/filter-user-info-dialog.component';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-filter-user-info',
    templateUrl: './filter-user-info.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FilterUserInfoComponent),
            multi: true
        }
    ],
    standalone: false
})
export class FilterUserInfoComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() key: string;

  @Input() operation: StringOperation | BooleanOperation | NumericOperation;

  @Input() valueType: EntityKeyValueType;

  private propagateChange = null;

  private keyFilterPredicateUserInfo: KeyFilterPredicateUserInfo;

  constructor(private dialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(keyFilterPredicateUserInfo: KeyFilterPredicateUserInfo): void {
    this.keyFilterPredicateUserInfo = keyFilterPredicateUserInfo;
  }

  public openFilterUserInfoDialog() {
   this.dialog.open<FilterUserInfoDialogComponent, FilterUserInfoDialogData,
     KeyFilterPredicateUserInfo>(FilterUserInfoDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilterPredicateUserInfo: deepClone(this.keyFilterPredicateUserInfo),
        valueType: this.valueType,
        key: this.key,
        operation: this.operation,
        readonly: this.disabled
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.keyFilterPredicateUserInfo = result;
          this.updateModel();
        }
      }
    );
  }

  private updateModel() {
    this.propagateChange(this.keyFilterPredicateUserInfo);
  }

}

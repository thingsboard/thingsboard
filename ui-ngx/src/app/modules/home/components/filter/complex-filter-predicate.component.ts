// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Inject, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ComplexFilterPredicateInfo, EntityKeyValueType } from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { ComplexFilterPredicateDialogData } from '@home/components/filter/filter-component.models';
import { COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN } from '@home/components/tokens';
import { ComponentType } from '@angular/cdk/portal';

@Component({
    selector: 'tb-complex-filter-predicate',
    templateUrl: './complex-filter-predicate.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ComplexFilterPredicateComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ComplexFilterPredicateComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() key: string;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  private propagateChange = null;

  private complexFilterPredicate: ComplexFilterPredicateInfo;

  constructor(@Inject(COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN) private complexFilterPredicateDialogComponent: ComponentType<any>,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(predicate: ComplexFilterPredicateInfo): void {
    this.complexFilterPredicate = predicate;
  }

  public openComplexFilterDialog() {
    this.dialog.open<any, ComplexFilterPredicateDialogData,
      ComplexFilterPredicateInfo>(this.complexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: this.disabled ? this.complexFilterPredicate : deepClone(this.complexFilterPredicate),
        readonly: this.disabled,
        valueType: this.valueType,
        isAdd: false,
        key: this.key,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource,
        onlyUserDynamicSource: this.onlyUserDynamicSource
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.complexFilterPredicate = result;
          this.updateModel();
        }
      }
    );
  }

  private updateModel() {
    this.propagateChange(this.complexFilterPredicate);
  }

}

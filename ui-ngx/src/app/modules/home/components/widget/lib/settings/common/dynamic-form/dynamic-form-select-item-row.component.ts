// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  ChangeDetectorRef,
  Component, DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import { FormSelectItem } from '@shared/models/dynamic-form.models';
import {
  DynamicFormSelectItemsComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-select-items.component';
import { ValueType } from '@shared/models/constants';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export const selectItemValid = (item: FormSelectItem): boolean => isDefinedAndNotNull(item.value) && !!item.label;

@Component({
    selector: 'tb-dynamic-form-select-item-row',
    templateUrl: './dynamic-form-select-item-row.component.html',
    styleUrls: ['./dynamic-form-select-item-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DynamicFormSelectItemRowComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DynamicFormSelectItemRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DynamicFormSelectItemRowComponent implements ControlValueAccessor, OnInit, Validator {

  ValueType = ValueType;

  @Input()
  disabled: boolean;

  @Input()
  index: number;

  @Output()
  selectItemRemoved = new EventEmitter();

  selectItemRowFormGroup: UntypedFormGroup;

  modelValue: FormSelectItem;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private cd: ChangeDetectorRef,
              private selectItemsComponent: DynamicFormSelectItemsComponent) {
  }

  ngOnInit() {
    this.selectItemRowFormGroup = this.fb.group({
      value: [null, [this.selectItemValueValidator()]],
      label: [null, [Validators.required]]
    });
    this.selectItemRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.selectItemRowFormGroup.disable({emitEvent: false});
    } else {
      this.selectItemRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: FormSelectItem): void {
    this.modelValue = value;
    this.selectItemRowFormGroup.patchValue(
      {
        value: value?.value,
        label: value?.label
      }, {emitEvent: false}
    );
    this.cd.markForCheck();
  }

  public validate(_c: UntypedFormControl) {
    const valueControl = this.selectItemRowFormGroup.get('value');
    if (valueControl.hasError('itemValueNotUnique')) {
      valueControl.updateValueAndValidity({onlySelf: false, emitEvent: false});
    }
    if (valueControl.hasError('itemValueNotUnique')) {
      this.selectItemRowFormGroup.get('value').markAsTouched();
      return {
        itemValueNotUnique: true
      };
    }
    const item: FormSelectItem = {...this.modelValue, ...this.selectItemRowFormGroup.value};
    if (!selectItemValid(item)) {
      return {
        selectItem: true
      };
    }
    return null;
  }

  private selectItemValueValidator(): ValidatorFn {
    return control => {
      if (!control.value) {
        return {
          required: true
        };
      }
      if (!this.selectItemsComponent.selectItemValueUnique(control.value, this.index)) {
        return {
          itemValueNotUnique: true
        };
      }
      return null;
    };
  }

  private updateModel() {
    const value: FormSelectItem = this.selectItemRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }
}

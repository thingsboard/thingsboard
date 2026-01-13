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

import {
  Component, DestroyRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  QueryList,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { TranslateService } from '@ngx-translate/core';
import { FormSelectItem } from '@shared/models/dynamic-form.models';
import {
  DynamicFormSelectItemRowComponent,
  selectItemValid
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-select-item-row.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-dynamic-form-select-items',
  templateUrl: './dynamic-form-select-items.component.html',
  styleUrls: ['./dynamic-form-select-items.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DynamicFormSelectItemsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DynamicFormSelectItemsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class DynamicFormSelectItemsComponent implements ControlValueAccessor, OnInit, Validator {

  @HostBinding('style.display') styleDisplay = 'flex';
  @HostBinding('style.overflow') styleOverflow = 'hidden';

  @ViewChildren(DynamicFormSelectItemRowComponent)
  selectItemRows: QueryList<DynamicFormSelectItemRowComponent>;

  @Input()
  disabled: boolean;

  selectItemsFormGroup: UntypedFormGroup;

  errorText = '';

  get dragEnabled(): boolean {
    return !this.disabled && this.selectItemsFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private translate: TranslateService) {
  }

  ngOnInit() {
    this.selectItemsFormGroup = this.fb.group({
      selectItems: this.fb.array([])
    });
    this.selectItemsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let items: FormSelectItem[] = this.selectItemsFormGroup.get('selectItems').value;
        if (items) {
          items = items.filter(i => selectItemValid(i));
        }
        this.propagateChange(items);
      }
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
      this.selectItemsFormGroup.disable({emitEvent: false});
    } else {
      this.selectItemsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: FormSelectItem[] | undefined): void {
    const items= value || [];
    this.selectItemsFormGroup.setControl('selectItems', this.prepareSelectItemsFormArray(items), {emitEvent: false});
  }

  public validate(_c: UntypedFormControl) {
    this.errorText = '';
    const itemsArray = this.selectItemsFormGroup.get('selectItems') as UntypedFormArray;
    const notUniqueControls =
      itemsArray.controls.filter(control => control.hasError('itemValueNotUnique'));
    for (const control of notUniqueControls) {
      control.updateValueAndValidity({onlySelf: false, emitEvent: false});
      if (control.hasError('itemValueNotUnique')) {
        this.errorText = this.translate.instant('dynamic-form.property.not-unique-select-option-value-error');
      }
    }
    let valid =  this.selectItemsFormGroup.valid;
    if (valid) {
      const items: FormSelectItem[] = this.selectItemsFormGroup.get('selectItems').value;
      valid = !items.some(item => !selectItemValid(item));
    }

    return valid ? null : {
      selectItems: {
        valid: false,
      },
    };
  }

  public selectItemValueUnique(value: any, index: number): boolean {
    const itemsArray = this.selectItemsFormGroup.get('selectItems') as UntypedFormArray;
    for (let i = 0; i < itemsArray.controls.length; i++) {
      if (i !== index) {
        const otherControl = itemsArray.controls[i];
        if (value === otherControl.value.value) {
          return false;
        }
      }
    }
    return true;
  }

  selectItemDrop(event: CdkDragDrop<string[]>) {
    const itemsArray = this.selectItemsFormGroup.get('selectItems') as UntypedFormArray;
    const item = itemsArray.at(event.previousIndex);
    itemsArray.removeAt(event.previousIndex, {emitEvent: false});
    itemsArray.insert(event.currentIndex, item, {emitEvent: true});
  }

  selectItemsFormArray(): UntypedFormArray {
    return this.selectItemsFormGroup.get('selectItems') as UntypedFormArray;
  }

  trackBySelectItem(_index: number, selectItemControl: AbstractControl): any {
    return selectItemControl;
  }

  removeSelectItem(index: number, emitEvent = true) {
    (this.selectItemsFormGroup.get('selectItems') as UntypedFormArray).removeAt(index, {emitEvent});
  }

  addSelectItem() {
    const item: FormSelectItem = {
      value: '',
      label: ''
    };
    const itemsArray = this.selectItemsFormGroup.get('selectItems') as UntypedFormArray;
    const itemControl = this.fb.control(item, []);
    itemsArray.push(itemControl);
  }

  private prepareSelectItemsFormArray(items: FormSelectItem[] | undefined): UntypedFormArray {
    const selectItemsControls: Array<AbstractControl> = [];
    if (items) {
      items.forEach((item) => {
        selectItemsControls.push(this.fb.control(item, []));
      });
    }
    return this.fb.array(selectItemsControls);
  }
}

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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceArray, coerceBoolean } from '@shared/decorators/coercion';
import { Observable, of } from 'rxjs';
import { filter, mergeMap, share, tap } from 'rxjs/operators';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { isDefined, isUndefined } from '@core/utils';

export interface StringItemsOption {
  name: string;
  value: any;
}
@Component({
    selector: 'tb-string-items-list',
    templateUrl: './string-items-list.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => StringItemsListComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class StringItemsListComponent implements ControlValueAccessor, OnInit {

  stringItemsForm: FormGroup;

  filteredValues: Observable<Array<StringItemsOption>>;

  searchText = '';

  itemList: StringItemsOption[] = [];

  private modelValue: Array<string> | null;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  @ViewChild('stringItemInput', {static: true}) stringItemInput: ElementRef<HTMLInputElement>;
  @ViewChild(MatAutocompleteTrigger) autocomplete: MatAutocompleteTrigger;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  @coerceBoolean()
  set required(value: boolean) {
    if (this.requiredValue !== value) {
      this.requiredValue = value;
      this.updateValidators();
    }
  }

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  label: string;

  @Input()
  placeholder: string;

  @Input()
  hint: string;

  @Input()
  requiredText: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  editable = false;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  fieldClass: string;

  @Input()
  @coerceArray()
  predefinedValues: StringItemsOption[];

  @Input()
  fetchOptionsFn: (searchText?: string) => Observable<Array<StringItemsOption>>;

  @Input()
  @coerceBoolean()
  allowUserValue = false;

  get itemsControl(): AbstractControl {
    return this.stringItemsForm.get('items');
  }

  get itemControl(): AbstractControl {
    return this.stringItemsForm.get('item');
  }

  private onTouched = () => {};
  private propagateChange: (value: any) => void = () => {};
  private dirty = false;

  constructor(private fb: FormBuilder) {
    this.stringItemsForm = this.fb.group({
      item: [null],
      items: [null]
    });
  }

  ngOnInit() {
    if (this.predefinedValues || isDefined(this.fetchOptionsFn)) {
      this.filteredValues = this.itemControl.valueChanges
        .pipe(
          tap((value) => {
            if (value && typeof value !== 'string') {
              this.add(value);
            } else if (value === null) {
              this.clear();
            }
          }),
          filter((value) => typeof value === 'string'),
          tap(name => this.searchText = name),
          mergeMap(name => this.fetchOptionsFn ? this.fetchOptionsFn(name) : this.fetchValues(name)),
          share()
        );
    }
  }

  updateValidators() {
    this.itemsControl.setValidators(this.required ? [Validators.required] : []);
    this.itemsControl.updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stringItemsForm.disable({emitEvent: false});
    } else {
      this.stringItemsForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.itemList = [];
      if (this.predefinedValues && !this.allowUserValue) {
        value.forEach(item => {
          const findItem = this.predefinedValues.find(option => option.value === item);
          if (findItem) {
            this.itemList.push(findItem);
          }
        });
      } else {
        value.forEach(item => this.itemList.push({value: item, name: item}));
      }
      this.itemsControl.setValue(this.itemList, {emitEvents: false});
    } else {
      this.itemsControl.setValue(null, {emitEvents: false});
      this.modelValue = null;
      this.itemList = [];
    }
    this.dirty = true;
  }

  addOnBlur(event: FocusEvent) {
    const target: HTMLElement = event.relatedTarget as HTMLElement;
    if ((target && target.tagName !== 'MAT-OPTION') || this.allowUserValue) {
      this.addItem(this.stringItemInput.nativeElement.value ?? '');
    }
    this.onTouched();
  }

  addOnEnd(event: MatChipInputEvent): void {
    this.addItem(event.value ?? '')
  }

  removeItems(item: StringItemsOption) {
    const index = this.modelValue.indexOf(item.value);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      this.itemList.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.itemsControl.setValue(this.itemList);
      this.propagateChange(this.modelValue);
      this.autocomplete?.closePanel();
    }
  }

  onFocus() {
    if (this.dirty) {
      this.itemControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayValueFn(values?: StringItemsOption): string | undefined {
    return values ? values.name : undefined;
  }

  private addItem(searchText: string) {
    searchText = searchText.trim();
    if (searchText) {
      if (this.allowUserValue || !this.predefinedValues && isUndefined(this.fetchOptionsFn)) {
        this.add({value: searchText, name: searchText});
      } else if (this.predefinedValues) {
        const findItems = this.predefinedValues
          .filter(value => value.name.toLowerCase().includes(searchText.toLowerCase()));
        if (findItems.length === 1) {
          this.add(findItems[0]);
        }
      } else if (isDefined(this.fetchOptionsFn)) {
        this.fetchOptionsFn(searchText).subscribe((findItems) => {
          if (findItems.length === 1) {
            this.add(findItems[0]);
          }
        })
      }
    }
  }

  private add(item: StringItemsOption) {
    if (!this.modelValue || this.modelValue.indexOf(item.value) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(item.value);
      this.itemList.push(item);
      this.itemsControl.setValue(this.itemList);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  private fetchValues(searchText?: string): Observable<Array<StringItemsOption>> {
    if (!this.predefinedValues?.length) {
      return of([]);
    }
    let result = this.predefinedValues;
    if (searchText && searchText.length) {
      result = this.predefinedValues.filter(option => option.name.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  private clear(value: string = '') {
    this.stringItemInput.nativeElement.value = value;
    this.itemControl.patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.stringItemInput.nativeElement.blur();
      this.stringItemInput.nativeElement.focus();
    }, 0);
  }
}

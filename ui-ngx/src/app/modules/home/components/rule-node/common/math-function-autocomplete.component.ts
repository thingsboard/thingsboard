// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { FunctionData, MathFunction, MathFunctionMap } from '../rule-node-config.models';
import { map, tap } from 'rxjs/operators';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
    selector: 'tb-math-function-autocomplete',
    templateUrl: './math-function-autocomplete.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MathFunctionAutocompleteComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MathFunctionAutocompleteComponent implements ControlValueAccessor, OnInit {

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input() disabled: boolean;

  @ViewChild('operationInput', {static: true}) operationInput: ElementRef;

  mathFunctionForm: UntypedFormGroup;

  modelValue: MathFunction | null;

  searchText = '';

  filteredOptions: Observable<FunctionData[]>;

  private dirty = false;

  private mathOperation = [...MathFunctionMap.values()];

  private propagateChange = null;

  constructor(public translate: TranslateService,
              private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.mathFunctionForm = this.fb.group({
      operation: ['']
    });
    this.filteredOptions = this.mathFunctionForm.get('operation').valueChanges.pipe(
      tap(value => {
        let modelValue: MathFunction;
        if (typeof value === 'string' && MathFunction[value]) {
          modelValue = MathFunction[value];
        } else {
          modelValue = null;
        }
        this.updateView(modelValue);
      }),
      map(value => {
        this.searchText = value || '';
        return value ? this._filter(value) : this.mathOperation.slice();
      }),
    );
  }

  private _filter(searchText: string) {
    const filterValue = searchText.toLowerCase();

    return this.mathOperation.filter(option => option.name.toLowerCase().includes(filterValue)
      || option.value.toLowerCase().includes(filterValue));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mathFunctionForm.disable({emitEvent: false});
    } else {
      this.mathFunctionForm.enable({emitEvent: false});
    }
  }

  mathFunctionDisplayFn(value: MathFunction | null) {
    if (value) {
      const funcData = MathFunctionMap.get(value)
      return funcData.value + ' | ' + funcData.name;
    }
    return '';
  }

  writeValue(value: MathFunction | null): void {
    this.modelValue = value;
    this.mathFunctionForm.get('operation').setValue(value, {emitEvent: false});
    this.dirty = true;
  }

  updateView(value: MathFunction | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  onFocus() {
    if (this.dirty) {
      this.mathFunctionForm.get('operation').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.mathFunctionForm.get('operation').patchValue('');
    setTimeout(() => {
      this.operationInput.nativeElement.blur();
      this.operationInput.nativeElement.focus();
    }, 0);
  }

}

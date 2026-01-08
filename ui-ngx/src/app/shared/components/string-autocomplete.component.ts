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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  NG_VALUE_ACCESSOR,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, switchMap, take, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';

export interface ErrorMessageConfig {
  [errorKey: string]: string;
}

@Component({
  selector: 'tb-string-autocomplete',
  templateUrl: './string-autocomplete.component.html',
  styleUrls: ['./string-autocomplete.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringAutocompleteComponent),
      multi: true
    }
  ]
})
export class StringAutocompleteComponent implements ControlValueAccessor, OnInit {

  @ViewChild('nameInput', {static: true}) nameInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  fetchOptionsFn: (searchText?: string) => Observable<Array<string>>;

  @Input()
  placeholderText: string = this.translate.instant('widget-config.set');

  @Input()
  subscriptSizing: SubscriptSizing = 'dynamic';

  @Input()
  additionalClass: string | string[] | Record<string, boolean | undefined | null> = 'tb-inline-field tb-suffix-show-on-hover';

  @Input()
  appearance: MatFormFieldAppearance = 'outline';

  @Input()
  label: string;

  @Input()
  panelWidth: string = 'fit-content';

  @Input()
  tooltipClass = 'tb-error-tooltip';

  @Input()
  errorText: string;

  @Input()
  controlValidators: ValidatorFn[] = [];

  @Input()
  errorMessages: ErrorMessageConfig;

  @Input()
  @coerceBoolean()
  showInlineError = false;

  selectionFormControl: FormControl;

  modelValue: string | null;

  filteredOptions$: Observable<Array<string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
  }

  ngOnInit() {
    const validators = [Validators.pattern(/.*\S.*/)];
    if (this.controlValidators?.length) {
     validators.push(...this.controlValidators);
      const parentHasRequired = this.controlValidators.some(v => v === Validators.required);
      if (this.required && !parentHasRequired) {
        validators.push(Validators.required);
      }
    } else if (this.required) {
      validators.push(Validators.required);
    }
    this.selectionFormControl = this.fb.control('', validators);
    this.filteredOptions$ = this.selectionFormControl.valueChanges
      .pipe(
        tap(value => this.updateView(value)),
        map(value => value ? value : ''),
        switchMap(value => this.fetchOptionsFn ? this.fetchOptionsFn(value) : of([]))
      );
  }

  writeValue(option?: string): void {
    this.searchText = '';
    this.modelValue = option ? option : null;

    if (this.fetchOptionsFn) {
      this.fetchOptionsFn(option)
        .pipe(
          map(options => {
            if (options) {
              const foundOption = options.find(opt => opt === option);
              return foundOption ? foundOption : option;
            }

            return option;
          }),
          take(1)
        )
        .subscribe(result => {
          this.selectionFormControl.patchValue(result, { emitEvent: false });
          this.dirty = true;
        });
    } else {
      this.selectionFormControl.patchValue(null, { emitEvent: false });
      this.dirty = true;
    }
  }

  onFocus() {
    if (this.dirty) {
      this.selectionFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string) {
    this.searchText = value ? value : '';
    if (this.modelValue !== value) {
      this.modelValue = value?.trim();
      this.propagateChange(this.modelValue);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectionFormControl.disable({emitEvent: false});
    } else {
      this.selectionFormControl.enable({emitEvent: false});
    }
  }

  clear() {
    this.selectionFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.nameInput.nativeElement.blur();
      this.nameInput.nativeElement.focus();
    }, 0);
  }

  get getErrorMessage(): string {
    if (!this.selectionFormControl.errors) {
      return '';
    }
    if (this.errorMessages) {
      for (const errorKey in this.selectionFormControl.errors) {
        if (this.errorMessages[errorKey]) {
          return this.errorMessages[errorKey];
        }
      }
    }
    return this.errorText;
  }
}

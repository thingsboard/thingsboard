///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  Component,
  Input,
  forwardRef,
  OnInit,
  AfterViewInit,
  HostBinding,
  ViewChild,
  ElementRef,
  Injector
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  FormControl,
  Validators,
  FormBuilder
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { tap, map, switchMap } from 'rxjs/operators';

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
export class StringAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  @HostBinding('style.display') get hostDisplay() {return 'flex';};

  @Input()
  disabled: boolean;

  @Input()
  required: boolean;

  @Input() fetchOptionsFn: (searchText?: string) => Observable<Array<string>>;

  @ViewChild('nameInput', {static: true}) nameInput: ElementRef;

  selectionFormControl: FormControl;

  modelValue: string | null;

  filteredOptions: Observable<Array<string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_val: any) => {};

  constructor(private injector: Injector,
              private fb: FormBuilder) {
  }

  ngOnInit() {
    this.selectionFormControl = this.fb.control('', []);
    this.filteredOptions = this.selectionFormControl.valueChanges
      .pipe(
        tap(value => this.updateView(value)),
        map(value => value ? value : ''),
        switchMap(value => this.fetchOptionsFn ? this.fetchOptionsFn(value) : of([]))
      );
  }

  ngAfterViewInit() {
    if (this.required) {
      this.selectionFormControl.setValidators([Validators.required]);
    }
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
          })
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
      this.modelValue = value;
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
}

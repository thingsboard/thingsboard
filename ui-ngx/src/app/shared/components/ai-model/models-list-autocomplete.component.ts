///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { AiModelMap, AiProvider } from '@shared/models/ai-model.models';

@Component({
  selector: 'tb-models-list-autocomplete',
  templateUrl: './models-list-autocomplete.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModelsListAutocompleteComponent),
      multi: true
    }
  ]
})
export class ModelsListAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

  @ViewChild('nameInput', {static: true}) nameInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  provider: AiProvider;

  @Input()
  placeholderText: string = this.translate.instant('widget-config.set');

  @Input()
  subscriptSizing: SubscriptSizing = 'dynamic';

  @Input()
  appearance: MatFormFieldAppearance = 'outline';

  @Input()
  label: string;

  @Input()
  errorText: string;

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
    this.selectionFormControl = this.fb.control('', this.required ? [Validators.required] : []);
    this.setupFilteredOptions();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.provider && !changes.provider.isFirstChange()) {
      this.setupFilteredOptions();
      this.selectionFormControl.setValue(null, {emitEvent: false});
      this.modelValue = null;
      this.propagateChange(null);
    }
  }

  private setupFilteredOptions() {
    this.filteredOptions$ = this.selectionFormControl.valueChanges.pipe(
      startWith(''),
      tap(value => this.updateView(value)),
      map(value => {
        const search = value ? value.toLowerCase() : '';
        const options = this.provider ? AiModelMap.get(this.provider).modelList || [] : [];
        return search ? options.filter(option => option.toLowerCase().includes(search)) : options;
      })
    );
  }

  writeValue(option?: string): void {
    this.searchText = '';
    this.modelValue = option ? option : null;

    if (option) {
      this.selectionFormControl.patchValue(option, { emitEvent: false });
      this.dirty = true;
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
    if (this.modelValue !== value && value) {
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
    this.propagateChange(null);
    this.modelValue = null;
    setTimeout(() => {
      this.nameInput.nativeElement.blur();
      this.nameInput.nativeElement.focus();
    }, 0);
  }
}

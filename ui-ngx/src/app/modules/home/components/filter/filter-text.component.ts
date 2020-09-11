///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { KeyFilter, keyFiltersToText } from '@shared/models/query/query.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-filter-text',
  templateUrl: './filter-text.component.html',
  styleUrls: ['./filter-text.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterTextComponent),
      multi: true
    }
  ]
})
export class FilterTextComponent implements ControlValueAccessor, OnInit {

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @Input()
  noFilterText = this.translate.instant('filter.no-filter-text');

  @Input()
  addFilterPrompt = this.translate.instant('filter.add-filter-prompt');

  requiredClass = false;

  private filterText: string;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder,
              private translate: TranslateService,
              private datePipe: DatePipe) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Array<KeyFilter>): void {
    this.updateFilterText(value);
  }

  private updateFilterText(value: Array<KeyFilter>) {
    this.requiredClass = false;
    if (value && value.length) {
      this.filterText = keyFiltersToText(this.translate, this.datePipe, value);
    } else {
      if (this.required && !this.disabled) {
        this.filterText = this.addFilterPrompt;
        this.requiredClass = true;
      } else {
        this.filterText = this.noFilterText;
      }
    }
  }

}

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

import { Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ComplexOperation, KeyFilter, keyFiltersToText } from '@shared/models/query/query.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { coerceBoolean } from '@shared/decorators/coercion';

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
  ],
  standalone: false
})
export class FilterTextComponent implements ControlValueAccessor, OnChanges {

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  @Input()
  noFilterText = this.translate.instant('filter.no-filter-text');

  @Input()
  addFilterPrompt = this.translate.instant('filter.add-filter-prompt');

  @Input()
  nowrap = false;

  @Input()
  operation: ComplexOperation = ComplexOperation.AND;

  requiredClass = false;

  public filterText: string;

  private currentValue: Array<KeyFilter>;

  constructor(private translate: TranslateService,
              private datePipe: DatePipe) {
  }

  registerOnChange(_fn: any): void {
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.operation && !changes.operation.firstChange
      && changes.operation.currentValue !== changes.operation.previousValue) {
      this.updateFilterText(this.currentValue);
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Array<KeyFilter>): void {
    this.currentValue = value;
    this.updateFilterText(value);
  }

  private updateFilterText(value: Array<KeyFilter>) {
    this.requiredClass = false;
    if (value && value.length) {
      this.filterText = keyFiltersToText(this.translate, this.datePipe, value, this.operation);
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

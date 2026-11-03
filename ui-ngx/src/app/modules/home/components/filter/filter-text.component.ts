// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
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
    ],
    standalone: false
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

  @Input()
  nowrap = false;

  requiredClass = false;

  public filterText: string;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: UntypedFormBuilder,
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

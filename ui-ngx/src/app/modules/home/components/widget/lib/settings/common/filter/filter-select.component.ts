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

import { Component, DestroyRef, ElementRef, forwardRef, Input, OnInit, SkipSelf, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { IAliasController } from '@core/api/widget-api.models';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { ENTER } from '@angular/cdk/keycodes';
import { ErrorStateMatcher } from '@angular/material/core';
import { FilterSelectCallbacks } from './filter-select.component.models';
import { Filter } from '@shared/models/query/query.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-filter-select',
  templateUrl: './filter-select.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => FilterSelectComponent),
    multi: true
  },
    {
      provide: ErrorStateMatcher,
      useExisting: FilterSelectComponent
    }]
})
export class FilterSelectComponent implements ControlValueAccessor, OnInit, ErrorStateMatcher {

  selectFilterFormGroup: UntypedFormGroup;

  modelValue: string | null;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: FilterSelectCallbacks;

  @Input()
  @coerceBoolean()
  showLabel: boolean;

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @ViewChild('filterAutocomplete') filterAutocomplete: MatAutocomplete;

  @Input()
  @coerceBoolean()
  tbRequired: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('filterInput', {static: true}) filterInput: ElementRef;

  filteredFilters: Observable<Array<Filter>>;

  searchText = '';

  private dirty = false;
  private filterList: Array<Filter> = [];
  private propagateChange = (_v: any) => { };

  constructor(@SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    this.selectFilterFormGroup = this.fb.group({
      filter: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.loadFilters();

    this.filteredFilters = this.selectFilterFormGroup.get('filter').valueChanges
      .pipe(
        tap(value => {
          let modelValue: Filter;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.filter) : ''),
        mergeMap(name => this.fetchFilters(name) ),
        share()
      );

    this.aliasController.filtersChanged.pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      this.loadFilters();
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = this.tbRequired && !this.modelValue;
    return originalErrorState || customErrorState;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectFilterFormGroup.disable({emitEvent: false});
    } else {
      this.selectFilterFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    let filter = null;
    if (value != null) {
      const filters = this.aliasController.getFilters();
      if (filters[value]) {
        filter = filters[value];
      }
    }
    if (filter != null) {
      this.modelValue = filter.id;
      this.selectFilterFormGroup.get('filter').patchValue(filter, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.selectFilterFormGroup.get('filter').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectFilterFormGroup.get('filter').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: Filter | null) {
    const filterId = value ? value.id : null;
    if (this.modelValue !== filterId) {
      this.modelValue = filterId;
      this.propagateChange(this.modelValue);
    }
  }

  displayFilterFn(filter?: Filter): string | undefined {
    return filter ? filter.filter : undefined;
  }

  fetchFilters(searchText?: string): Observable<Array<Filter>> {
    this.searchText = searchText;
    let result = this.filterList;
    if (searchText && searchText.length) {
      result = this.filterList.filter((filter) => filter.filter.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  clear(value: string = '') {
    this.filterInput.nativeElement.value = value;
    this.selectFilterFormGroup.get('filter').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.filterInput.nativeElement.blur();
      this.filterInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return text?.length > 0;
  }

  filterEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createFilter($event, this.searchText);
      }
    }
  }

  createFilter($event: Event, filter: string, focusOnCancel = true) {
    $event.preventDefault();
    $event.stopPropagation();
    if (this.callbacks && this.callbacks.createFilter) {
      this.callbacks.createFilter(filter).subscribe((newFilter) => {
          if (!newFilter) {
            if (focusOnCancel) {
              setTimeout(() => {
                this.filterInput.nativeElement.blur();
                this.filterInput.nativeElement.focus();
              }, 0);
            }
          } else {
            this.modelValue = newFilter.id;
            this.selectFilterFormGroup.get('filter').patchValue(newFilter, {emitEvent: true});
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }

  private loadFilters(): void {
    this.filterList = [];
    const filters = this.aliasController.getFilters();
    for (const filterId of Object.keys(filters)) {
      this.filterList.push(filters[filterId]);
    }
    this.dirty = true;
  }
}

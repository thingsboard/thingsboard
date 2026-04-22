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
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { Observable, of, shareReplay } from 'rxjs';
import { debounceTime, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { IAliasController } from '@core/api/widget-api.models';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { ENTER } from '@angular/cdk/keycodes';
import { FilterSelectCallbacks } from './filter-select.component.models';
import { Filter } from '@shared/models/query/query.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';

@Component({
    selector: 'tb-filter-select',
    templateUrl: './filter-select.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FilterSelectComponent),
            multi: true
        },
        {
            provide: ErrorStateMatcher,
            useExisting: forwardRef(() => FilterSelectComponent)
        }
    ],
    standalone: false
})
export class FilterSelectComponent extends AutocompleteBaseDirective<Filter, string>
    implements ControlValueAccessor, OnInit, ErrorStateMatcher {

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

  @Input()
  @coerceBoolean()
  tbRequired: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('filterInput', {static: true}) filterInput: ElementRef;

  @ViewChild('filterInput', {read: MatAutocompleteTrigger}) autocompleteTrigger: MatAutocompleteTrigger;

  filteredFilters: Observable<Array<Filter>>;

  private filterList: Array<Filter> = [];
  private propagateChange = (_v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.selectFilterFormGroup = this.fb.group({
      filter: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  isErrorState(control: AbstractControl | null, form: any): boolean {
    const submitted = form?.submitted ?? false;
    return !!(control?.invalid && (control?.dirty || control?.touched || submitted));
  }

  protected getControl(): FormControl {
    return this.selectFilterFormGroup.get('filter') as FormControl;
  }

  protected getAutocompleteTrigger(): MatAutocompleteTrigger {
    return this.autocompleteTrigger;
  }

  protected getInput(): ElementRef<HTMLInputElement> {
    return this.filterInput as ElementRef<HTMLInputElement>;
  }

  protected getFilteredEntities(): Observable<Array<Filter>> {
    return this.filteredFilters;
  }

  protected getModelValue(): string | null {
    return this.modelValue;
  }

  protected isCreateNew(): boolean {
    return true;
  }

  protected getDisplayName(entity: Filter): string {
    return entity.filter ?? '';
  }

  ngOnInit() {
    const filterControl = this.selectFilterFormGroup.get('filter');
    if (this.tbRequired) {
      filterControl.addValidators(Validators.required);
      filterControl.updateValueAndValidity({ emitEvent: false });
    }

    this.filteredFilters = filterControl.valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: string;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.filter) : ''),
        switchMap(name => {
          this.isFetching = true;
          return this.fetchFilters(name).pipe(
            finalize(() => this.isFetching = false)
          );
        }),
        tap(entities => {
          if (this.pendingBlur) {
            this.performValidation(entities);
          }
        }),
        shareReplay(1)
      );

    this.aliasController.filtersChanged.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadFilters();
    });
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

  protected updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayFilterFn(filter?: Filter): string | undefined {
    return filter ? filter.filter : undefined;
  }

  fetchFilters(searchText?: string): Observable<Array<Filter>> {
    this.searchText = searchText ?? '';
    let result = this.filterList;
    if (searchText && searchText.length) {
      result = this.filterList.filter((filter) => filter.filter.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
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

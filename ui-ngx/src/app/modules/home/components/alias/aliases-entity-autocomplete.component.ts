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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityInfo } from '@shared/models/entity.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { EntityService } from '@core/http/entity.service';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-aliases-entity-autocomplete',
  templateUrl: './aliases-entity-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AliasesEntityAutocompleteComponent),
    multi: true
  }]
})
export class AliasesEntityAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectEntityInfoFormGroup: UntypedFormGroup;

  modelValue: EntityInfo | null;

  @Input()
  alias: string;

  @Input()
  entityFilter: EntityFilter;

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

  @ViewChild('entityInfoInput', {static: true}) entityInfoInput: ElementRef;

  filteredEntityInfos: Observable<Array<EntityInfo>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: UntypedFormBuilder) {
    this.selectEntityInfoFormGroup = this.fb.group({
      entityInfo: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntityInfos = this.selectEntityInfoFormGroup.get('entityInfo').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchEntityInfos(name)),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: EntityInfo | null): void {
    this.searchText = '';
    if (isDefinedAndNotNull(value)) {
      this.modelValue = value;
      this.selectEntityInfoFormGroup.get('entityInfo').patchValue(value, {emitEvent: true});
    } else {
      this.modelValue = null;
      this.selectEntityInfoFormGroup.get('entityInfo').patchValue(null, {emitEvent: false});
    }
  }

  updateView(value: EntityInfo | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityInfoFn(entityInfo?: EntityInfo): string | undefined {
    return entityInfo ? entityInfo.name : undefined;
  }

  fetchEntityInfos(searchText?: string): Observable<Array<EntityInfo>> {
    this.searchText = searchText;
    return this.getEntityInfos(this.searchText).pipe(
      map(pageData => {
        return pageData.data;
      })
    );
  }

  getEntityInfos(searchText: string): Observable<PageData<EntityInfo>> {
    return this.entityService.findEntityInfosByFilterAndName(this.entityFilter, searchText, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<EntityInfo>()))
    );
  }

  clear() {
    this.selectEntityInfoFormGroup.get('entityInfo').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.entityInfoInput.nativeElement.blur();
      this.entityInfoInput.nativeElement.focus();
    }, 0);
  }

}

///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, SkipSelf, ViewChild} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NG_VALUE_ACCESSOR, NgForm
} from '@angular/forms';
import {Observable} from 'rxjs';
import {map, mergeMap, startWith, tap, share, pairwise, filter} from 'rxjs/operators';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {AliasEntityType, EntityType} from '@shared/models/entity-type.models';
import {BaseData} from '@shared/models/base-data';
import {EntityId} from '@shared/models/id/entity-id';
import {EntityService} from '@core/http/entity.service';
import {ErrorStateMatcher, MatAutocomplete, MatAutocompleteSelectedEvent, MatChipList} from '@angular/material';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-entity-list',
  templateUrl: './entity-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityListComponent),
      multi: true
    }
  ]
})
export class EntityListComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entityListFormGroup: FormGroup;

  modelValue: Array<string> | null;

  entityTypeValue: EntityType;

  @Input()
  set entityType(entityType: EntityType) {
    if (this.entityTypeValue !== entityType) {
      this.entityTypeValue = entityType;
      this.reset();
    }
  }

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

  @ViewChild('entityInput', {static: false}) entityInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityAutocomplete', {static: false}) matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: false}) chipList: MatChipList;

  entities: Array<BaseData<EntityId>> = [];
  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  private searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    this.entityListFormGroup = this.fb.group({
      entity: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntities = this.entityListFormGroup.get('entity').valueChanges
    .pipe(
      startWith<string | BaseData<EntityId>>(''),
      tap((value) => {
        if (value && typeof value !== 'string') {
          this.add(value);
        } else if (value === null) {
          this.clear(this.entityInput.nativeElement.value);
        }
      }),
      filter((value) => typeof value === 'string'),
      map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
      mergeMap(name => this.fetchEntities(name) ),
      share()
    );
  }

  ngAfterViewInit(): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null) {
      this.modelValue = value;
      this.entityService.getEntities(this.entityTypeValue, value).subscribe(
        (entities) => {
          this.entities = entities;
        }
      );
    } else {
      this.entities = [];
      this.modelValue = null;
    }
  }

  reset() {
    this.entities = [];
    this.modelValue = null;
    this.entityListFormGroup.get('entity').patchValue('', {emitEvent: true});
    this.propagateChange(this.modelValue);
  }

  add(entity: BaseData<EntityId>): void {
    if (!this.modelValue || this.modelValue.indexOf(entity.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entity.id.id);
      this.entities.push(entity);
      if (this.required) {
        this.chipList.errorState = false;
      }
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entity: BaseData<EntityId>) {
    const index = this.entities.indexOf(entity);
    if (index >= 0) {
      this.entities.splice(index, 1);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
        if (this.required) {
          this.chipList.errorState = true;
        }
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  displayEntityFn(entity?: BaseData<EntityId>): string | undefined {
    return entity ? entity.name : undefined;
  }

  fetchEntities(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;
    return this.entityService.getEntitiesByNameFilter(this.entityTypeValue, searchText,
      50, '', false, true).pipe(
      map((data) => data ? data : []));
  }

  clear(value: string = '') {
    this.entityInput.nativeElement.value = value;
    this.entityListFormGroup.get('entity').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityInput.nativeElement.blur();
      this.entityInput.nativeElement.focus();
    }, 0);
  }

}

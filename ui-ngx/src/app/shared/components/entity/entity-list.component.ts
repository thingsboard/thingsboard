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

import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipList } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-entity-list',
  templateUrl: './entity-list.component.html',
  styleUrls: ['./entity-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityListComponent),
      multi: true
    }
  ]
})
export class EntityListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  entityListFormGroup: FormGroup;

  modelValue: Array<string> | null;

  @Input()
  entityType: EntityType;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  @ViewChild('entityInput') entityInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipList;

  entities: Array<BaseData<EntityId>> = [];
  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    this.entityListFormGroup = this.fb.group({
      entities: [this.entities, this.required ? [Validators.required] : []],
      entity: [null]
    });
  }

  updateValidators() {
    this.entityListFormGroup.get('entities').setValidators(this.required ? [Validators.required] : []);
    this.entityListFormGroup.get('entities').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntities = this.entityListFormGroup.get('entity').valueChanges
    .pipe(
      // startWith<string | BaseData<EntityId>>(''),
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

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'entityType') {
          this.reset();
        }
      }
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityListFormGroup.disable({emitEvent: false});
    } else {
      this.entityListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.entityService.getEntities(this.entityType, value).subscribe(
        (entities) => {
          this.entities = entities;
          this.entityListFormGroup.get('entities').setValue(this.entities);
        }
      );
    } else {
      this.entities = [];
      this.entityListFormGroup.get('entities').setValue(this.entities);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.entities = [];
    this.entityListFormGroup.get('entities').setValue(this.entities);
    this.modelValue = null;
    if (this.entityInput) {
      this.entityInput.nativeElement.value = '';
    }
    this.entityListFormGroup.get('entity').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
  }

  add(entity: BaseData<EntityId>): void {
    if (!this.modelValue || this.modelValue.indexOf(entity.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entity.id.id);
      this.entities.push(entity);
      this.entityListFormGroup.get('entities').setValue(this.entities);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entity: BaseData<EntityId>) {
    let index = this.entities.indexOf(entity);
    if (index >= 0) {
      this.entities.splice(index, 1);
      this.entityListFormGroup.get('entities').setValue(this.entities);
      index = this.modelValue.indexOf(entity.id.id);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
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
    return this.entityService.getEntitiesByNameFilter(this.entityType, searchText,
      50, '', {ignoreLoading: true}).pipe(
      map((data) => data ? data : []));
  }

  onFocus() {
    if (this.dirty) {
      this.entityListFormGroup.get('entity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
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

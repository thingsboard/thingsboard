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

import {
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Observable } from 'rxjs';
import { filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData, getEntityDisplayName } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { isArray } from 'lodash';

@Component({
  selector: 'tb-entity-list',
  templateUrl: './entity-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EntityListComponent),
      multi: true
    }
  ]
})
export class EntityListComponent implements ControlValueAccessor, OnInit, OnChanges {

  entityListFormGroup: UntypedFormGroup;

  private modelValue: Array<string> | null;

  @Input()
  entityType: EntityType;

  @Input()
  subType: string;

  @Input()
  labelText: string;

  @Input()
  placeholderText = this.translate.instant('entity.entity-list');

  @Input()
  requiredText = this.translate.instant('entity.entity-list-empty');

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

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
  @coerceBoolean()
  disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  hint: string;

  @Input()
  @coerceBoolean()
  syncIdsWithDB = false;

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  @coerceBoolean()
  allowCreateNew: boolean;

  @Input()
  @coerceBoolean()
  useEntityDisplayName = false;

  @Output()
  createNew = new EventEmitter<string>();

  @ViewChild('entityInput') entityInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entities: Array<BaseData<EntityId>> = [];
  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_v: any) => { };

  constructor(private translate: TranslateService,
              private entityService: EntityService,
              private fb: UntypedFormBuilder) {
    this.entityListFormGroup = this.fb.group({
      entities: [this.entities],
      entity: [null]
    });
  }

  private updateValidators() {
    this.entityListFormGroup.get('entities').setValidators(this.required ? [Validators.required] : []);
    this.entityListFormGroup.get('entities').updateValueAndValidity();
  }

  createNewEntity($event: Event, searchText?: string) {
    $event.stopPropagation();
    this.createNew.emit(searchText);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
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
      map((value) => value ? value : ''),
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
      this.entityService.getEntities(this.entityType, value)
        .subscribe(resolvedEntities => {
          this.entities = resolvedEntities;
          this.entityListFormGroup.get('entities').setValue(this.entities);
          if (this.syncIdsWithDB && this.modelValue.length !== this.entities.length) {
            this.modelValue = this.entities.map(entity => entity.id.id);
            if (!this.modelValue.length) {
              this.modelValue = null;
            }
            this.propagateChange(this.modelValue);
          }
        });
    } else {
      this.entities = [];
      this.entityListFormGroup.get('entities').setValue(this.entities);
      this.modelValue = null;
    }
    this.dirty = true;
    if (this.entityInput) {
      this.entityInput.nativeElement.value = '';
    }
  }

  validate(): ValidationErrors | null {
    return (isArray(this.modelValue) && this.modelValue.length) || !this.required ? null : {
      entities: {valid: false}
    };
  }

  private reset() {
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

  private add(entity: BaseData<EntityId>): void {
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

  public remove(entity: BaseData<EntityId>) {
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

  public displayEntityFn(entity?: BaseData<EntityId>): string | undefined {
    return entity ? (this.useEntityDisplayName ? getEntityDisplayName(entity) : entity.name) : undefined;
  }

  private fetchEntities(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;

    return this.entityService.getEntitiesByNameFilter(this.entityType, searchText,
      50, this.subType ? this.subType : '', {ignoreLoading: true}).pipe(
      map((data) => data ? data : []));
  }

  public onFocus() {
    if (this.dirty) {
      this.entityListFormGroup.get('entity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private clear(value: string = '') {
    this.entityInput.nativeElement.value = value;
    this.entityListFormGroup.get('entity').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityInput.nativeElement.blur();
      this.entityInput.nativeElement.focus();
    }, 0);
  }

  public textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }
}

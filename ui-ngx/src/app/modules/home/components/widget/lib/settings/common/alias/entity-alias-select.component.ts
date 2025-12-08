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
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  NgForm,
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EntityAlias } from '@shared/models/alias.models';
import { IAliasController } from '@core/api/widget-api.models';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { EntityAliasSelectCallbacks } from './entity-alias-select.component.models';
import { ENTER } from '@angular/cdk/keycodes';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-entity-alias-select',
  templateUrl: './entity-alias-select.component.html',
  styleUrls: ['./entity-alias-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityAliasSelectComponent),
    multi: true
  }]
})
export class EntityAliasSelectComponent implements ControlValueAccessor, OnInit, ErrorStateMatcher {

  selectEntityAliasFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  aliasController: IAliasController;

  @Input()
  allowedEntityTypes: Array<EntityType>;

  @Input()
  callbacks: EntityAliasSelectCallbacks;

  @Input()
  @coerceBoolean()
  showLabel: boolean;

  @ViewChild('entityAliasAutocomplete') entityAliasAutocomplete: MatAutocomplete;

  @Input()
  @coerceBoolean()
  tbRequired: boolean;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @ViewChild('entityAliasInput', {static: true}) entityAliasInput: ElementRef;

  filteredEntityAliases: Observable<Array<EntityAlias>>;

  searchText = '';

  private dirty = false;
  private entityAliasList: Array<EntityAlias> = [];
  private propagateChange = (_v: any) => { };

  constructor(@SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private entityService: EntityService,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    this.selectEntityAliasFormGroup = this.fb.group({
      entityAlias: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.loadEntityAliases();

    this.filteredEntityAliases = this.selectEntityAliasFormGroup.get('entityAlias').valueChanges
      .pipe(
        tap(value => {
          let modelValue: EntityAlias;
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
        map(value => value ? (typeof value === 'string' ? value : value.alias) : ''),
        mergeMap(name => this.fetchEntityAliases(name) ),
        share()
      );

    this.aliasController.entityAliasesChanged.pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      this.loadEntityAliases();
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = this.tbRequired && !this.modelValue;
    return originalErrorState || customErrorState;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectEntityAliasFormGroup.disable({emitEvent: false});
    } else {
      this.selectEntityAliasFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    let entityAlias = null;
    if (value != null) {
      const entityAliases = this.aliasController.getEntityAliases();
      if (entityAliases[value]) {
        entityAlias = entityAliases[value];
      }
    }
    if (entityAlias != null) {
      this.modelValue = entityAlias.id;
      this.selectEntityAliasFormGroup.get('entityAlias').patchValue(entityAlias, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.selectEntityAliasFormGroup.get('entityAlias').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectEntityAliasFormGroup.get('entityAlias').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: EntityAlias | null) {
    const aliasId = value ? value.id : null;
    if (this.modelValue !== aliasId) {
      this.modelValue = aliasId;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityAliasFn(entityAlias?: EntityAlias): string | undefined {
    return entityAlias ? entityAlias.alias : undefined;
  }

  fetchEntityAliases(searchText?: string): Observable<Array<EntityAlias>> {
    this.searchText = searchText;
    let result = this.entityAliasList;
    if (searchText && searchText.length) {
      result = this.entityAliasList.filter((entityAlias) => entityAlias.alias.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  clear(value: string = '') {
    this.entityAliasInput.nativeElement.value = value;
    this.selectEntityAliasFormGroup.get('entityAlias').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityAliasInput.nativeElement.blur();
      this.entityAliasInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return text?.length > 0;
  }

  entityAliasEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createEntityAlias($event, this.searchText);
      }
    }
  }

  editEntityAlias($event: Event) {
    $event.preventDefault();
    $event.stopPropagation();
    if (this.callbacks && this.callbacks.editEntityAlias) {
      this.callbacks.editEntityAlias(this.selectEntityAliasFormGroup.get('entityAlias').value,
        this.allowedEntityTypes).subscribe((alias) => {
        if (alias) {
          this.modelValue = alias.id;
          this.selectEntityAliasFormGroup.get('entityAlias').patchValue(alias, {emitEvent: true});
        }
      });
    }
  }

  createEntityAlias($event: Event, alias: string, focusOnCancel = true) {
    $event.preventDefault();
    $event.stopPropagation();
    if (this.callbacks && this.callbacks.createEntityAlias) {
      this.callbacks.createEntityAlias(alias, this.allowedEntityTypes).subscribe((newAlias) => {
          if (!newAlias) {
            if (focusOnCancel) {
              setTimeout(() => {
                this.entityAliasInput.nativeElement.blur();
                this.entityAliasInput.nativeElement.focus();
              }, 0);
            }
          } else {
            this.modelValue = newAlias.id;
            this.selectEntityAliasFormGroup.get('entityAlias').patchValue(newAlias, {emitEvent: true});
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }

  private loadEntityAliases(): void {
    this.entityAliasList = [];
    const entityAliases = this.aliasController.getEntityAliases();
    for (const aliasId of Object.keys(entityAliases)) {
      if (this.allowedEntityTypes?.length) {
        if (!this.entityService.filterAliasByEntityTypes(entityAliases[aliasId], this.allowedEntityTypes)) {
          continue;
        }
      }
      this.entityAliasList.push(entityAliases[aliasId]);
    }
    this.dirty = true;
  }
}

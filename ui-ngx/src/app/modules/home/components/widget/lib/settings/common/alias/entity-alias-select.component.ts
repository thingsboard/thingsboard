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
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Observable, of, shareReplay } from 'rxjs';
import { debounceTime, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EntityAlias } from '@shared/models/alias.models';
import { IAliasController } from '@core/api/widget-api.models';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { EntityAliasSelectCallbacks } from './entity-alias-select.component.models';
import { ENTER } from '@angular/cdk/keycodes';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { objectRequired } from '@core/utils';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';

@Component({
    selector: 'tb-entity-alias-select',
    templateUrl: './entity-alias-select.component.html',
    styleUrls: ['./entity-alias-select.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityAliasSelectComponent),
            multi: true
        }],
    standalone: false
})
export class EntityAliasSelectComponent extends AutocompleteBaseDirective<EntityAlias, string>
    implements ControlValueAccessor, OnInit {

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

  @ViewChild('entityAliasInput', {static: true}) entityAliasInput: ElementRef;

  @ViewChild('entityAliasInput', {read: MatAutocompleteTrigger}) autocompleteTrigger: MatAutocompleteTrigger;

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

  filteredEntityAliases: Observable<Array<EntityAlias>>;

  private entityAliasList: Array<EntityAlias> = [];
  private propagateChange = (_v: any) => { };

  constructor(private entityService: EntityService,
              private fb: FormBuilder) {
    super();
    this.selectEntityAliasFormGroup = this.fb.group({
      entityAlias: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  protected getControl(): FormControl {
    return this.selectEntityAliasFormGroup.get('entityAlias') as FormControl;
  }

  protected getAutocompleteTrigger(): MatAutocompleteTrigger {
    return this.autocompleteTrigger;
  }

  protected getInput(): ElementRef<HTMLInputElement> {
    return this.entityAliasInput as ElementRef<HTMLInputElement>;
  }

  protected getFilteredEntities(): Observable<Array<EntityAlias>> {
    return this.filteredEntityAliases;
  }

  protected getModelValue(): string | null {
    return this.modelValue;
  }

  protected isCreateNew(): boolean {
    return false;
  }

  protected getDisplayName(entity: EntityAlias): string {
    return entity.alias ?? '';
  }

  ngOnInit() {
    const aliasControl = this.selectEntityAliasFormGroup.get('entityAlias');
    if (this.tbRequired) {
      aliasControl.addValidators(Validators.required);
      aliasControl.updateValueAndValidity({ emitEvent: false });
    }

    this.filteredEntityAliases = aliasControl.valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: string | null;
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
        map(value => value ? (typeof value === 'string' ? value : value.alias) : ''),
        switchMap(name => {
          this.isFetching = true;
          return this.fetchEntityAliases(name).pipe(
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

    this.aliasController.entityAliasesChanged.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadEntityAliases();
    });

    this.loadEntityAliases();
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

  protected updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityAliasFn(entityAlias?: EntityAlias): string | undefined {
    return entityAlias ? entityAlias.alias : undefined;
  }

  fetchEntityAliases(searchText?: string): Observable<Array<EntityAlias>> {
    this.searchText = searchText ?? '';
    let result = this.entityAliasList;
    if (searchText && searchText.length) {
      result = this.entityAliasList.filter((entityAlias) => entityAlias.alias.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
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

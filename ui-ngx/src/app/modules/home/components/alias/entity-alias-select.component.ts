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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, SkipSelf, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  NgForm
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityAlias } from '@shared/models/alias.models';
import { IAliasController } from '@core/api/widget-api.models';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatAutocomplete, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { EntityAliasSelectCallbacks } from './entity-alias-select.component.models';
import { ENTER } from '@angular/cdk/keycodes';
import { ErrorStateMatcher } from '@angular/material/core';

@Component({
  selector: 'tb-entity-alias-select',
  templateUrl: './entity-alias-select.component.html',
  styleUrls: ['./entity-alias-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityAliasSelectComponent),
    multi: true
  },
  {
    provide: ErrorStateMatcher,
    useExisting: EntityAliasSelectComponent
  }]
})
export class EntityAliasSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit, ErrorStateMatcher {

  selectEntityAliasFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  aliasController: IAliasController;

  @Input()
  allowedEntityTypes: Array<EntityType>;

  @Input()
  callbacks: EntityAliasSelectCallbacks;

  @Input()
  showLabel: boolean;

  @ViewChild('entityAliasAutocomplete') entityAliasAutocomplete: MatAutocomplete;
  @ViewChild('autocomplete', { read: MatAutocompleteTrigger }) autoCompleteTrigger: MatAutocompleteTrigger;


  private requiredValue: boolean;
  get tbRequired(): boolean {
    return this.requiredValue;
  }
  @Input()
  set tbRequired(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @ViewChild('entityAliasInput', {static: true}) entityAliasInput: ElementRef;

  entityAliasList: Array<EntityAlias> = [];

  filteredEntityAliases: Observable<Array<EntityAlias>>;

  searchText = '';

  private dirty = false;

  private creatingEntityAlias = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private entityService: EntityService,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private fb: FormBuilder) {
    this.selectEntityAliasFormGroup = this.fb.group({
      entityAlias: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    const entityAliases = this.aliasController.getEntityAliases();
    for (const aliasId of Object.keys(entityAliases)) {
      if (this.allowedEntityTypes && this.allowedEntityTypes.length) {
        if (!this.entityService.filterAliasByEntityTypes(entityAliases[aliasId], this.allowedEntityTypes)) {
          continue;
        }
      }
      this.entityAliasList.push(entityAliases[aliasId]);
    }

    this.filteredEntityAliases = this.selectEntityAliasFormGroup.get('entityAlias').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
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
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = this.tbRequired && !this.modelValue;
    return originalErrorState || customErrorState;
  }

  ngAfterViewInit(): void {}

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
    return (text && text != null && text.length > 0) ? true : false;
  }

  entityAliasEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createEntityAlias($event, this.searchText);
      }
    }
  }

  createEntityAlias($event: Event, alias: string) {
    $event.preventDefault();
    this.creatingEntityAlias = true;
    if (this.callbacks && this.callbacks.createEntityAlias) {
      this.callbacks.createEntityAlias(alias, this.allowedEntityTypes).subscribe((newAlias) => {
          if (!newAlias) {
            setTimeout(() => {
              this.entityAliasInput.nativeElement.blur();
              this.entityAliasInput.nativeElement.focus();
            }, 0);
          } else {
            this.entityAliasList.push(newAlias);
            this.modelValue = newAlias.id;
            this.selectEntityAliasFormGroup.get('entityAlias').patchValue(newAlias, {emitEvent: true});
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }
}

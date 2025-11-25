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
  effect,
  ElementRef,
  forwardRef,
  Input,
  input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs/operators';
import { combineLatest, of, Subject } from 'rxjs';
import { EntityService } from '@core/http/entity.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntitiesKeysByQuery } from '@shared/models/entity.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { isEqual } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-entity-key-autocomplete',
  templateUrl: './entity-key-autocomplete.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityKeyAutocompleteComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EntityKeyAutocompleteComponent),
      multi: true
    }
  ],
})
export class EntityKeyAutocompleteComponent implements ControlValueAccessor, Validator, OnChanges {

  @ViewChild('keyInput', {static: true}) keyInput: ElementRef;

  @Input() placeholder = this.translate.instant('action.set');
  @Input() requiredText = this.translate.instant('common.hint.key-required');

  entityFilter = input.required<EntityFilter>();
  dataKeyType = input.required<DataKeyType>();
  keyScopeType = input<AttributeScope>();

  keyControl = this.fb.control('', [Validators.required]);
  searchText = '';
  keyInputSubject = new Subject<void>();

  private propagateChange: (value: string) => void;
  private cachedResult: EntitiesKeysByQuery;

  keys$ = this.keyInputSubject.asObservable()
    .pipe(
      switchMap(() => {
        return this.cachedResult ? of(this.cachedResult) : this.entityService.findEntityKeysByQuery({
          pageLink: { page: 0, pageSize: 100 },
          entityFilter: this.entityFilter(),
        }, this.dataKeyType() === DataKeyType.attribute, this.dataKeyType() === DataKeyType.timeseries, this.keyScopeType(), {ignoreLoading: true});
      }),
      map(result => {
        this.cachedResult = result;
        switch (this.dataKeyType()) {
          case DataKeyType.attribute:
            return result.attribute;
          case DataKeyType.timeseries:
            return result.timeseries;
          default:
            return [];
        }
      }),
    );

  filteredKeys$ = combineLatest([this.keys$, this.keyControl.valueChanges.pipe(startWith(''))])
    .pipe(
      map(([keys, searchText = '']) => {
        this.searchText = searchText;
        return searchText ? keys.filter(item => item.toLowerCase().includes(searchText.toLowerCase())) : keys;
      })
    );

  constructor(
    private fb: FormBuilder,
    private entityService: EntityService,
    private translate: TranslateService,
  ) {
    this.keyControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.propagateChange(value));
    effect(() => {
      if (this.keyScopeType() || this.entityFilter() && this.dataKeyType()) {
        this.cachedResult = null;
        this.searchText = '';
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    const filterChanged = changes.entityFilter?.previousValue &&
      !isEqual(changes.entityFilter.currentValue, changes.entityFilter.previousValue);
    const keyScopeChanged = changes.keyScopeType?.previousValue &&
      changes.keyScopeType.currentValue !== changes.keyScopeType.previousValue;
    const keyTypeChanged = changes.dataKeyType?.previousValue &&
      changes.dataKeyType.currentValue !== changes.dataKeyType.previousValue;

    if (filterChanged || keyScopeChanged || keyTypeChanged) {
      this.keyControl.setValue('', {emitEvent: false});
      this.cachedResult = null;
    }
  }

  clear(): void {
    this.keyControl.patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  registerOnChange(onChange: (value: string) => void): void {
    this.propagateChange = onChange;
  }

  registerOnTouched(_): void {}

  validate(): ValidationErrors | null {
    return this.keyControl.valid || this.keyControl.disabled ? null : { keyControl: false };
  }

  writeValue(value: string): void {
    this.keyControl.patchValue(value, {emitEvent: false});
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.keyControl.disable({emitEvent: false});
    } else {
      this.keyControl.enable({emitEvent: false});
    }
  }
}

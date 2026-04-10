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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, ReplaySubject, Subscription, throwError } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  switchMap,
  tap,
  share,
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@app/shared/models/entity-type.models';
import { BroadcastService } from '@app/core/services/broadcast.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { EntityService } from '@core/http/entity.service';

@Component({
    selector: 'tb-entity-subtype-autocomplete',
    templateUrl: './entity-subtype-autocomplete.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntitySubTypeAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class EntitySubTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  subTypeFormGroup: UntypedFormGroup;

  modelValue: string | null;

  @Input()
  entityType: EntityType;

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

  @Input()
  excludeSubTypes: Array<string>;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @ViewChild('subTypeInput', {static: true}) subTypeInput: ElementRef;

  selectEntitySubtypeText: string;
  entitySubtypeText: string;
  entitySubtypeRequiredText: string;
  entitySubtypeMaxLength: string;

  filteredSubTypes: Observable<Array<string>>;

  subTypes: Observable<Array<string>>;

  private broadcastSubscription: Subscription;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              public translate: TranslateService,
              private fb: UntypedFormBuilder,
              private entityService: EntityService) {
    this.subTypeFormGroup = this.fb.group({
      subType: [null, Validators.maxLength(255)]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    switch (this.entityType) {
      case EntityType.ASSET:
        this.selectEntitySubtypeText = 'asset.select-asset-type';
        this.entitySubtypeText = 'asset.asset-type';
        this.entitySubtypeRequiredText = 'asset.asset-type-required';
        this.entitySubtypeMaxLength = 'asset.asset-type-max-length';
        this.broadcastSubscription = this.broadcast.on('assetSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.DEVICE:
        this.selectEntitySubtypeText = 'device.select-device-type';
        this.entitySubtypeText = 'device.device-type';
        this.entitySubtypeRequiredText = 'device.device-type-required';
        this.entitySubtypeMaxLength = 'device.device-type-max-length';
        this.broadcastSubscription = this.broadcast.on('deviceSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.EDGE:
        this.selectEntitySubtypeText = 'edge.select-edge-type';
        this.entitySubtypeText = 'edge.edge-type';
        this.entitySubtypeRequiredText = 'edge.edge-type-required';
        this.entitySubtypeMaxLength = 'edge.type-max-length';
        this.broadcastSubscription = this.broadcast.on('edgeSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.ENTITY_VIEW:
        this.selectEntitySubtypeText = 'entity-view.select-entity-view-type';
        this.entitySubtypeText = 'entity-view.entity-view-type';
        this.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
        this.entitySubtypeMaxLength = 'entity-view.type-max-length'
        this.broadcastSubscription = this.broadcast.on('entityViewSaved', () => {
          this.subTypes = null;
        });
        break;
    }

    this.filteredSubTypes = this.subTypeFormGroup.get('subType').valueChanges
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(value => {
          this.updateView(value);
        }),
        // startWith<string | EntitySubtype>(''),
        map(value => value ? value : ''),
        switchMap(type => this.fetchSubTypes(type))
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    if (this.broadcastSubscription) {
      this.broadcastSubscription.unsubscribe();
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.subTypeFormGroup.disable({emitEvent: false});
    } else {
      this.subTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    this.subTypeFormGroup.get('subType').patchValue(value, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.subTypeFormGroup.get('subType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displaySubTypeFn(subType?: string): string | undefined {
    return subType ? subType : undefined;
  }

  fetchSubTypes(searchText?: string, strictMatch: boolean = false): Observable<Array<string>> {
    this.searchText = searchText;
    return this.getSubTypes().pipe(
      map(subTypes => subTypes.filter(subType => {
        if (strictMatch) {
          return searchText ? subType === searchText : false;
        } else {
          return searchText ? subType.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        }
      }))
    );
  }

  getSubTypes(): Observable<Array<string>> {
    if (!this.subTypes) {
      const subTypesObservable = this.entityService.getEntitySubtypesObservable(this.entityType);
      if (subTypesObservable) {
        const excludeSubTypesSet = new Set(this.excludeSubTypes);
        this.subTypes = subTypesObservable.pipe(
          catchError(() => of([] as Array<string>)),
          map(subTypes => {
            const filteredSubTypes: Array<string> = [];
            subTypes.forEach(subType => !excludeSubTypesSet.has(subType) && filteredSubTypes.push(subType));
            return filteredSubTypes;
          }),
          share({
            connector: () => new ReplaySubject(1),
            resetOnError: false,
            resetOnComplete: false,
            resetOnRefCountZero: false,
          })
        );
      } else {
        return throwError(null);
      }
    }
    return this.subTypes;
  }

  clear() {
    this.subTypeFormGroup.get('subType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.subTypeInput.nativeElement.blur();
      this.subTypeInput.nativeElement.focus();
    }, 0);
  }

}

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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { filter, map, mergeMap, publishReplay, refCount, tap } from 'rxjs/operators';
import { ObjectLwM2M } from './profile-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { Direction } from '@shared/models/page/sort-order';
import { isDefined, isDefinedAndNotNull, isEmptyStr, isString } from '@core/utils';

@Component({
  selector: 'tb-profile-lwm2m-object-list',
  templateUrl: './lwm2m-object-list.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObjectListComponent),
      multi: true
    }]
})
export class Lwm2mObjectListComponent implements ControlValueAccessor, OnInit, Validators {

  private requiredValue: boolean;
  private dirty = false;
  private lw2mModels: Observable<Array<ObjectLwM2M>>;
  private modelValue: Array<number> = [];

  lwm2mListFormGroup: FormGroup;
  objectsList: Array<ObjectLwM2M> = [];
  filteredObjectsList: Observable<Array<ObjectLwM2M>>;
  disabled = false;
  searchText = '';

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
    this.updateValidators();
  }

  @Output()
  addList = new EventEmitter<any>();

  @Output()
  removeList = new EventEmitter<any>();

  @ViewChild('objectInput') objectInput: ElementRef<HTMLInputElement>;

  private propagateChange = (v: any) => {
  }

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    this.lwm2mListFormGroup = this.fb.group({
      objectsList: [this.objectsList],
      objectLwm2m: ['']
    });
  }

  private updateValidators = (): void => {
    this.lwm2mListFormGroup.get('objectLwm2m').setValidators(this.required ? [Validators.required] : []);
    this.lwm2mListFormGroup.get('objectLwm2m').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredObjectsList = this.lwm2mListFormGroup.get('objectLwm2m').valueChanges
      .pipe(
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear();
          }
        }),
        filter(searchText => isString(searchText)),
        mergeMap(searchText => this.fetchListObjects(searchText))
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lwm2mListFormGroup.disable({emitEvent: false});
      if (isDefined(this.objectInput)) {
        this.clear();
      }
    } else {
      this.lwm2mListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: any): void {
    this.searchText = '';
    if (isDefinedAndNotNull(value)) {
      if (Array.isArray(value.objectIds)) {
        this.modelValue = value.objectIds;
        this.objectsList = value.objectsList;
      } else {
        this.objectsList = [];
        this.modelValue = [];
      }
      this.lwm2mListFormGroup.get('objectsList').setValue(this.objectsList, {emitEvents: false});
      this.dirty = false;
    }
  }

  private add(object: ObjectLwM2M): void {
    if (this.modelValue.indexOf(object.id) === -1) {
      this.modelValue.push(object.id);
      this.objectsList.push(object);
      this.lwm2mListFormGroup.get('objectsList').setValue(this.objectsList);
      this.addList.next(this.objectsList);
    }
    this.clear();
  }

  remove = (object: ObjectLwM2M): void => {
    let index = this.objectsList.indexOf(object);
    if (index >= 0) {
      this.objectsList.splice(index, 1);
      this.lwm2mListFormGroup.get('objectsList').setValue(this.objectsList);
      index = this.modelValue.indexOf(object.id);
      this.modelValue.splice(index, 1);
      this.removeList.next(object);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.clear();
    }
  }

  displayObjectLwm2mFn = (object?: ObjectLwM2M): string | undefined => {
    return object ? object.name : undefined;
  }

  private fetchListObjects = (searchText?: string): Observable<Array<ObjectLwM2M>> => {
    this.searchText = searchText;
    const filters = {names: [], ids: []};
    if (isDefinedAndNotNull(searchText) && !isEmptyStr(searchText)) {
      const ids = searchText.match(/\d+/g);
      filters.ids = ids !== null ? ids.map(Number) : filters.ids;
      filters.names = searchText.trim().toUpperCase().split(' ');
    }
    const predicate = objectLwM2M => filters.names.find(word => objectLwM2M.name.toUpperCase().includes(word))
      || filters.ids.includes(objectLwM2M.id);
    return this.getLwM2mModels().pipe(
      map(objectLwM2Ms => searchText ? objectLwM2Ms.filter(predicate) : objectLwM2Ms)
    );
  }

  private getLwM2mModels(): Observable<Array<ObjectLwM2M>> {
    if (!this.lw2mModels) {
      const sortOrder = {
        property: 'name',
        direction: Direction.ASC
      };
      this.lw2mModels = this.deviceProfileService.getLwm2mObjects(sortOrder).pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.lw2mModels;
  }


  onFocus = (): void => {
    if (!this.dirty) {
      this.lwm2mListFormGroup.get('objectLwm2m').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = true;
    }
  }

  private clear = (value: string = ''): void => {
    this.objectInput.nativeElement.value = value;
    this.searchText = '';
    this.lwm2mListFormGroup.get('objectLwm2m').patchValue(value);
    setTimeout(() => {
      this.objectInput.nativeElement.blur();
      this.objectInput.nativeElement.focus();
    }, 0);
  }
}

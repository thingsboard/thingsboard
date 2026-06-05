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
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { isDefinedAndNotNull, isEmptyStr, isEqual, isObject } from '@core/utils';
import {
  extractParamsFromJSResourceUrl,
  isJSResource,
  prependTbResourcePrefix,
  removeTbResourcePrefix,
  ResourceInfo,
  ResourceSubType,
  ResourceType
} from '@shared/models/resource.models';
import { TbResourceId } from '@shared/models/id/tb-resource-id';
import { ResourceService } from '@core/http/resource.service';
import { PageLink } from '@shared/models/page/page-link';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';

@Component({
    selector: 'tb-resource-autocomplete',
    templateUrl: './resource-autocomplete.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ResourceAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class ResourceAutocompleteComponent implements ControlValueAccessor, OnInit {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  placeholder: string;

  @Input()
  @coerceBoolean()
  hideRequiredMarker = false;

  @Input()
  @coerceBoolean()
  allowAutocomplete = false;

  @Input()
  subType = ResourceSubType.EXTENSION;

  ResourceSubType = ResourceSubType;

  resourceFormGroup = this.fb.group({
    resource: this.fb.control<string|ResourceInfo>(null)
  });

  filteredResources$: Observable<Array<ResourceInfo>>;

  searchText = '';

  @ViewChild('resourceInput', {static: true}) resourceInput: ElementRef;

  resource: ResourceInfo;
  private modelValue: string;
  private dirty = false;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder,
              private resourceService: ResourceService) {
  }

  ngOnInit(): void {
    if(this.required) {
      this.resourceFormGroup.get('resource').setValidators(Validators.required);
      this.resourceFormGroup.get('resource').updateValueAndValidity({emitEvent: false});
    }
    this.filteredResources$ = this.resourceFormGroup.get('resource').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: string;
          if (isObject(value)) {
            this.resource = value as ResourceInfo;
            modelValue = prependTbResourcePrefix(this.resource.link);
          } else if (isEmptyStr(value) || this.subType !== ResourceSubType.EXTENSION) {
            this.resource = null;
            modelValue = null;
          } else {
            this.resource = null;
            modelValue = value as string;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.title) : ''),
        switchMap(name => this.fetchResources(name)),
        share()
      );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.resourceFormGroup.disable({emitEvent: false});
    } else {
      this.resourceFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | TbResourceId) {
    if (isDefinedAndNotNull(value)) {
      this.searchText = '';
      if (isObject(value) && typeof value !== 'string' && (value as TbResourceId).id) {
        this.resourceService.getResourceInfoById(value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: resource => {
            this.resource = resource;
            this.modelValue = prependTbResourcePrefix(resource.link);
            this.resourceFormGroup.get('resource').patchValue(resource, {emitEvent: false});
          },
          error: () => {
            this.resource = null;
            this.modelValue = '';
            this.resourceFormGroup.get('resource').patchValue('');
          }
        });
      } else if (typeof value === 'string' && isJSResource(value)) {
        const url = removeTbResourcePrefix(value);
        const params = extractParamsFromJSResourceUrl(url);
        this.resourceService.getResourceInfo(params.type, params.scope, params.key, {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: resource => {
            this.resource = resource;
            this.modelValue = value;
            this.resourceFormGroup.get('resource').patchValue(resource, {emitEvent: false});
          },
          error: () => {
            this.resource = null;
            this.modelValue = '';
            this.resourceFormGroup.get('resource').patchValue('');
          }
        })
      } else {
        this.modelValue = value as string;
        this.resourceFormGroup.get('resource').patchValue(value as string, {emitEvent: false});
      }
      this.dirty = true;
    }
  }

  displayResourceFn(resource?: ResourceInfo | string): string {
    return isObject(resource) ? (resource as ResourceInfo).title : resource as string;
  }

  clear() {
    this.resourceFormGroup.get('resource').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.resourceInput.nativeElement.blur();
      this.resourceInput.nativeElement.focus();
    }, 0);
  }

  onFocus() {
    if (this.dirty) {
      this.resourceFormGroup.get('resource').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private updateView(value: string) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  private fetchResources(searchText?: string): Observable<Array<ResourceInfo>> {
    this.searchText = searchText;
    return this.resourceService.getResources(new PageLink(50, 0, searchText), ResourceType.JS_MODULE, this.subType, {ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map(data => data.data)
    );
  }

}

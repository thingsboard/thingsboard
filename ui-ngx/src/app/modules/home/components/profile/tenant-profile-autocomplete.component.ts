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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TenantProfileId } from '@shared/models/id/tenant-profile-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { entityIdEquals } from '../../../../shared/models/id/entity-id';

@Component({
  selector: 'tb-tenant-profile-autocomplete',
  templateUrl: './tenant-profile-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TenantProfileAutocompleteComponent),
    multi: true
  }]
})
export class TenantProfileAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectTenantProfileFormGroup: FormGroup;

  modelValue: TenantProfileId | null;

  @Input()
  selectDefaultProfile = false;

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

  @ViewChild('tenantProfileInput', {static: true}) tenantProfileInput: ElementRef;

  filteredTenantProfiles: Observable<Array<EntityInfoData>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private tenantProfileService: TenantProfileService,
              private fb: FormBuilder) {
    this.selectTenantProfileFormGroup = this.fb.group({
      tenantProfile: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredTenantProfiles = this.selectTenantProfileFormGroup.get('tenantProfile').valueChanges
      .pipe(
        tap((value: EntityInfoData | string) => {
          let modelValue: TenantProfileId | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = new TenantProfileId(value.id.id);
          }
          this.updateView(modelValue);
        }),
        startWith<string | EntityInfoData>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchTenantProfiles(name) )
      );
  }

  selectDefaultTenantProfileIfNeeded(): void {
    if (this.selectDefaultProfile && !this.modelValue) {
      this.tenantProfileService.getDefaultTenantProfileInfo().subscribe(
        (profile) => {
          if (profile) {
            this.modelValue = new TenantProfileId(profile.id.id);
            this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(profile, {emitEvent: false});
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: TenantProfileId | null): void {
    this.searchText = '';
    if (value != null) {
      this.tenantProfileService.getTenantProfileInfo(value.id).subscribe(
        (profile) => {
          this.modelValue = new TenantProfileId(profile.id.id);
          this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(profile, {emitEvent: true});
        }
      );
    } else {
      this.modelValue = null;
      this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(null, {emitEvent: true});
      this.selectDefaultTenantProfileIfNeeded();
    }
  }

  updateView(value: TenantProfileId | null) {
    if (!entityIdEquals(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayTenantProfileFn(profile?: EntityInfoData): string | undefined {
    return profile ? profile.name : undefined;
  }

  fetchTenantProfiles(searchText?: string): Observable<Array<EntityInfoData>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.tenantProfileService.getTenantProfileInfos(pageLink, {ignoreLoading: true}).pipe(
      map(pageData => {
        return pageData.data;
      })
    );
  }

  clear() {
    this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.tenantProfileInput.nativeElement.blur();
      this.tenantProfileInput.nativeElement.focus();
    }, 0);
  }

}

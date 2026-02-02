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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TenantProfileId } from '@shared/models/id/tenant-profile-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TruncatePipe } from '@shared//pipe/truncate.pipe';
import { ENTER } from '@angular/cdk/keycodes';
import { TenantProfile } from '@shared/models/tenant.model';
import { MatDialog } from '@angular/material/dialog';
import { TenantProfileDialogComponent, TenantProfileDialogData } from './tenant-profile-dialog.component';
import { emptyPageData } from '@shared/models/page/page-data';
import { getEntityDetailsPageURL } from '@core/utils';

@Component({
  selector: 'tb-tenant-profile-autocomplete',
  templateUrl: './tenant-profile-autocomplete.component.html',
  styleUrls: ['./tenant-profile-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TenantProfileAutocompleteComponent),
    multi: true
  }]
})
export class TenantProfileAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectTenantProfileFormGroup: UntypedFormGroup;

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

  @Input()
  showDetailsPageLink = false;

  @Output()
  tenantProfileUpdated = new EventEmitter<TenantProfileId>();

  @ViewChild('tenantProfileInput', {static: true}) tenantProfileInput: ElementRef;

  filteredTenantProfiles: Observable<Array<EntityInfoData>>;

  searchText = '';
  tenantProfileURL: string;

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private tenantProfileService: TenantProfileService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
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
        debounceTime(150),
        tap((value: EntityInfoData | string) => {
          let modelValue: TenantProfileId | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = new TenantProfileId(value.id.id);
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchTenantProfiles(name)),
        share()
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
    if (this.disabled) {
      this.selectTenantProfileFormGroup.disable({emitEvent: false});
    } else {
      this.selectTenantProfileFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TenantProfileId | null): void {
    this.searchText = '';
    if (value != null) {
      this.tenantProfileService.getTenantProfileInfo(value.id).subscribe(
        (profile) => {
          this.modelValue = new TenantProfileId(profile.id.id);
          this.tenantProfileURL = getEntityDetailsPageURL(this.modelValue.id, this.modelValue.entityType);
          this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(profile, {emitEvent: false});
        }
      );
    } else {
      this.modelValue = null;
      this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(null, {emitEvent: false});
      this.selectDefaultTenantProfileIfNeeded();
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectTenantProfileFormGroup.get('tenantProfile').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
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
      catchError(() => of(emptyPageData<EntityInfoData>())),
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

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  tenantProfileEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createTenantProfile($event, this.searchText);
      }
    }
  }

  createTenantProfile($event: Event, profileName: string) {
    $event.preventDefault();
    $event.stopPropagation();
    const tenantProfile: TenantProfile = {
      name: profileName
    };
    this.openTenantProfileDialog(tenantProfile, true);
  }

  editTenantProfile($event: Event) {
    $event.preventDefault();
    this.tenantProfileService.getTenantProfile(this.modelValue.id).subscribe(
      (tenantProfile) => {
        this.openTenantProfileDialog(tenantProfile, false);
      }
    );
  }

  openTenantProfileDialog(tenantProfile: TenantProfile, isAdd: boolean) {
    this.dialog.open<TenantProfileDialogComponent, TenantProfileDialogData,
      TenantProfile>(TenantProfileDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        tenantProfile
      }
    }).afterClosed().subscribe(
      (savedTenantProfile) => {
        if (!savedTenantProfile) {
          setTimeout(() => {
            this.tenantProfileInput.nativeElement.blur();
            this.tenantProfileInput.nativeElement.focus();
          }, 0);
        } else {
          this.tenantProfileService.getTenantProfileInfo(savedTenantProfile.id.id).subscribe(
            (profile) => {
              this.modelValue = new TenantProfileId(profile.id.id);
              this.selectTenantProfileFormGroup.get('tenantProfile').patchValue(profile, {emitEvent: true});
              if (isAdd) {
                this.propagateChange(this.modelValue);
              } else {
                this.tenantProfileUpdated.next(savedTenantProfile.id);
              }
            }
          );
        }
      }
    );
  }
}

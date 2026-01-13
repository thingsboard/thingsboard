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

import {
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  NgZone,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TruncatePipe } from '@shared//pipe/truncate.pipe';
import { ENTER } from '@angular/cdk/keycodes';
import { MatDialog } from '@angular/material/dialog';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { emptyPageData } from '@shared/models/page/page-data';
import { getEntityDetailsPageURL } from '@core/utils';
import { AssetProfileId } from '@shared/models/id/asset-profile-id';
import { AssetProfile, AssetProfileInfo } from '@shared/models/asset.models';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { AssetProfileDialogComponent, AssetProfileDialogData } from './asset-profile-dialog.component';
import { SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-asset-profile-autocomplete',
  templateUrl: './asset-profile-autocomplete.component.html',
  styleUrls: ['./asset-profile-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AssetProfileAutocompleteComponent),
    multi: true
  }]
})
export class AssetProfileAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectAssetProfileFormGroup: UntypedFormGroup;

  modelValue: AssetProfileId | null;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  selectDefaultProfile = false;

  @Input()
  selectFirstProfile = false;

  @Input()
  displayAllOnEmpty = false;

  @Input()
  editProfileEnabled = true;

  @Input()
  addNewProfile = true;

  @Input()
  showDetailsPageLink = false;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  @Input()
  hint: string;

  @Output()
  assetProfileUpdated = new EventEmitter<AssetProfileId>();

  @Output()
  assetProfileChanged = new EventEmitter<AssetProfileInfo>();

  @ViewChild('assetProfileInput', {static: true}) assetProfileInput: ElementRef;

  @ViewChild('assetProfileAutocomplete', {static: true}) assetProfileAutocomplete: MatAutocomplete;

  filteredAssetProfiles: Observable<Array<AssetProfileInfo>>;

  searchText = '';
  assetProfileURL: string;

  useAssetProfileLink = true;

  private authUser: AuthUser;

  private dirty = false;

  private ignoreClosedPanel = false;

  private allAssetProfile: AssetProfileInfo = {
    name: this.translate.instant('asset-profile.all-asset-profiles'),
    id: null
  };

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private assetProfileService: AssetProfileService,
              private fb: UntypedFormBuilder,
              private zone: NgZone,
              private dialog: MatDialog) {
    this.authUser = getCurrentAuthUser(this.store);
    if (this.authUser.authority === Authority.CUSTOMER_USER) {
      this.useAssetProfileLink = false;
    }
    this.selectAssetProfileFormGroup = this.fb.group({
      assetProfile: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredAssetProfiles = this.selectAssetProfileFormGroup.get('assetProfile').valueChanges
      .pipe(
        tap((value: AssetProfileInfo | string) => {
          let modelValue: AssetProfileInfo | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          if (!this.displayAllOnEmpty || modelValue) {
            this.updateView(modelValue);
          }
        }),
        map(value => {
          if (value) {
            if (typeof value === 'string') {
              return value;
            } else {
              if (this.displayAllOnEmpty && value === this.allAssetProfile) {
                return '';
              } else {
                return value.name;
              }
            }
          } else {
            return '';
          }
        }),
        debounceTime(150),
        distinctUntilChanged(),
        switchMap(name => this.fetchAssetProfiles(name)),
        share()
      );
  }

  selectDefaultAssetProfileIfNeeded(): void {
    if (this.selectDefaultProfile && !this.modelValue) {
      this.assetProfileService.getDefaultAssetProfileInfo().subscribe(
        (profile) => {
          if (profile) {
            this.selectAssetProfileFormGroup.get('assetProfile').patchValue(profile, {emitEvent: false});
            this.updateView(profile);
          } else {
            this.selectFirstAssetProfileIfNeeded();
          }
        }
      );
    } else {
      this.selectFirstAssetProfileIfNeeded();
    }
  }

  selectFirstAssetProfileIfNeeded(): void {
    if (this.selectFirstProfile && !this.modelValue) {
      const pageLink = new PageLink(1, 0, null, {
        property: 'createdTime',
        direction: Direction.DESC
      });
      this.assetProfileService.getAssetProfileInfos(pageLink, {ignoreLoading: true}).subscribe(
        (pageData => {
          const data = pageData.data;
          if (data.length) {
            this.selectAssetProfileFormGroup.get('assetProfile').patchValue(data[0], {emitEvent: false});
            this.updateView(data[0]);
          }
        })
      );
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectAssetProfileFormGroup.disable({emitEvent: false});
    } else {
      this.selectAssetProfileFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AssetProfileId | null): void {
    this.searchText = '';
    if (value != null) {
      this.assetProfileService.getAssetProfileInfo(value.id).subscribe(
        (profile) => {
          this.modelValue = new AssetProfileId(profile.id.id);
          if (this.useAssetProfileLink) {
            this.assetProfileURL = getEntityDetailsPageURL(this.modelValue.id, this.modelValue.entityType);
          }
          this.selectAssetProfileFormGroup.get('assetProfile').patchValue(profile, {emitEvent: false});
          this.assetProfileChanged.emit(profile);
        }
      );
    } else if (this.displayAllOnEmpty) {
      this.modelValue = null;
      this.selectAssetProfileFormGroup.get('assetProfile').patchValue(this.allAssetProfile, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.selectAssetProfileFormGroup.get('assetProfile').patchValue(null, {emitEvent: false});
      this.selectDefaultAssetProfileIfNeeded();
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectAssetProfileFormGroup.get('assetProfile').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  onPanelClosed() {
    if (this.ignoreClosedPanel) {
      this.ignoreClosedPanel = false;
    } else {
      if (this.displayAllOnEmpty && !this.selectAssetProfileFormGroup.get('assetProfile').value) {
        this.zone.run(() => {
          this.selectAssetProfileFormGroup.get('assetProfile').patchValue(this.allAssetProfile, {emitEvent: true});
        }, 0);
      }
    }
  }

  updateView(assetProfile: AssetProfileInfo | null) {
    const idValue = assetProfile && assetProfile.id ? new AssetProfileId(assetProfile.id.id) : null;
    if (!entityIdEquals(this.modelValue, idValue)) {
      this.modelValue = idValue;
      this.propagateChange(this.modelValue);
      this.assetProfileChanged.emit(assetProfile);
    }
  }

  displayAssetProfileFn(profile?: AssetProfileInfo): string | undefined {
    return profile ? profile.name : undefined;
  }

  fetchAssetProfiles(searchText?: string): Observable<Array<AssetProfileInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.assetProfileService.getAssetProfileInfos(pageLink, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<AssetProfileInfo>())),
      map(pageData => {
        let data = pageData.data;
        if (this.displayAllOnEmpty) {
          data = [this.allAssetProfile, ...data];
        }
        return data;
      })
    );
  }

  clear() {
    this.ignoreClosedPanel = true;
    this.selectAssetProfileFormGroup.get('assetProfile').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.assetProfileInput.nativeElement.blur();
      this.assetProfileInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  assetProfileEnter($event: KeyboardEvent) {
    if (this.editProfileEnabled && $event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createAssetProfile($event, this.searchText);
      }
    }
  }

  createAssetProfile($event: Event, profileName: string) {
    $event.stopPropagation();
    const assetProfile: AssetProfile = {
      name: profileName
    } as AssetProfile;
    if (this.addNewProfile) {
      this.openAssetProfileDialog(assetProfile, true);
    }
  }

  editAssetProfile($event: Event) {
    $event.stopPropagation();
    this.assetProfileService.getAssetProfile(this.modelValue.id).subscribe(
      (assetProfile) => {
        this.openAssetProfileDialog(assetProfile, false);
      }
    );
  }

  openAssetProfileDialog(assetProfile: AssetProfile, isAdd: boolean) {
    this.dialog.open<AssetProfileDialogComponent, AssetProfileDialogData,
      AssetProfile>(AssetProfileDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        assetProfile
      }
    }).afterClosed().subscribe(
      (savedAssetProfile) => {
        if (!savedAssetProfile) {
          setTimeout(() => {
            this.assetProfileInput.nativeElement.blur();
            this.assetProfileInput.nativeElement.focus();
          }, 0);
        } else {
          this.assetProfileService.getAssetProfileInfo(savedAssetProfile.id.id).subscribe(
            (profile) => {
              this.modelValue = new AssetProfileId(profile.id.id);
              this.selectAssetProfileFormGroup.get('assetProfile').patchValue(profile, {emitEvent: true});
              if (isAdd) {
                this.propagateChange(this.modelValue);
              } else {
                this.assetProfileUpdated.next(savedAssetProfile.id);
              }
              this.assetProfileChanged.emit(profile);
            }
          );
        }
      }
    );
  }
}

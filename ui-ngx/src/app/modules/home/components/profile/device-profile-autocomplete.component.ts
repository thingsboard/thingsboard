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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TruncatePipe } from '@shared//pipe/truncate.pipe';
import { ENTER } from '@angular/cdk/keycodes';
import { MatDialog } from '@angular/material/dialog';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  DeviceProfile,
  DeviceProfileInfo,
  DeviceProfileType,
  DeviceTransportType
} from '@shared/models/device.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DeviceProfileDialogComponent, DeviceProfileDialogData } from './device-profile-dialog.component';

@Component({
  selector: 'tb-device-profile-autocomplete',
  templateUrl: './device-profile-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DeviceProfileAutocompleteComponent),
    multi: true
  }]
})
export class DeviceProfileAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectDeviceProfileFormGroup: FormGroup;

  modelValue: DeviceProfileId | null;

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

  @Output()
  deviceProfileUpdated = new EventEmitter<DeviceProfileId>();

  @Output()
  deviceProfileChanged = new EventEmitter<DeviceProfileInfo>();

  @ViewChild('deviceProfileInput', {static: true}) deviceProfileInput: ElementRef;

  filteredDeviceProfiles: Observable<Array<DeviceProfileInfo>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder,
              private dialog: MatDialog) {
    this.selectDeviceProfileFormGroup = this.fb.group({
      deviceProfile: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredDeviceProfiles = this.selectDeviceProfileFormGroup.get('deviceProfile').valueChanges
      .pipe(
        tap((value: DeviceProfileInfo | string) => {
          let modelValue: DeviceProfileInfo | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        startWith<string | DeviceProfileInfo>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchDeviceProfiles(name) )
      );
  }

  selectDefaultDeviceProfileIfNeeded(): void {
    if (this.selectDefaultProfile && !this.modelValue) {
      this.deviceProfileService.getDefaultDeviceProfileInfo().subscribe(
        (profile) => {
          if (profile) {
            this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(profile, {emitEvent: false});
            this.updateView(profile);
          }
        }
      );
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: DeviceProfileId | null): void {
    this.searchText = '';
    if (value != null) {
      this.deviceProfileService.getDeviceProfileInfo(value.id).subscribe(
        (profile) => {
          this.modelValue = new DeviceProfileId(profile.id.id);
          this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(profile, {emitEvent: true});
        }
      );
    } else {
      this.modelValue = null;
      this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(null, {emitEvent: true});
      this.selectDefaultDeviceProfileIfNeeded();
    }
  }

  updateView(deviceProfile: DeviceProfileInfo | null) {
    const idValue = deviceProfile ? new DeviceProfileId(deviceProfile.id.id) : null;
    if (!entityIdEquals(this.modelValue, idValue)) {
      this.modelValue = idValue;
      this.propagateChange(this.modelValue);
      this.deviceProfileChanged.emit(deviceProfile);
    }
  }

  displayDeviceProfileFn(profile?: DeviceProfileInfo): string | undefined {
    return profile ? profile.name : undefined;
  }

  fetchDeviceProfiles(searchText?: string): Observable<Array<DeviceProfileInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.deviceProfileService.getDeviceProfileInfos(pageLink, {ignoreLoading: true}).pipe(
      map(pageData => {
        return pageData.data;
      })
    );
  }

  clear() {
    this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.deviceProfileInput.nativeElement.blur();
      this.deviceProfileInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  deviceProfileEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createDeviceProfile($event, this.searchText);
      }
    }
  }

  createDeviceProfile($event: Event, profileName: string) {
    $event.preventDefault();
    const deviceProfile: DeviceProfile = {
      id: null,
      name: profileName,
      type: DeviceProfileType.DEFAULT,
      transportType: DeviceTransportType.DEFAULT,
      profileData: {
        configuration: createDeviceProfileConfiguration(DeviceProfileType.DEFAULT),
        transportConfiguration: createDeviceProfileTransportConfiguration(DeviceTransportType.DEFAULT)
      }
    };
    this.openDeviceProfileDialog(deviceProfile, true);
  }

  editDeviceProfile($event: Event) {
    $event.preventDefault();
    this.deviceProfileService.getDeviceProfile(this.modelValue.id).subscribe(
      (deviceProfile) => {
        this.openDeviceProfileDialog(deviceProfile, false);
      }
    );
  }

  openDeviceProfileDialog(deviceProfile: DeviceProfile, isAdd: boolean) {
    this.dialog.open<DeviceProfileDialogComponent, DeviceProfileDialogData,
      DeviceProfile>(DeviceProfileDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        deviceProfile
      }
    }).afterClosed().subscribe(
      (savedDeviceProfile) => {
        if (!savedDeviceProfile) {
          setTimeout(() => {
            this.deviceProfileInput.nativeElement.blur();
            this.deviceProfileInput.nativeElement.focus();
          }, 0);
        } else {
          this.deviceProfileService.getDeviceProfileInfo(savedDeviceProfile.id.id).subscribe(
            (profile) => {
              this.modelValue = new DeviceProfileId(profile.id.id);
              this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(profile, {emitEvent: true});
              if (isAdd) {
                this.propagateChange(this.modelValue);
              } else {
                this.deviceProfileUpdated.next(savedDeviceProfile.id);
              }
              this.deviceProfileChanged.emit(profile);
            }
          );
        }
      }
    );
  }
}

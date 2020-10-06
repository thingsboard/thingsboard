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

import {
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input, NgZone,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { map, mergeMap, share, tap } from 'rxjs/operators';
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
import { MatAutocomplete } from '@angular/material/autocomplete';
import { AddDeviceProfileDialogComponent, AddDeviceProfileDialogData } from './add-device-profile-dialog.component';

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

  @Input()
  displayAllOnEmpty = false;

  @Input()
  editProfileEnabled = true;

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

  @ViewChild('deviceProfileAutocomplete', {static: true}) deviceProfileAutocomplete: MatAutocomplete;

  filteredDeviceProfiles: Observable<Array<DeviceProfileInfo>>;

  searchText = '';

  private dirty = false;

  private ignoreClosedPanel = false;

  private allDeviceProfile: DeviceProfileInfo = {
    name: this.translate.instant('device-profile.all-device-profiles'),
    type: DeviceProfileType.DEFAULT,
    transportType: DeviceTransportType.DEFAULT,
    id: null
  };

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder,
              private zone: NgZone,
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
          if (!this.displayAllOnEmpty || modelValue) {
            this.updateView(modelValue);
          }
        }),
        map(value => {
          if (value) {
            if (typeof value === 'string') {
              return value;
            } else {
              if (this.displayAllOnEmpty && value === this.allDeviceProfile) {
                return '';
              } else {
                return value.name;
              }
            }
          } else {
            return '';
          }
        }),
        mergeMap(name => this.fetchDeviceProfiles(name) ),
        share()
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
          this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(profile, {emitEvent: false});
          this.deviceProfileChanged.emit(profile);
        }
      );
    } else if (this.displayAllOnEmpty) {
      this.modelValue = null;
      this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(this.allDeviceProfile, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(null, {emitEvent: false});
      this.selectDefaultDeviceProfileIfNeeded();
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectDeviceProfileFormGroup.get('deviceProfile').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  onPanelClosed() {
    if (this.ignoreClosedPanel) {
      this.ignoreClosedPanel = false;
    } else {
      if (this.displayAllOnEmpty && !this.selectDeviceProfileFormGroup.get('deviceProfile').value) {
        this.zone.run(() => {
          this.selectDeviceProfileFormGroup.get('deviceProfile').patchValue(this.allDeviceProfile, {emitEvent: true});
        }, 0);
      }
    }
  }

  updateView(deviceProfile: DeviceProfileInfo | null) {
    const idValue = deviceProfile && deviceProfile.id ? new DeviceProfileId(deviceProfile.id.id) : null;
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
        let data = pageData.data;
        if (this.displayAllOnEmpty) {
          data = [this.allDeviceProfile, ...data];
        }
        return data;
      })
    );
  }

  clear() {
    this.ignoreClosedPanel = true;
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
    if (this.editProfileEnabled && $event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createDeviceProfile($event, this.searchText);
      }
    }
  }

  createDeviceProfile($event: Event, profileName: string) {
    $event.preventDefault();
    const deviceProfile: DeviceProfile = {
      name: profileName
    } as DeviceProfile;
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
    let deviceProfileObservable: Observable<DeviceProfile>;
    if (!isAdd) {
      deviceProfileObservable = this.dialog.open<DeviceProfileDialogComponent, DeviceProfileDialogData,
        DeviceProfile>(DeviceProfileDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          isAdd: false,
          deviceProfile
        }
      }).afterClosed();
    } else {
      deviceProfileObservable = this.dialog.open<AddDeviceProfileDialogComponent, AddDeviceProfileDialogData,
        DeviceProfile>(AddDeviceProfileDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          deviceProfileName: deviceProfile.name
        }
      }).afterClosed();
    }
    deviceProfileObservable.subscribe(
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

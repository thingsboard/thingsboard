///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {Component, EventEmitter, forwardRef, Inject, Input, Output} from "@angular/core";
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from "@angular/forms";
import {coerceBooleanProperty} from "@angular/cdk/coercion";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {DeviceProfileService} from "@core/http/device-profile.service";
import {WINDOW} from "@core/services/window.service";
import {deepClone, isDefinedAndNotNull, isEmpty} from "@core/utils";
import {
  Lwm2mAttributesDialogComponent,
  Lwm2mAttributesDialogData
} from "@home/components/profile/device/lwm2m/lwm2m-attributes-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";


@Component({
  selector: 'tb-profile-lwm2m-attributes',
  templateUrl: './lwm2m-attributes.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mAttributesComponent),
    multi: true
  }]
})
export class Lwm2mAttributesComponent implements ControlValueAccessor {
  attributeLwm2mFormGroup: FormGroup;
  disabled = false;

  private requiredValue: boolean;
  private dirty = false;

  @Input()
  attributeLwm2m: {};

  @Input()
  destName: string;

  @Output()
  updateAttributeLwm2m = new EventEmitter<any>();

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = (v: any) => {
  }

  constructor(private store: Store<AppState>,
              private dialog: MatDialog,
              private fb: FormBuilder,
              private deviceProfileService: DeviceProfileService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.attributeLwm2mFormGroup.disable({emitEvent: false});
    } else {
      this.attributeLwm2mFormGroup.enable({emitEvent: false});
    }
  }

  ngOnInit() {
    this.attributeLwm2mFormGroup = this.fb.group({
      attributeLwm2m: [this.attributeLwm2m]
    });
  }

  writeValue(value: {} | null): void {}


  attributeLwm2mToString = (attributeLwm2m: {}): string => {
    return this.isIconEditAdd (attributeLwm2m) ? JSON.stringify(attributeLwm2m) :this.translate.instant('device-profile.lwm2m.no-data');
  }

  isDisableBtn (attributeLwm2m: {}): boolean {
    return this.disabled ? !(isDefinedAndNotNull(attributeLwm2m) && !isEmpty(attributeLwm2m) && this.disabled) :  this.disabled;
  }

  isIconEditAdd (attributeLwm2m: {}): boolean {
    return isDefinedAndNotNull(attributeLwm2m) && !isEmpty(attributeLwm2m);
  }

  public editAttributesLwm2m = ($event: Event): void => {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<Lwm2mAttributesDialogComponent, Lwm2mAttributesDialogData, Object>(Lwm2mAttributesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        attributeLwm2m: this.disabled ? this.attributeLwm2m : deepClone(this.attributeLwm2m),
        destName: this.destName
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.attributeLwm2m = result;
        this.attributeLwm2mFormGroup.patchValue({attributeLwm2m: this.attributeLwm2m});
        this.updateAttributeLwm2m.next(this.attributeLwm2m);
      }
    });
  }
}

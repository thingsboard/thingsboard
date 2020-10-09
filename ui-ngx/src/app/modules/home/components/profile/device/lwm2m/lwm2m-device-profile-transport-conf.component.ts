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

import {Component, forwardRef, Input, OnInit} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType, Lwm2mDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import {MatTabChangeEvent} from "@angular/material/tabs";

@Component({
  selector: 'tb-lwm2m-device-profile-transport-conf',
  templateUrl: './lwm2m-device-profile-transport-conf.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfComponent),
    multi: true
  }]
})
export class Lwm2mDeviceProfileTransportConfComponent implements ControlValueAccessor, OnInit {

  lwm2mDeviceProfileTransportConfigurationFormGroup: FormGroup;
  private configurationValue: {};
  private requiredValue: boolean;
  private tabIndexPrevious = 0 as number;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => {
  };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.lwm2mDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      shortId: [null, Validators.required],
      lifetime: [null, Validators.required],
      defaultMinPeriod: [null, Validators.required],
      notifIfDisabled: [true, []],
      binding: ["U", Validators.required],
      configurationJson: [null, Validators.required]
    });
    this.lwm2mDeviceProfileTransportConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.lwm2mDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Lwm2mDeviceProfileTransportConfiguration | null): void {
    this.lwm2mDeviceProfileTransportConfigurationFormGroup.patchValue({
        configurationJson: value
      },
      {emitEvent: false});
    this.configurationValue = this.lwm2mDeviceProfileTransportConfigurationFormGroup.getRawValue().configurationJson;
    this.lwm2mDeviceProfileTransportConfigurationFormGroup.patchValue({
        shortId: this.configurationValue['bootstrap'].servers.shortId,
        lifetime: this.configurationValue['bootstrap'].servers.lifetime,
        defaultMinPeriod: this.configurationValue['bootstrap'].servers.defaultMinPeriod,
        notifIfDisabled: this.configurationValue['bootstrap'].servers.notifIfDisabled,
        binding: this.configurationValue['bootstrap'].servers.binding
      },
      {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    this.upDateValueToJson();
    if (this.lwm2mDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.lwm2mDeviceProfileTransportConfigurationFormGroup.getRawValue().configurationJson;
      configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }

  tabChanged = (tabChangeEvent: MatTabChangeEvent): void => {
    if (this.tabIndexPrevious !== tabChangeEvent.index) this.upDateValueToJson();
    this.tabIndexPrevious = tabChangeEvent.index;
  }

  upDateValueToJson(): void {
    switch (this.tabIndexPrevious) {
      case 0:
        this.upDateValueToJsonTab_0();
        break;
      case 1:
        // this.upDateValueToJsonTab_1();
        break;
      case 2:
        // this.upDateValueToJsonTab_2();
        break;
    }
  }

  upDateValueToJsonTab_0(): void {
    if (this.lwm2mDeviceProfileTransportConfigurationFormGroup !== null) {
      if (!this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('shortId').pristine && this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('shortId').valid) {
        this.configurationValue['bootstrap'].servers.shortId = this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('shortId').value;
        this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('shortId').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('lifetime').pristine && this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('lifetime').valid) {
        this.configurationValue['bootstrap'].servers.lifetime = this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('lifetime').value;
        this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('lifetime').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('defaultMinPeriod').pristine && this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('defaultMinPeriod').valid) {
        this.configurationValue['bootstrap'].servers.defaultMinPeriod = this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('defaultMinPeriod').value;
        this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('defaultMinPeriod').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('notifIfDisabled').pristine && this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('notifIfDisabled').valid) {
        this.configurationValue['bootstrap'].servers.notifIfDisabled = this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('notifIfDisabled').value;
        this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('notifIfDisabled').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('binding').pristine && this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('binding').valid) {
        this.configurationValue['bootstrap'].servers.binding = this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('binding').value;
        this.lwm2mDeviceProfileTransportConfigurationFormGroup.get('binding').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      // if (this.bootstrapFormGroup !== null && !this.bootstrapFormGroup.pristine && this.bootstrapFormGroup.valid) {
      //   this.configurationValue['bootstrap'].bootstrapServer = this.bootstrapFormGroup.value;
      //   this.bootstrapFormGroup.markAsPristine({
      //     onlySelf: true
      //   });
      //   this.upDateJsonAllConfig();
      // }
      //
      // if (this.lwm2mServerFormGroup !== null && !this.lwm2mServerFormGroup.pristine && this.lwm2mServerFormGroup.valid) {
      //   this.configurationValue['bootstrap'].lwm2mServer = this.lwm2mServerFormGroup.value;
      //   // this.configurationValue['bootstrap'].lwm2mServer.bootstrapServerIs = false;
      //   this.lwm2mServerFormGroup.markAsPristine({
      //     onlySelf: true
      //   });
      //   this.upDateJsonAllConfig();
      // }
    }
  }

  upDateJsonAllConfig(): void {
    this.lwm2mDeviceProfileTransportConfigurationFormGroup.patchValue({
      configurationJson: this.configurationValue
    }, {emitEvent: false});
    this.lwm2mDeviceProfileTransportConfigurationFormGroup.markAsDirty();
  }


}

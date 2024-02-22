///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  AdminSettings,
  DeviceConnectivityProtocol,
  DeviceConnectivitySettings,
  GatewaySettings,
  GeneralSettings
} from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { property } from 'lodash';

@Component({
  selector: 'tb-general-settings',
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss', './settings-card.scss']
})
export class GeneralSettingsComponent extends PageComponent implements HasConfirmForm, OnDestroy {

  generalSettings: FormGroup;
  deviceConnectivitySettingsForm: FormGroup;
  gatewaySettingsArray: FormArray = this.fb.array([]);
  isHasAvailableVersions = false;

  protocol: DeviceConnectivityProtocol = 'http';

  private adminSettings: AdminSettings<GeneralSettings>;
  private deviceConnectivitySettings: AdminSettings<DeviceConnectivitySettings>;
  private gatewaySettings: AdminSettings<GatewaySettings>;
  defaultVersion: string;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              public fb: FormBuilder) {
    super(store);
    this.buildGeneralServerSettingsForm();
    this.adminService.getAdminSettings<GeneralSettings>('general')
      .subscribe(adminSettings => this.processGeneralSettings(adminSettings));
    this.buildDeviceConnectivitySettingsForm();
    this.adminService.getAdminSettings<DeviceConnectivitySettings>('connectivity')
      .subscribe(deviceConnectivitySettings => this.processDeviceConnectivitySettings(deviceConnectivitySettings));
    this.adminService.getAdminSettings<GatewaySettings>('gateway')
      .subscribe(gatewaySettings => {
        this.isHasAvailableVersions = gatewaySettings.jsonValue.availableVersions
                                      && !!gatewaySettings.jsonValue.availableVersions.length;
        // gatewaySettings.jsonValue = {"availableVersions":["latest","3.4.4","3.4.3.1","3.4.3","3.4.2","3.4"],"version":"latest", "key": "test"};
        this.processGatewaySettings(gatewaySettings)
      });
    // this.adminService('/api/notification')
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildGeneralServerSettingsForm() {
    this.generalSettings = this.fb.group({
      baseUrl: ['', [Validators.required]],
      prohibitDifferentUrl: ['',[]]
    });
  }

  private buildGatewayServerSettingsForm(settingsPairs) {
    this.gatewaySettingsArray.controls = [];
    settingsPairs.forEach(item => {
      this.gatewaySettingsArray.push(this.fb.group({
        property: [{ value: item.property, disabled: true}],
        value: [item.value]
      }));
    });
  }

  private buildDeviceConnectivitySettingsForm() {
    this.deviceConnectivitySettingsForm = this.fb.group({
      http: this.buildDeviceConnectivityInfoForm(),
      https: this.buildDeviceConnectivityInfoForm(),
      mqtt: this.buildDeviceConnectivityInfoForm(),
      mqtts: this.buildDeviceConnectivityInfoForm(),
      coap: this.buildDeviceConnectivityInfoForm(),
      coaps: this.buildDeviceConnectivityInfoForm()
    });
  }

  private buildDeviceConnectivityInfoForm(): FormGroup {
    const formGroup = this.fb.group({
      enabled: [false, []],
      host: [{value: '', disabled: true}],
      port: [{value: null, disabled: true}, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
    });
    formGroup.get('enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        formGroup.get('host').enable({emitEvent: false});
        formGroup.get('port').enable({emitEvent: false});
      } else {
        formGroup.get('host').disable({emitEvent: false});
        formGroup.get('port').disable({emitEvent: false});
      }
    });
    return formGroup;
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.generalSettings.value};
    this.adminService.saveAdminSettings(this.adminSettings)
      .subscribe(adminSettings => this.processGeneralSettings(adminSettings));
  }

  saveDeviceConnectivitySettings(): void {
    this.deviceConnectivitySettings.jsonValue = {
      ...this.deviceConnectivitySettings.jsonValue,
      ...this.deviceConnectivitySettingsForm.getRawValue()
    };
    this.adminService.saveAdminSettings<DeviceConnectivitySettings>(this.deviceConnectivitySettings)
      .subscribe(deviceConnectivitySettings => this.processDeviceConnectivitySettings(deviceConnectivitySettings));
  }

  saveGatewaySettings(): void {
    this.gatewaySettingsArray.getRawValue().forEach(item => {
      this.gatewaySettings.jsonValue[item.property] = item.value;
    })
    if (!this.gatewaySettings) {this.gatewaySettings = {key: 'gatewaySettings', jsonValue: {"version": "latest"}}};
    if (!this.gatewaySettings.jsonValue) {this.gatewaySettings.jsonValue = {"version": "latest"}};
    if (!this.gatewaySettings.jsonValue.version) {this.gatewaySettings.jsonValue["version"] = "latest"};

    this.adminService.saveAdminSettings<GatewaySettings>(this.gatewaySettings)
      .subscribe(gatewaySettings => this.processGatewaySettings(gatewaySettings));
  }

  discardGeneralSettings(): void {
    this.generalSettings.reset(this.adminSettings.jsonValue);
  }

  discardGatewaySettings(): void {
    this.gatewaySettingsArray.controls = [];
    this.processGatewaySettings(this.gatewaySettings);
  }

  discardDeviceConnectivitySettings(): void {
    this.deviceConnectivitySettingsForm.reset(this.deviceConnectivitySettings.jsonValue);
  }

  private processGeneralSettings(generalSettings: AdminSettings<GeneralSettings>): void {
    this.adminSettings = generalSettings;
    this.generalSettings.reset(this.adminSettings.jsonValue);
  }

  private processDeviceConnectivitySettings(deviceConnectivitySettings: AdminSettings<DeviceConnectivitySettings>): void {
    this.deviceConnectivitySettings = deviceConnectivitySettings;
    this.deviceConnectivitySettingsForm.reset(this.deviceConnectivitySettings.jsonValue);
  }

  private processGatewaySettings(gatewaySettings: AdminSettings<GatewaySettings>): void {
    this.gatewaySettings = gatewaySettings;
    this.defaultVersion = gatewaySettings.jsonValue.version;
    const settingsPairs = [];
    
    let availableVersions = this.gatewaySettings.jsonValue && this.gatewaySettings.jsonValue.availableVersions
                              ? this.gatewaySettings.jsonValue.availableVersions
                              : [];
    console.log('availableVersions', availableVersions)
    if (availableVersions.length === 0) {
        const version = this.gatewaySettings.jsonValue && this.gatewaySettings.jsonValue.version
                              ? this.gatewaySettings.jsonValue.version
                              : 'latest';
        availableVersions.push(version);
    }

    Object.entries(this.gatewaySettings.jsonValue).forEach(([property, value]) => {
      if (property === 'version') {
        settingsPairs.push({
          property,
          value: availableVersions
        });
      } else if (property !== 'availableVersions') {
        settingsPairs.push({
          property,
          value
        });
      }
    })
    this.buildGatewayServerSettingsForm(settingsPairs)
  }

  confirmForm(): FormGroup {
    return this.generalSettings.dirty ? this.generalSettings : this.deviceConnectivitySettingsForm;
  }
}

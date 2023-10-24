///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { AdminSettings, DeviceConnectivitySettings, GeneralSettings } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-general-settings',
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss', './settings-card.scss']
})
export class GeneralSettingsComponent extends PageComponent implements HasConfirmForm {

  generalSettings: UntypedFormGroup;
  private adminSettings: AdminSettings<GeneralSettings>;

  deviceConnectivitySettingsForm: UntypedFormGroup;
  private deviceConnectivitySettings: AdminSettings<DeviceConnectivitySettings>;

  protocol = 'http';

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              public fb: UntypedFormBuilder) {
    super(store);
    this.buildGeneralServerSettingsForm();
    this.adminService.getAdminSettings<GeneralSettings>('general')
      .subscribe(adminSettings => this.processGeneralSettings(adminSettings));
    this.buildDeviceConnectivitySettingsForm();
    this.adminService.getAdminSettings<DeviceConnectivitySettings>('connectivity')
      .subscribe(deviceConnectivitySettings => this.processDeviceConnectivitySettings(deviceConnectivitySettings));
  }

  buildGeneralServerSettingsForm() {
    this.generalSettings = this.fb.group({
      baseUrl: ['', [Validators.required]],
      prohibitDifferentUrl: ['',[]]
    });
  }

  buildDeviceConnectivitySettingsForm() {
    this.deviceConnectivitySettingsForm = this.fb.group({
      http: this.fb.group({
        enabled: [false, []],
        host: ['', []],
        port: [null, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
      }),
      https: this.fb.group({
        enabled: [false, []],
        host: ['', []],
        port: [null, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
      }),
      mqtt: this.fb.group({
        enabled: [false, []],
        host: ['', []],
        port: [null, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
      }),
      mqtts: this.fb.group({
        enabled: [false, []],
        host: ['', []],
        port: [null, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
      }),
      coap: this.fb.group({
        enabled: [false, []],
        host: ['', []],
        port: [null, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
      }),
      coaps: this.fb.group({
        enabled: [false, []],
        host: ['', []],
        port: [null, [Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]]
      }),
    });
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.generalSettings.value};
    this.adminService.saveAdminSettings(this.adminSettings)
      .subscribe(adminSettings => this.processGeneralSettings(adminSettings));
  }

  saveDeviceConnectivitySettings(): void {
    this.deviceConnectivitySettings.jsonValue = {...this.deviceConnectivitySettings.jsonValue, ...this.deviceConnectivitySettingsForm.value};
    this.adminService.saveAdminSettings<DeviceConnectivitySettings>(this.deviceConnectivitySettings)
      .subscribe(deviceConnectivitySettings => this.processDeviceConnectivitySettings(deviceConnectivitySettings));
  }

  discardGeneralSettings(): void {
    this.generalSettings.reset(this.adminSettings.jsonValue);
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

  confirmForm(): UntypedFormGroup {
    return this.generalSettings.dirty ? this.generalSettings : this.deviceConnectivitySettingsForm;
  }

}

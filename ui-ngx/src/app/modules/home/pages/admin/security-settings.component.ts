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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SecuritySettings } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-security-settings',
  templateUrl: './security-settings.component.html',
  styleUrls: ['./security-settings.component.scss', './settings-card.scss']
})
export class SecuritySettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  securitySettingsFormGroup: FormGroup;
  securitySettings: SecuritySettings;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildSecuritySettingsForm();
    this.adminService.getSecuritySettings().subscribe(
      (securitySettings) => {
        this.securitySettings = securitySettings;
        this.securitySettingsFormGroup.reset(this.securitySettings);
      }
    );
  }

  buildSecuritySettingsForm() {
    this.securitySettingsFormGroup = this.fb.group({
      maxFailedLoginAttempts: [null, [Validators.min(0)]],
      userLockoutNotificationEmail: ['', []],
      passwordPolicy: this.fb.group(
        {
          minimumLength: [null, [Validators.required, Validators.min(5), Validators.max(50)]],
          minimumUppercaseLetters: [null, Validators.min(0)],
          minimumLowercaseLetters: [null, Validators.min(0)],
          minimumDigits: [null, Validators.min(0)],
          minimumSpecialCharacters: [null, Validators.min(0)],
          passwordExpirationPeriodDays: [null, Validators.min(0)],
          passwordReuseFrequencyDays: [null, Validators.min(0)]
        }
      )
    });
  }

  save(): void {
    this.securitySettings = {...this.securitySettings, ...this.securitySettingsFormGroup.value};
    this.adminService.saveSecuritySettings(this.securitySettings).subscribe(
      (securitySettings) => {
        this.securitySettings = securitySettings;
        this.securitySettingsFormGroup.reset(this.securitySettings);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.securitySettingsFormGroup;
  }

}

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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminSettings, MailServerSettings, portPattern } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-mail-server',
  templateUrl: './mail-server.component.html',
  styleUrls: ['./mail-server.component.scss', './settings-card.scss']
})
export class MailServerComponent extends PageComponent implements OnInit, HasConfirmForm {

  mailSettings: FormGroup;
  adminSettings: AdminSettings<MailServerSettings>;
  smtpProtocols = ['smtp', 'smtps'];

  tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildMailServerSettingsForm();
    this.adminService.getAdminSettings<MailServerSettings>('mail').subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.mailSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  buildMailServerSettingsForm() {
    this.mailSettings = this.fb.group({
      mailFrom: ['', [Validators.required]],
      smtpProtocol: ['smtp'],
      smtpHost: ['localhost', [Validators.required]],
      smtpPort: ['25', [Validators.required,
        Validators.pattern(portPattern),
        Validators.maxLength(5)]],
      timeout: ['10000', [Validators.required,
        Validators.pattern(/^[0-9]{1,6}$/),
        Validators.maxLength(6)]],
      enableTls: ['false'],
      tlsVersion: [],
      enableProxy: ['false', []],
      proxyHost: ['', [Validators.required]],
      proxyPort: ['', [Validators.required, Validators.maxLength(5), Validators.pattern(portPattern)]],
      proxyUser: [''],
      proxyPassword: [''],
      username: [''],
      password: ['']
    });
    this.registerDisableOnLoadFormControl(this.mailSettings.get('smtpProtocol'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('enableTls'));
  }

  enableProxy(): boolean {
    let enableProxy: boolean = this.mailSettings.get('enableProxy').value;
    if (enableProxy) {
      this.mailSettings.get('proxyHost').enable();
      this.mailSettings.get('proxyPort').enable();
    } else {
      this.mailSettings.get('proxyHost').disable();
      this.mailSettings.get('proxyPort').disable();
    }
    return enableProxy;
  }

  sendTestMail(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettings.value};
    this.adminService.sendTestMail(this.adminSettings).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.test-mail-sent'),
          type: 'success' }));
      }
    );
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettings.value};
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.mailSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.mailSettings;
  }

}

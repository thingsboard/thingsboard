///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { FormBuilder, FormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import {
  EntitiesVersionControlSettings,
  VersionControlAuthMethod,
  versionControlAuthMethodTranslationMap
} from '@shared/models/settings.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-version-control-settings',
  templateUrl: './version-control-settings.component.html',
  styleUrls: ['./version-control-settings.component.scss', './settings-card.scss']
})
export class VersionControlSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  versionControlSettingsForm: FormGroup;
  settings: EntitiesVersionControlSettings = null;

  versionControlAuthMethod = VersionControlAuthMethod;
  versionControlAuthMethods = Object.values(VersionControlAuthMethod);
  versionControlAuthMethodTranslations = versionControlAuthMethodTranslationMap;

  showChangePassword = false;
  changePassword = false;

  showChangePrivateKeyPassword = false;
  changePrivateKeyPassword = false;

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private dialogService: DialogService,
              private translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.versionControlSettingsForm = this.fb.group({
      repositoryUri: [null, [Validators.required]],
      defaultBranch: [null, []],
      authMethod: [VersionControlAuthMethod.USERNAME_PASSWORD, [Validators.required]],
      username: [null, []],
      password: [null, []],
      privateKeyFileName: [null, [Validators.required]],
      privateKey: [null, []],
      privateKeyPassword: [null, []]
    });
    this.updateValidators(false);
    this.versionControlSettingsForm.get('authMethod').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.versionControlSettingsForm.get('privateKeyFileName').valueChanges.subscribe(() => {
      this.updateValidators(false);
    });
    this.adminService.getEntitiesVersionControlSettings({ignoreErrors: true}).subscribe(
      (settings) => {
        this.settings = settings;
        if (this.settings.authMethod === VersionControlAuthMethod.USERNAME_PASSWORD) {
          this.showChangePassword = true;
        } else {
          this.showChangePrivateKeyPassword = true;
        }
        this.versionControlSettingsForm.reset(this.settings);
        this.updateValidators(false);
    });
  }

  checkAccess(): void {
    const settings: EntitiesVersionControlSettings = this.versionControlSettingsForm.value;
    this.adminService.checkVersionControlAccess(settings).subscribe(() => {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.check-vc-access-success'),
        type: 'success' }));
    });
  }

  save(): void {
    const settings: EntitiesVersionControlSettings = this.versionControlSettingsForm.value;
    this.adminService.saveEntitiesVersionControlSettings(settings).subscribe(
      (savedSettings) => {
        this.settings = savedSettings;
        if (this.settings.authMethod === VersionControlAuthMethod.USERNAME_PASSWORD) {
          this.showChangePassword = true;
          this.changePassword = false;
        } else {
          this.showChangePrivateKeyPassword = true;
          this.changePrivateKeyPassword = false;
        }
        this.versionControlSettingsForm.reset(this.settings);
        this.updateValidators(false);
      }
    );
  }

  delete(formDirective: FormGroupDirective): void {
    this.dialogService.confirm(
      this.translate.instant('admin.delete-git-settings-title', ),
      this.translate.instant('admin.delete-git-settings-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.adminService.deleteEntitiesVersionControlSettings().subscribe(
          () => {
            this.settings = null;
            this.showChangePassword = false;
            this.changePassword = false;
            this.showChangePrivateKeyPassword = false;
            this.changePrivateKeyPassword = false;
            formDirective.resetForm();
            this.versionControlSettingsForm.reset({ authMethod: VersionControlAuthMethod.USERNAME_PASSWORD });
            this.updateValidators(false);
          }
        );
      }
    });
  }

  confirmForm(): FormGroup {
    return this.versionControlSettingsForm;
  }

  changePasswordChanged() {
    if (this.changePassword) {
      this.versionControlSettingsForm.get('password').patchValue('');
      this.versionControlSettingsForm.get('password').markAsDirty();
    }
    this.updateValidators(false);
  }

  changePrivateKeyPasswordChanged() {
    if (this.changePrivateKeyPassword) {
      this.versionControlSettingsForm.get('privateKeyPassword').patchValue('');
      this.versionControlSettingsForm.get('privateKeyPassword').markAsDirty();
    }
    this.updateValidators(false);
  }

  updateValidators(emitEvent?: boolean): void {
    const authMethod: VersionControlAuthMethod = this.versionControlSettingsForm.get('authMethod').value;
    const privateKeyFileName: string = this.versionControlSettingsForm.get('privateKeyFileName').value;
    if (authMethod === VersionControlAuthMethod.USERNAME_PASSWORD) {
      this.versionControlSettingsForm.get('username').enable({emitEvent});
      if (this.changePassword || !this.showChangePassword) {
        this.versionControlSettingsForm.get('password').enable({emitEvent});
      } else {
        this.versionControlSettingsForm.get('password').disable({emitEvent});
      }
      this.versionControlSettingsForm.get('privateKeyFileName').disable({emitEvent});
      this.versionControlSettingsForm.get('privateKey').disable({emitEvent});
      this.versionControlSettingsForm.get('privateKeyPassword').disable({emitEvent});
    } else {
      this.versionControlSettingsForm.get('username').disable({emitEvent});
      this.versionControlSettingsForm.get('password').disable({emitEvent});
      this.versionControlSettingsForm.get('privateKeyFileName').enable({emitEvent});
      this.versionControlSettingsForm.get('privateKey').enable({emitEvent});
      if (this.changePrivateKeyPassword || !this.showChangePrivateKeyPassword) {
        this.versionControlSettingsForm.get('privateKeyPassword').enable({emitEvent});
      } else {
        this.versionControlSettingsForm.get('privateKeyPassword').disable({emitEvent});
      }
      if (isNotEmptyStr(privateKeyFileName)) {
        this.versionControlSettingsForm.get('privateKey').clearValidators();
      } else {
        this.versionControlSettingsForm.get('privateKey').setValidators([Validators.required]);
      }
    }
    this.versionControlSettingsForm.get('username').updateValueAndValidity({emitEvent: false});
    this.versionControlSettingsForm.get('password').updateValueAndValidity({emitEvent: false});
    this.versionControlSettingsForm.get('privateKeyFileName').updateValueAndValidity({emitEvent: false});
    this.versionControlSettingsForm.get('privateKey').updateValueAndValidity({emitEvent: false});
    this.versionControlSettingsForm.get('privateKeyPassword').updateValueAndValidity({emitEvent: false});
  }

}

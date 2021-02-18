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
import { UserService } from '@core/http/user.service';
import { User } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { ActionAuthUpdateUserDetails } from '@core/auth/auth.actions';
import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { ChangePasswordDialogComponent } from '@modules/home/pages/profile/change-password-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { AuthService } from '@core/auth/auth.service';
import { ActivatedRoute } from '@angular/router';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent extends PageComponent implements OnInit, HasConfirmForm {

  authorities = Authority;
  profile: FormGroup;
  user: User;
  languageList = env.supportedLangs;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private userService: UserService,
              private authService: AuthService,
              private translate: TranslateService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildProfileForm();
    this.userLoaded(this.route.snapshot.data.user);
  }

  buildProfileForm() {
    this.profile = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: [''],
      lastName: [''],
      language: [''],
      homeDashboardId: [null],
      homeDashboardHideToolbar: [true]
    });
  }

  save(): void {
    this.user = {...this.user, ...this.profile.value};
    if (!this.user.additionalInfo) {
      this.user.additionalInfo = {};
    }
    this.user.additionalInfo.lang = this.profile.get('language').value;
    this.user.additionalInfo.homeDashboardId = this.profile.get('homeDashboardId').value;
    this.user.additionalInfo.homeDashboardHideToolbar = this.profile.get('homeDashboardHideToolbar').value;
    this.userService.saveUser(this.user).subscribe(
      (user) => {
        this.userLoaded(user);
        this.store.dispatch(new ActionAuthUpdateUserDetails({ userDetails: {
            additionalInfo: {...user.additionalInfo},
            authority: user.authority,
            createdTime: user.createdTime,
            tenantId: user.tenantId,
            customerId: user.customerId,
            email: user.email,
            firstName: user.firstName,
            id: user.id,
            lastName: user.lastName,
          } }));
        this.store.dispatch(new ActionSettingsChangeLanguage({ userLang: user.additionalInfo.lang }));
      }
    );
  }

  changePassword(): void {
    this.dialog.open(ChangePasswordDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    });
  }

  userLoaded(user: User) {
    this.user = user;
    this.profile.reset(user);
    let lang;
    let homeDashboardId;
    let homeDashboardHideToolbar = true;
    if (user.additionalInfo) {
      if (user.additionalInfo.lang) {
        lang = user.additionalInfo.lang;
      }
      homeDashboardId = user.additionalInfo.homeDashboardId;
      if (isDefinedAndNotNull(user.additionalInfo.homeDashboardHideToolbar)) {
        homeDashboardHideToolbar = user.additionalInfo.homeDashboardHideToolbar;
      }
    }
    if (!lang) {
      lang = this.translate.currentLang;
    }
    this.profile.get('language').setValue(lang);
    this.profile.get('homeDashboardId').setValue(homeDashboardId);
    this.profile.get('homeDashboardHideToolbar').setValue(homeDashboardHideToolbar);
  }

  confirmForm(): FormGroup {
    return this.profile;
  }

}

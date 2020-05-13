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
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

@Component({
  selector: 'tb-change-password-dialog',
  templateUrl: './change-password-dialog.component.html',
  styleUrls: ['./change-password-dialog.component.scss']
})
export class ChangePasswordDialogComponent extends DialogComponent<ChangePasswordDialogComponent> implements OnInit {

  changePassword: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              private authService: AuthService,
              public dialogRef: MatDialogRef<ChangePasswordDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.buildChangePasswordForm();
  }

  buildChangePasswordForm() {
    this.changePassword = this.fb.group({
      currentPassword: [''],
      newPassword: [''],
      newPassword2: ['']
    });
  }

  onChangePassword(): void {
    if (this.changePassword.get('newPassword').value !== this.changePassword.get('newPassword2').value) {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('login.passwords-mismatch-error'),
        type: 'error' }));
    } else {
      this.authService.changePassword(
        this.changePassword.get('currentPassword').value,
        this.changePassword.get('newPassword').value).subscribe(() => {
          this.dialogRef.close(true);
      });
    }
  }
}

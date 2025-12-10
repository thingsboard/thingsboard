///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { AuthService } from '@core/auth/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { passwordsMatchValidator, passwordStrengthValidator } from '@shared/models/password.models';

@Component({
  selector: 'tb-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent {

  isExpiredPassword: boolean;
  isLoading = false;

  resetPassword: FormGroup;
  passwordPolicy: UserPasswordPolicy;

  private resetToken: string;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private authService: AuthService,
              private fb: FormBuilder) {

    this.resetToken = this.route.snapshot.queryParams['resetToken'] || '';
    this.passwordPolicy = this.route.snapshot.data['passwordPolicy'];
    this.isExpiredPassword = this.route.snapshot.data['expiredPassword'] ?? false;

    this.buildResetPasswordForm();
  }

  private buildResetPasswordForm() {
    this.resetPassword = this.fb.group({
      newPassword: ['', [Validators.required, passwordStrengthValidator(this.passwordPolicy)]],
      newPassword2: ['']
    }, {
      validators: [
        passwordsMatchValidator('newPassword', 'newPassword2'),
      ]
    });
  }

  onResetPassword() {
    if (this.resetPassword.invalid) {
      this.resetPassword.markAllAsTouched();
    } else {
      this.isLoading = true;
      this.authService.resetPassword(this.resetToken, this.resetPassword.get('newPassword').value).subscribe({
        next: () => this.router.navigateByUrl('login'),
        error: () => {this.isLoading = false;}
      });
    }
  }
}

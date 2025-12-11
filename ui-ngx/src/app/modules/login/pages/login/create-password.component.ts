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
import { ActivatedRoute } from '@angular/router';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { passwordsMatchValidator, passwordStrengthValidator } from '@shared/models/password.models';

@Component({
  selector: 'tb-create-password',
  templateUrl: './create-password.component.html',
  styleUrls: ['./password.component.scss']
})
export class CreatePasswordComponent {

  passwordPolicy: UserPasswordPolicy;
  createPassword: FormGroup;

  isLoading = false;

  private activateToken: string;

  constructor(private route: ActivatedRoute,
              private authService: AuthService,
              private fb: FormBuilder) {

    this.activateToken = this.route.snapshot.queryParams['activateToken'] || '';
    this.passwordPolicy = this.route.snapshot.data['passwordPolicy'];

    this.buildCreatePasswordForm();
  }

  private buildCreatePasswordForm() {
    this.createPassword = this.fb.group({
      newPassword: ['', [Validators.required, passwordStrengthValidator(this.passwordPolicy)]],
      newPassword2: ['']
    }, {
      validators: [
        passwordsMatchValidator('newPassword', 'newPassword2'),
      ]
    });
  }

  onCreatePassword() {
    if (this.createPassword.invalid) {
      this.createPassword.markAllAsTouched();
    } else {
      this.isLoading = true
      this.authService.activate(this.activateToken, this.createPassword.get('newPassword').value, true)
        .subscribe({
          error: () => {this.isLoading = false;}
        });
    }
  }
}

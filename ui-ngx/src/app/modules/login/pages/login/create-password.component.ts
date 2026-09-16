// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { passwordsMatchValidator, passwordStrengthValidator } from '@shared/models/password.models';
import { finalize } from 'rxjs/operators';
import { PageComponent } from '@shared/components/page.component';

@Component({
    selector: 'tb-create-password',
    templateUrl: './create-password.component.html',
    styleUrls: ['./password.component.scss'],
    standalone: false
})
export class CreatePasswordComponent extends PageComponent {

  passwordPolicy: UserPasswordPolicy;
  createPassword: FormGroup;

  isLoading = false;

  private activateToken: string;

  constructor(private route: ActivatedRoute,
              private authService: AuthService,
              private fb: FormBuilder) {
    super();
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
      this.isLoading = true;
      this.authService.activate(this.activateToken, this.createPassword.get('newPassword').value, true).pipe(
        finalize(() => {this.isLoading = false;})
      ).subscribe(() => {});
    }
  }
}

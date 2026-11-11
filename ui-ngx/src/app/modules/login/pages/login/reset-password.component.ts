// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, HostBinding } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { passwordsMatchValidator, passwordStrengthValidator } from '@shared/models/password.models';
import { finalize } from 'rxjs/operators';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
    selector: 'tb-reset-password',
    templateUrl: './reset-password.component.html',
    styleUrls: ['./password.component.scss'],
    standalone: false
})
export class ResetPasswordComponent {

  isExpiredPassword: boolean;
  isLoading = false;

  resetPassword: FormGroup;
  passwordPolicy: UserPasswordPolicy;

  private resetToken: string;

  @HostBinding('class') class = 'tb-custom-css';

  constructor(private route: ActivatedRoute,
              private router: Router,
              private authService: AuthService,
              private fb: FormBuilder,
              public wl: WhiteLabelingService) {

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
      this.authService.resetPassword(this.resetToken, this.resetPassword.get('newPassword').value).pipe(
        finalize(() => {this.isLoading = false})
      ).subscribe(() => this.router.navigateByUrl('login'));
    }
  }
}

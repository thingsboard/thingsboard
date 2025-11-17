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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { combineLatest } from 'rxjs';
import {
  passwordsMatchValidator,
  passwordStrengthValidator
} from '@shared/models/password.models';

@Component({
  selector: 'tb-create-password',
  templateUrl: './create-password.component.html',
  styleUrls: ['./create-password.component.scss']
})
export class CreatePasswordComponent extends PageComponent {

  activateToken = '';
  createPassword: UntypedFormGroup;
  passwordPolicy: UserPasswordPolicy;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private authService: AuthService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);

    combineLatest([
      this.route.queryParams,
      this.route.data
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(([params, data]) => {
        this.activateToken = params['activateToken'] || '';
        this.passwordPolicy = data['passwordPolicy'];
      });

    this.buildCreatePasswordForm();
  }

  private buildCreatePasswordForm() {
    this.createPassword = this.fb.group({
      newPassword: ['', [Validators.required, passwordStrengthValidator(this.passwordPolicy)]],
      newPassword2:['']
    }, {
      validators: [
        passwordsMatchValidator('newPassword', 'newPassword2'),
      ]
    });
  }

  get passwordErrorsLength(): number {
    return Object.keys(this.createPassword.get('newPassword')?.errors ?? {}).length;
  }

  onCreatePassword() {
    if (this.createPassword.invalid) {
      this.createPassword.markAllAsTouched();
    } else {
      this.authService.activate(
        this.activateToken,
        this.createPassword.get('password').value, true).subscribe();
    }
  }
}

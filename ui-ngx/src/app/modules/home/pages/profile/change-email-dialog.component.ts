///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { validateEmail } from '@core/utils';
import { UserService } from '@core/http/user.service';
import { EmailChangeStatus } from '@shared/models/user.model';
import { HttpErrorResponse } from '@angular/common/http';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-change-email-dialog',
    templateUrl: './change-email-dialog.component.html',
    styleUrls: ['../security/authentication-dialog/authentication-dialog.component.scss'],
    standalone: false
})
export class ChangeEmailDialogComponent extends DialogComponent<ChangeEmailDialogComponent, string> {

  emailForm: UntypedFormGroup;
  verificationForm: UntypedFormGroup;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private userService: UserService,
              private translate: TranslateService,
              public dialogRef: MatDialogRef<ChangeEmailDialogComponent, string>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.emailForm = this.fb.group({
      email: ['', [Validators.required, validateEmail]]
    });

    this.verificationForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });
  }

  nextStep() {
    switch (this.stepper.selectedIndex) {
      case 0:
        if (this.emailForm.valid) {
          const email = this.emailForm.get('email').value as string;
          this.userService.changeEmail(email).subscribe({
            next: (result) => {
              if (result.status === EmailChangeStatus.SUCCESS) {
                this.dialogRef.close(email);
              } else {
                this.stepper.next();
              }
            },
            error: (error: HttpErrorResponse) => this.showRateLimitError(error)
          });
        } else {
          this.showFormErrors(this.emailForm);
        }
        break;
      case 1:
        if (this.verificationForm.valid) {
          this.userService.verifyEmailChange(this.verificationForm.get('verificationCode').value).subscribe({
            next: () => this.dialogRef.close(this.emailForm.get('email').value),
            error: (error: HttpErrorResponse) => this.showRateLimitError(error)
          });
        } else {
          this.showFormErrors(this.verificationForm);
        }
        break;
    }
  }

  closeDialog() {
    return this.dialogRef.close(null);
  }

  // The global HTTP interceptor only surfaces a 429 when the request config asks it to auto-retry,
  // which this dialog does not want; show the backend's own message (throttle or failure-cap) instead.
  private showRateLimitError(error: HttpErrorResponse) {
    if (error.status === 429) {
      // A proxy or load balancer in front of the platform can synthesize its own 429 with a plain-text
      // or empty body, leaving no error.message to show; fall back to a generic translated message.
      const message = error.error?.message || this.translate.instant('server-error.too-many-requests');
      this.store.dispatch(new ActionNotificationShow({message, type: 'error'}));
    }
  }

  private showFormErrors(form: UntypedFormGroup) {
    Object.keys(form.controls).forEach(field => {
      form.get(field).markAsTouched({onlySelf: true});
    });
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding, ViewChild } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder } from '@angular/forms';
import { SignupRequest, SignupRequestValues, SignUpResult } from '@shared/models/signup.models';
import { ActivatedRoute, Router } from '@angular/router';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { SignupService } from '@core/http/signup.service';
import { DialogService } from '@core/services/dialog.service';
import { ReCaptcha2Component, ReCaptchaV3Service } from 'ngx-captcha';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { MatDialog } from '@angular/material/dialog';
import { SignupDialogComponent, SignupDialogData } from '@modules/signup/pages/signup/signup-dialog.component';
import { from } from 'rxjs';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { passwordStrengthValidator } from '@shared/models/password.models';

@Component({
    selector: 'tb-signup',
    templateUrl: './signup.component.html',
    styleUrls: ['./signup.component.scss'],
    standalone: false
})
export class SignupComponent {

  @ViewChild('recaptcha') recaptchaComponent: ReCaptcha2Component;

  private signupRequest = SignupRequest.create();

  signup = this.fb.group({
    fields: this.signupRequest.fields,
    recaptchaResponse: [this.signupRequest.recaptchaResponse]
  })
  acceptPrivacyPolicy: boolean;
  acceptTermsOfUse: boolean;
  acceptAll: boolean;
  signupParams = this.selfRegistrationService.signUpParams;
  passwordPolicy: UserPasswordPolicy;

  isLoading = false;

  @HostBinding('class') class = 'tb-custom-css';

  constructor(private store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private authService: AuthService,
              private signupService: SignupService,
              public wl: WhiteLabelingService,
              private selfRegistrationService: SelfRegistrationService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private reCaptchaV3Service: ReCaptchaV3Service,
              private dialog: MatDialog,
              private fb: FormBuilder) {
    this.passwordPolicy = this.route.snapshot.data['passwordPolicy'];
    this.signup.get('fields.PASSWORD').setValidators(passwordStrengthValidator(this.passwordPolicy));
  }

  signUp(): void {
    if (this.signup.valid) {
      if (this.validateSignUpRequest()) {
        const signupRequestValue = this.signup.value;
        delete signupRequestValue.fields.CHECK_PASSWORD;
        if (this.signupParams?.captcha?.version === 'v2') {
          this.executeSignup(signupRequestValue as SignupRequestValues);
        } else {
          from(this.reCaptchaV3Service.executeAsPromise(this.signupParams?.captcha?.siteKey,
            this.signupParams?.captcha?.logActionName, {useGlobalDomain: true})).subscribe(
            {
              next: (token) => {
                signupRequestValue.recaptchaResponse = token;
                this.executeSignup(signupRequestValue as SignupRequestValues);
              },
              error: err => {
                this.store.dispatch(new ActionNotificationShow({ message: 'ReCaptcha error: ' + err,
                  type: 'error' }));
              }
            }
          );
        }
      }
    } else {
      this.signup.markAllAsTouched();
    }
  }

  private executeSignup(signupRequest: SignupRequestValues): void {
    this.isLoading = true;
    this.signupService.signup(signupRequest).subscribe({
      next: (signupResult) => {
        if (signupResult === SignUpResult.INACTIVE_USER_EXISTS) {
          this.isLoading = false;
          this.promptToResendEmailVerification();
          if (this.recaptchaComponent) {
            this.recaptchaComponent.resetCaptcha();
          }
        } else {
          this.router.navigateByUrl(`/signup/emailVerification?email=${this.emailToURLParam}`).then(() => {});
        }
      },
      error: () => {
        this.isLoading = false;
        if (this.recaptchaComponent) {
          this.recaptchaComponent.resetCaptcha();
        }
      }
    });
  }

  private promptToResendEmailVerification() {
    this.dialogService.confirm(
      this.translate.instant('signup.inactive-user-exists-title'),
      this.translate.instant('signup.inactive-user-exists-text'),
      this.translate.instant('action.cancel'),
      this.translate.instant('signup.resend')
    ).subscribe((result) => {
      if (result) {
        this.authService.resendEmailActivation(this.signup.get('fields.EMAIL').value).subscribe(
          () => {
            this.router.navigateByUrl(`/signup/emailVerification?email=${this.emailToURLParam}`).then(() => {});
          }
        );
      }
    });
  }

  private validateSignUpRequest(): boolean {
    if (this.signupParams?.captcha?.version === 'v2' &&
      (!this.signup.get('recaptchaResponse').value || this.signup.get('recaptchaResponse').value.length < 1)) {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('signup.no-captcha-message'),
        type: 'error' }));
      return false;
    }
    if (this.signupParams.showPrivacyPolicy && this.signupParams.showTermsOfUse) {
      if (!this.acceptAll) {
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('signup.accept-privacy-policy-and-terms-of-use-message'),
          type: 'error'
        }));
        return false;
      }
    } else if (this.signupParams.showPrivacyPolicy) {
      if (!this.acceptPrivacyPolicy) {
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('signup.accept-privacy-policy-message'),
          type: 'error'
        }));
        return false;
      }
    } else if (this.signupParams.showTermsOfUse) {
      if (!this.acceptTermsOfUse) {
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('signup.accept-terms-of-use-message'),
          type: 'error'
        }));
        return false;
      }
    }
    return true;
  }

  openPrivacyPolicy($event: Event) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<SignupDialogComponent, SignupDialogData, boolean>
    (SignupDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: 'signup.privacy-policy',
        content$: this.selfRegistrationService.loadPrivacyPolicy()
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.acceptPrivacyPolicy = true;
          if (this.signupParams.showTermsOfUse && this.acceptTermsOfUse) {
            this.acceptAll = true;
          }
        }
      });
  }

  openTermsOfUse($event: Event) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<SignupDialogComponent, SignupDialogData, boolean>
    (SignupDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: 'signup.terms-of-use',
        content$: this.selfRegistrationService.loadTermsOfUse()
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.acceptTermsOfUse = true;
          if (this.signupParams.showPrivacyPolicy && this.acceptPrivacyPolicy) {
            this.acceptAll = true;
          }
        }
      });
  }

  private get emailToURLParam(): string {
    return encodeURIComponent(this.signup.get('fields.EMAIL').value);
  }
}

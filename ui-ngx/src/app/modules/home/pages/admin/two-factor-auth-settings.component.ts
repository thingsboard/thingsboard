import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute } from '@angular/router';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { WINDOW } from '@core/services/window.service';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import {
  TwoFactorAuthProviderType,
  TwoFactorAuthSettings,
  TwoFactorAuthSettingsForm
} from '@shared/models/two-factor-auth.models';

@Component({
  selector: 'tb-2fa-settings',
  templateUrl: './two-factor-auth-settings.component.html',
  styleUrls: ['./two-factor-auth-settings.component.scss', './settings-card.scss']
})
export class TwoFactorAuthSettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser = this.authState.authUser;

  twoFaFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private twoFaService: TwoFactorAuthenticationService,
              private fb: FormBuilder,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit() {
    this.build2faSettingsForm();
    this.twoFaService.getTwoFaSettings().subscribe((setting) => {
      console.log(this.formDataPreprocessing(setting));
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  confirmForm(): FormGroup {
    return this.twoFaFormGroup;
  }

  isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  save() {

  }

  private build2faSettingsForm(): void {
    this.twoFaFormGroup = this.fb.group({
      useSystemTwoFactorAuthSettings: [false],
      maxVerificationFailuresBeforeUserLockout: [30, [
        Validators.required,
        Validators.pattern(/^\d*$/),
        Validators.min(0),
        Validators.max(65535)
      ]],
      totalAllowedTimeForVerification: [3600, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(/^\d*$/)
      ]],
      verificationCodeCheckRateLimit: ['', Validators.pattern(/^[1-9]\d*:[1-9]\d*$/)],
      verificationCodeSendRateLimit: ['', Validators.pattern(/^[1-9]\d*:[1-9]\d*$/)],
      providers: this.fb.array([])
    });
  }

  addProviders() {
    const newProviders = this.fb.group({
      providerType: [TwoFactorAuthProviderType.TOTP],
      issuerName: ['', Validators.required],
      smsVerificationMessageTemplate: [{
        value: 'Verification code: ${verificationCode}',
        disabled: true
      }, [
        Validators.required,
        Validators.pattern(/\${verificationCode}/)
      ]],
      verificationCodeLifetime: [{
        value: 120,
        disabled: true
      }, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(/^\d*$/)
      ]]
    });
    newProviders.get('providerType').valueChanges.subscribe(type => {
      switch (type) {
        case TwoFactorAuthProviderType.SMS:
          newProviders.get('issuerName').disable({emitEvent: false});
          newProviders.get('smsVerificationMessageTemplate').enable({emitEvent: false});
          newProviders.get('verificationCodeLifetime').enable({emitEvent: false});
          break;
        case TwoFactorAuthProviderType.TOTP:
          newProviders.get('issuerName').enable({emitEvent: false});
          newProviders.get('smsVerificationMessageTemplate').disable({emitEvent: false});
          newProviders.get('verificationCodeLifetime').disable({emitEvent: false});
          break;
      }
    });
    if (this.providersForm.length) {
      const selectProvidersType = this.providersForm.value[0].providerType;
      if (selectProvidersType !== TwoFactorAuthProviderType.TOTP) {
        newProviders.get('providerType').patchValue(TwoFactorAuthProviderType.SMS, {emitEvents: true})
      }
    }
    this.providersForm.push(newProviders);
  }

  removeProviders($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.providersForm.removeAt(index);
    this.providersForm.markAsTouched();
    this.providersForm.markAsDirty();
  }

  get providersForm(): FormArray {
    return this.twoFaFormGroup.get('providers') as FormArray;
  }

  private formDataPreprocessing(data: TwoFactorAuthSettings): TwoFactorAuthSettingsForm {
    return data;
  }

  private formDataPostprocessing(data: TwoFactorAuthSettingsForm): TwoFactorAuthSettings{
    return data;
  }

  trackByParams(index: number): number {
    return index;
  }

}

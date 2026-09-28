// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { ReCaptcha2Component, ReCaptchaV3Service } from 'ngx-captcha';
import { MobileService } from '@core/services/mobile.service';
import { SelfRegistrationService } from '@app/core/http/self-register.service';
import { from } from 'rxjs';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ActivatedRoute } from '@angular/router';
import { CaptchaParams, CaptchaVersion } from '@shared/models/self-register.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
    selector: 'tb-recaptcha',
    templateUrl: './tb-recaptcha.component.html',
    styleUrls: ['./tb-recaptcha.component.scss'],
    standalone: false
})
export class TbRecaptchaComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('recaptcha') recaptchaComponent: ReCaptcha2Component;

  captcha: CaptchaParams;

  recaptchaResponse: string;

  activeCaptcha = false;

  private isMobileApp = this.mobileService.isMobileApp();

  constructor(private selfRegistrationService: SelfRegistrationService,
              private reCaptchaV3Service: ReCaptchaV3Service,
              private mobileService: MobileService,
              private route: ActivatedRoute) {
    super();

    if (this.route.snapshot.queryParamMap.has('version') && this.route.snapshot.queryParamMap.get('siteKey')) {
      let queryVersion = this.route.snapshot.queryParamMap.get('version');
      if (queryVersion !== 'v2' && queryVersion !== 'v3') {
        queryVersion = 'v3';
      }

      this.captcha = {
        version: queryVersion as CaptchaVersion,
        siteKey: this.route.snapshot.queryParamMap.get('siteKey'),
        logActionName: this.route.snapshot.queryParamMap.get('logActionName') ?? '',
      }

      this.activeCaptcha = isDefinedAndNotNull(this.captcha?.siteKey)
    } else {
      this.captcha = this.selfRegistrationService.signUpParams.captcha;
      this.activeCaptcha = this.selfRegistrationService.signUpParams.activate;
    }
  }

  ngOnInit() {
    if (this.isMobileApp) {
      this.mobileService.registerResetRecaptchaFunction(() => {
        setTimeout(() => {
          if (this.recaptchaComponent) {
            this.recaptchaComponent.resetCaptcha();
          }
        });
      });
      setTimeout(() => {
        this.mobileService.onRecaptchaLoaded();
      });
    }
    if (this.activeCaptcha && this.captcha.version === 'v3') {
      from(this.reCaptchaV3Service.executeAsPromise(this.captcha.siteKey,
        this.captcha.logActionName, {useGlobalDomain: true})).subscribe(
        {
          next: (token) => {
            this.mobileService.handleReCaptchaResponse(token);
          },
          error: err => {
            this.store.dispatch(new ActionNotificationShow({ message: 'ReCaptcha error: ' + err,
              type: 'error' }));
          }
        }
      );
    }
  }

  ngOnDestroy() {
    if (this.isMobileApp) {
      this.mobileService.unregisterResetRecaptchaFunction();
    }
    super.ngOnDestroy();
  }

  onRecaptchaResponse() {
    this.mobileService.handleReCaptchaResponse(this.recaptchaResponse);
  }

}

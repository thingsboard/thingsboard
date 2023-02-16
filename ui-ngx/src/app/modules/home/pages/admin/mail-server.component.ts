///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminSettings, MailServerSettings, smtpPortPattern } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { isDefined, isDefinedAndNotNull, isString } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  DomainSchema,
  domainSchemaTranslations,
  MailDomainSchema,
  MailServerOauth2Provider,
  mailServerOauth2ProvidersTranslations,
  OAuth2DomainInfo
} from '@shared/models/oauth2.models';
import { WINDOW } from '@core/services/window.service';

@Component({
  selector: 'tb-mail-server',
  templateUrl: './mail-server.component.html',
  styleUrls: ['./mail-server.component.scss', './settings-card.scss']
})
export class MailServerComponent extends PageComponent implements OnInit, OnDestroy, HasConfirmForm {

  mailSettings: FormGroup;
  domainForm: FormGroup;
  adminSettings: AdminSettings<MailServerSettings>;
  smtpProtocols = ['smtp', 'smtps'];
  showChangePassword = false;

  protocols = Object.values(MailDomainSchema);
  domainSchemaTranslations = domainSchemaTranslations;

  mailServerOauth2Provider = MailServerOauth2Provider;
  mailServerOauth2Providers = Object.values(MailServerOauth2Provider);
  mailServerOauth2ProvidersTranslations = mailServerOauth2ProvidersTranslations;

  tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

  private destroy$ = new Subject<void>();
  private DOMAIN_AND_PORT_REGEXP = /^(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?$/;
  private loginProcessingUrl: string = this.route.snapshot.data.loginProcessingUrl;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private adminService: AdminService,
              private translate: TranslateService,
              public fb: FormBuilder,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit() {
    this.loginProcessingUrl = this.route.snapshot.data.loginProcessingUrl;
    this.buildMailServerSettingsForm();
    this.buildDomainInfoForm();
    this.adminService.getAdminSettings<MailServerSettings>('mail').subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        if (this.adminSettings.jsonValue && isString(this.adminSettings.jsonValue.enableTls)) {
          this.adminSettings.jsonValue.enableTls = (this.adminSettings.jsonValue.enableTls as any) === 'true';
        }
        this.showChangePassword =
          isDefinedAndNotNull(this.adminSettings.jsonValue.showChangePassword) ? this.adminSettings.jsonValue.showChangePassword : true ;
        delete this.adminSettings.jsonValue.showChangePassword;
        this.mailSettings.patchValue(this.adminSettings.jsonValue, {emitEvent: false});
        this.enableMailPassword(!this.showChangePassword);
        this.enableProxyChanged();
        if (this.adminSettings.jsonValue.enableOauth2) {
          this.enableOauth2(!!this.adminSettings.jsonValue.enableOauth2);
          this.parseUrl(this.adminSettings.jsonValue.redirectUri);
        }
      }
    );
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  buildMailServerSettingsForm() {
    this.mailSettings = this.fb.group({
      mailFrom: ['', [Validators.required]],
      smtpProtocol: ['smtp'],
      smtpHost: ['localhost', [Validators.required]],
      smtpPort: ['25', [Validators.required,
        Validators.pattern(smtpPortPattern),
        Validators.maxLength(5)]],
      timeout: ['10000', [Validators.required,
        Validators.pattern(/^[0-9]{1,6}$/),
        Validators.maxLength(6)]],
      enableTls: [false],
      tlsVersion: [{ value: null, disabled: true }],
      enableProxy: [false],
      proxyHost: [{ value: '', disabled: true }, [Validators.required]],
      proxyPort: [{ value: '', disabled: true }, [Validators.required, Validators.min(1), Validators.max(65535)]],
      proxyUser: [{ value: '', disabled: true }],
      proxyPassword: [{ value: '', disabled: true }],
      username: [''],
      changePassword: [false],
      password: [''],
      enableOauth2: [false],
      providerId: [{ value:MailServerOauth2Provider.GOOGLE, disabled: true }],
      clientId: [{ value:'', disabled: true }, [Validators.required, Validators.maxLength(255)]],
      clientSecret: [{ value:'', disabled: true }, [Validators.required, Validators.maxLength(2048)]],
      providerTenantId: [{value: '', disabled: true}, [Validators.required]],
      authUri: [{value: '', disabled: true}, [Validators.required]],
      tokenUri: [{value: '', disabled: true}, [Validators.required]],
      scope: [{value: '', disabled: true}, [Validators.required]],
      redirectUri: [{ value:'', disabled: true}]
    });

    this.registerDisableOnLoadFormControl(this.mailSettings.get('smtpProtocol'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('enableTls'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('enableProxy'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('changePassword'));

    this.mailSettings.get('enableProxy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.enableProxyChanged();
    });

    this.mailSettings.get('changePassword').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.enableMailPassword(value);
    });

    this.mailSettings.get('enableOauth2').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe( value => this.enableOauth2(value));

    this.mailSettings.get('providerId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe( value => this.enableProviderIdChanged(value));
  }

  private buildDomainInfoForm(domainInfo?: OAuth2DomainInfo) {
    this.domainForm = this.fb.group({
      name: [domainInfo ? domainInfo.name : this.window.location.hostname, [
        Validators.required, Validators.maxLength(255),
        Validators.pattern(this.DOMAIN_AND_PORT_REGEXP)]],
      scheme: [domainInfo?.scheme ? domainInfo.scheme : MailDomainSchema.HTTPS, Validators.required]
    }, {validators: this.uniqueDomainValidator});
  }

  private uniqueDomainValidator(control: FormGroup): { [key: string]: boolean } | null {
    if (control.parent?.value) {
      const domain = control.value.name;
      const listProtocols = control.parent.getRawValue()
        .filter((domainInfo) => domainInfo.name === domain)
        .map((domainInfo) => domainInfo.scheme);
      if (listProtocols.length > 1 || new Set(listProtocols).size !== listProtocols.length) {
        return {unique: true};
      }
    }
    return null;
  }

  enableOauth2(value: boolean): void {
    if (value) {
      this.mailSettings.get('providerId').enable();
      this.mailSettings.get('clientId').enable();
      this.mailSettings.get('clientSecret').enable();
      this.mailSettings.get('redirectUri').enable();
    } else {
      this.mailSettings.get('providerId').disable();
      this.mailSettings.get('clientId').disable();
      this.mailSettings.get('clientSecret').disable();
      this.mailSettings.get('redirectUri').disable();
    }
  }

  enableProviderIdChanged(value: MailServerOauth2Provider): void {
    if (value === this.mailServerOauth2Provider.MICROSOFT) {
      this.mailSettings.get('providerTenantId').enable();
    } else if (value === this.mailServerOauth2Provider.CUSTOM) {
      this.mailSettings.get('authUri').enable();
      this.mailSettings.get('tokenUri').enable();
      this.mailSettings.get('scope').enable();
    } else {
      this.mailSettings.get('providerTenantId').disable();
      this.mailSettings.get('authUri').disable();
      this.mailSettings.get('tokenUri').disable();
      this.mailSettings.get('scope').disable();
    }
  }

  enableProxyChanged(): void {
    const enableProxy: boolean = this.mailSettings.get('enableProxy').value;
    if (enableProxy) {
      this.mailSettings.get('proxyHost').enable();
      this.mailSettings.get('proxyPort').enable();
    } else {
      this.mailSettings.get('proxyHost').disable();
      this.mailSettings.get('proxyPort').disable();
    }
  }

  enableMailPassword(enable: boolean) {
    if (enable) {
      this.mailSettings.get('password').enable({emitEvent: false});
    } else {
      this.mailSettings.get('password').disable({emitEvent: false});
    }
  }

  sendTestMail(): void {
    this.getAdminSettings();
    this.adminService.sendTestMail(this.adminSettings).subscribe();
  }

  save(): void {
    this.getAdminSettings();
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.showChangePassword = true;
        this.mailSettings.reset(this.adminSettings.jsonValue);
        this.domainForm.reset();
        this.parseUrl(this.adminSettings.jsonValue.redirectUri);
      }
    );
  }

  getAdminSettings(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettingsFormValue};
    if (this.adminSettings.jsonValue.enableOauth2) {
      this.adminSettings.jsonValue.redirectUri = this.redirectURI();
    }
  }

  generateAccessToken() {
    this.adminService.generateAccessToken().subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('admin.oauth2.generate-access-token-success'),
          type: 'success'
        }));
      }
    );
  }

  redirectURI(schema?: DomainSchema): string {
    const domainInfo = this.domainForm.getRawValue();
    if (domainInfo.name !== '') {
      let protocol;
      if (isDefined(schema)) {
        protocol = schema.toLowerCase();
      } else {
        protocol = domainInfo.scheme.toLowerCase();
      }
      return `${protocol}://${domainInfo.name}${this.loginProcessingUrl}`;
    }
    return '';
  }

  parseUrl(value: string) {
    const url = new URL(value);
    this.domainForm.get('scheme').patchValue(
      url.protocol.includes('https:') ? MailDomainSchema.HTTPS : MailDomainSchema.HTTP, {emitEvent: false}
    );
    this.domainForm.get('name').patchValue(url.host, {emitEvent: false});
  }

  confirmForm(): FormGroup {
    return this.mailSettings;
  }

  private get mailSettingsFormValue(): MailServerSettings {
    const formValue = this.mailSettings.value;
    delete formValue.changePassword;
    return formValue;
  }
}

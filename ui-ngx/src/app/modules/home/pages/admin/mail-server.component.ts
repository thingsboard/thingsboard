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
import { FormBuilder, FormGroup, UntypedFormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AdminSettings,
  MailConfigTemplate,
  MailServerOauth2Provider,
  mailServerOauth2ProvidersTranslations,
  MailServerSettings,
  smtpPortPattern,
  SmtpProtocol
} from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { isDefined, isDefinedAndNotNull, isString } from '@core/utils';
import { forkJoin, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DomainSchema, domainSchemaTranslations, } from '@shared/models/oauth2.models';
import { WINDOW } from '@core/services/window.service';
import { AuthService } from '@core/auth/auth.service';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';

@Component({
  selector: 'tb-mail-server',
  templateUrl: './mail-server.component.html',
  styleUrls: ['./mail-server.component.scss', './settings-card.scss']
})
export class MailServerComponent extends PageComponent implements OnInit, OnDestroy, HasConfirmForm {
  adminSettings: AdminSettings<MailServerSettings>;
  smtpProtocols = Object.values(SmtpProtocol);
  showChangePassword = false;

  protocols = Object.values(DomainSchema).filter(value => value !== DomainSchema.MIXED);
  domainSchemaTranslations = domainSchemaTranslations;

  mailServerOauth2Provider = MailServerOauth2Provider;
  mailServerOauth2ProvidersTranslations = mailServerOauth2ProvidersTranslations;

  tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

  helpLink: string;

  templates = new Map<string, MailConfigTemplate>();

  templateProvider = ['CUSTOM'];

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  private destroy$ = new Subject<void>();
  private DOMAIN_AND_PORT_REGEXP = /^(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?$/;
  private URL_REGEXP = /^[A-Za-z][A-Za-z\d.+-]*:\/*(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?(?:\/[\w#!:.,?+=&%@\-/]*)?$/;
  private loginProcessingUrl: string;

  mailSettings = this.fb.group({
    mailFrom: ['', [Validators.required]],
    smtpProtocol: [SmtpProtocol.SMTP],
    smtpHost: ['localhost', [Validators.required]],
    smtpPort: [25, [Validators.required,
      Validators.pattern(smtpPortPattern),
      Validators.maxLength(5)]],
    timeout: [10000, [Validators.required,
      Validators.pattern(/^[0-9]{1,6}$/),
      Validators.maxLength(6)]],
    enableTls: [false],
    tlsVersion: [{ value: null, disabled: true }],
    enableProxy: [false],
    proxyHost: [{ value: '', disabled: true }, [Validators.required]],
    proxyPort: [{ value: null, disabled: true }, [Validators.required, Validators.min(1), Validators.max(65535)]],
    proxyUser: [{ value: '', disabled: true }],
    proxyPassword: [{ value: '', disabled: true }],
    username: [''],
    changePassword: [false],
    password: [''],
    enableOauth2: [false],
    providerId: ['', [Validators.required]],
    clientId: [{ value:'', disabled: true }, [Validators.required, Validators.maxLength(255)]],
    clientSecret: [{ value:'', disabled: true }, [Validators.required, Validators.maxLength(2048)]],
    providerTenantId: [{value: '', disabled: true}, [Validators.required]],
    authUri: [{value: '', disabled: true}, [Validators.required, Validators.pattern(this.URL_REGEXP)]],
    tokenUri: [{value: '', disabled: true}, [Validators.required, Validators.pattern(this.URL_REGEXP)]],
    scope: [],
    redirectUri: [{ value:'', disabled: true}]
  });

  private defaultConfiguration = {
    providerId: MailServerOauth2Provider.CUSTOM,
    smtpProtocol: SmtpProtocol.SMTP,
    smtpHost: '',
    smtpPort: null,
    timeout: null,
    enableTls: false,
    tlsVersion: null,
    enableProxy: false,
    proxyHost: '',
    proxyPort: null,
    proxyUser: '',
    proxyPassword: '',
    enableOauth2: false,
    clientId: '',
    clientSecret: '',
    providerTenantId: '',
    authUri: '',
    tokenUri: '',
    scope: [],
    redirectUri: ''
  };

  domainForm = this.fb.group({
    name: [this.window.location.hostname, [
      Validators.required, Validators.maxLength(255),
      Validators.pattern(this.DOMAIN_AND_PORT_REGEXP)]
    ],
    scheme: [DomainSchema.HTTPS, Validators.required]
  }, {validators: this.uniqueDomainValidator});

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private adminService: AdminService,
              private authService: AuthService,
              private translate: TranslateService,
              public fb: FormBuilder,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit() {
    this.mailServerSettingsForm();
    this.domainFormConfiguration();

    forkJoin([
      this.adminService.getLoginProcessingUrl(),
      this.adminService.getMailConfigTemplate(),
      this.adminService.getAdminSettings<MailServerSettings>('mail')
    ]).subscribe(([loginProcessingUrl, mailConfigTemplate, adminSettings]) => {
      this.loginProcessingUrl = loginProcessingUrl;
      this.initTemplates(mailConfigTemplate);
      this.adminSettings = adminSettings;
      if (this.adminSettings.jsonValue && isString(this.adminSettings.jsonValue.enableTls)) {
        this.adminSettings.jsonValue.enableTls = (this.adminSettings.jsonValue.enableTls as any) === 'true';
      }
      this.showChangePassword = isDefinedAndNotNull(this.adminSettings.jsonValue.showChangePassword)
        ? this.adminSettings.jsonValue.showChangePassword : true;
      delete this.adminSettings.jsonValue.showChangePassword;
      this.mailSettings.reset(this.adminSettings.jsonValue, {emitEvent: false});
      this.enableMailPassword(!this.showChangePassword);
      this.enableProxyChanged();
      this.enableTls(this.adminSettings.jsonValue.enableTls);
      this.helpLink = this.templates.get(this.adminSettings.jsonValue.providerId)?.helpLink || null;
      if (this.adminSettings.jsonValue.enableOauth2) {
        this.enableOauth2(!!this.adminSettings.jsonValue.enableOauth2);
        this.enableProviderTenantIdChanged(this.adminSettings.jsonValue.providerId);
        this.parseUrl(this.adminSettings.jsonValue.redirectUri);
        this.mailSettings.get('redirectUri').patchValue(this.adminSettings.jsonValue.redirectUri, {emitEvent: false});
      } else {
        this.mailSettings.get('enableOauth2').patchValue(false, {emitEvent: false});
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  private initTemplates(templates): void {
    templates.map(provider => {
      delete provider.additionalInfo;
      this.templates.set(provider.providerId, provider);
    });
    this.templateProvider.push(...Array.from(this.templates.keys()));
    this.templateProvider.sort();
  }

  private mailServerSettingsForm(): void {
    this.registerDisableOnLoadFormControl(this.mailSettings.get('smtpProtocol'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('enableTls'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('enableProxy'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('changePassword'));

    this.mailSettings.get('enableTls').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.enableTls(value));

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
    ).subscribe( value => {
      this.enableOauth2(value);
      this.enableProviderTenantIdChanged(this.mailSettings.get('providerId').value);
      if (value && !this.mailSettings.get('redirectUri').value) {
        this.mailSettings.get('redirectUri').patchValue(this.redirectURI(), {emitEvent: false});
      }
    });

    this.mailSettings.get('providerTenantId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(tenantId => {
      const authorizationUri = this.templates.get(this.mailServerOauth2Provider.OFFICE_365).authorizationUri.replace('%s', `${tenantId}`);
      const accessTokenUri = this.templates.get(this.mailServerOauth2Provider.OFFICE_365).accessTokenUri.replace('%s', `${tenantId}`);
      this.mailSettings.get('authUri').patchValue(authorizationUri, {emitEvent: false});
      this.mailSettings.get('tokenUri').patchValue(accessTokenUri, {emitEvent: false});
    });

    this.mailSettings.get('providerId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe( value => {
      if (value === this.mailServerOauth2Provider.CUSTOM || !value) {
        this.mailSettings.reset({...this.adminSettings.jsonValue, ...this.defaultConfiguration}, {emitEvent: false});
      } else {
        const config = this.templates.get(value);
        this.helpLink = config.helpLink;
        this.mailSettings.patchValue({
          smtpProtocol: SmtpProtocol[config.smtpProtocol],
          smtpHost: config.smtpHost,
          smtpPort: config.smtpPort,
          timeout: config.timeout,
          enableTls: config.enableTls,
          tlsVersion: config.tlsVersion,
          authUri: config.authorizationUri,
          tokenUri: config.accessTokenUri,
          scope: config.scope,
          enableOauth2: false,
          enableProxy: false,
          proxyHost: '',
          proxyPort: null,
          proxyUser: '',
          proxyPassword: '',
          clientId: '',
          clientSecret: '',
          providerTenantId: '',
          redirectUri: ''
        }, {emitEvent: false});
      }
      this.enableTls(this.mailSettings.get('enableTls').value);
      this.enableOauth2(this.mailSettings.get('enableOauth2').value);
      this.enableProviderTenantIdChanged(value);
    });
  }

  private domainFormConfiguration(): void {
    this.domainForm.get('name').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      value => this.mailSettings.get('redirectUri').patchValue(
        this.redirectURI(this.domainForm.get('scheme').value, value),
        {emitEvent: false}
      )
    );
    this.domainForm.get('scheme').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      (value) => this.mailSettings.get('redirectUri').patchValue(this.redirectURI(value), {emitEvent: false})
    );
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

  private enableOauth2(value: boolean): void {
    if (value) {
      this.mailSettings.get('clientId').enable({emitEvent: false});
      this.mailSettings.get('clientSecret').enable({emitEvent: false});
      this.mailSettings.get('redirectUri').enable({emitEvent: false});
      if (this.mailSettings.get('providerId').value === this.mailServerOauth2Provider.CUSTOM) {
        this.mailSettings.get('authUri').enable({emitEvent: false});
        this.mailSettings.get('tokenUri').enable({emitEvent: false});
      } else {
        this.mailSettings.get('authUri').disable({emitEvent: false});
        this.mailSettings.get('tokenUri').disable({emitEvent: false});
      }
    } else {
      this.mailSettings.get('clientId').disable({emitEvent: false});
      this.mailSettings.get('clientSecret').disable({emitEvent: false});
      this.mailSettings.get('redirectUri').disable({emitEvent: false});
      this.mailSettings.get('authUri').disable({emitEvent: false});
      this.mailSettings.get('tokenUri').disable({emitEvent: false});
    }
  }

  private enableProviderTenantIdChanged(value: string): void {
    if (value === this.mailServerOauth2Provider.OFFICE_365 && this.mailSettings.get('enableOauth2').value) {
      this.mailSettings.get('providerTenantId').enable({emitEvent: false});
    } else {
      this.mailSettings.get('providerTenantId').disable({emitEvent: false});
    }
  }

  private enableProxyChanged(): void {
    const enableProxy: boolean = this.mailSettings.get('enableProxy').value;
    if (enableProxy) {
      this.mailSettings.get('proxyHost').enable({emitEvent: false});
      this.mailSettings.get('proxyPort').enable({emitEvent: false});
      this.mailSettings.get('proxyUser').enable({emitEvent: false});
      this.mailSettings.get('proxyPassword').enable({emitEvent: false});
    } else {
      this.mailSettings.get('proxyHost').disable({emitEvent: false});
      this.mailSettings.get('proxyPort').disable({emitEvent: false});
      this.mailSettings.get('proxyUser').disable({emitEvent: false});
      this.mailSettings.get('proxyPassword').disable({emitEvent: false});
    }
  }

  private enableMailPassword(enable: boolean) {
    if (enable) {
      this.mailSettings.get('password').enable({emitEvent: false});
    } else {
      this.mailSettings.get('password').disable({emitEvent: false});
    }
  }

  private enableTls(enable: boolean): void {
    if (enable) {
      this.mailSettings.get('tlsVersion').enable({emitEvent: false});
    } else {
      this.mailSettings.get('tlsVersion').disable({emitEvent: false});
    }
  }

  sendTestMail(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettingsFormValue};
    this.adminService.sendTestMail(this.adminSettings).subscribe({
      next: () => this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.test-mail-sent'),
        type: 'success' })),
      error: error => this.store.dispatch(new ActionNotificationShow({message: error.error.message, type: 'error'}))
    });
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettingsFormValue};
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.showChangePassword = true;
        this.mailSettings.reset(this.adminSettings.jsonValue, {emitEvent: false});
        this.domainForm.reset(this.domainForm.value);
        this.parseUrl(this.adminSettings.jsonValue.redirectUri);
      }
    );
  }

  generateAccessToken(): void {
    this.adminService.generateAccessToken().subscribe(
      uri => this.window.location.href = uri
    );
  }

  redirectURI(schema?: DomainSchema, name?: string): string {
    const domainInfo = this.domainForm.value;
    if (domainInfo.name !== '') {
      const protocol = isDefined(schema) ? schema.toLowerCase() : domainInfo.scheme.toLowerCase();
      const domainName = isDefined(name) ? name : domainInfo.name;
      return `${protocol}://${domainName}${this.loginProcessingUrl}`;
    }
    return '';
  }

  private parseUrl(value: string): void {
    if (value) {
      const url = new URL(value);
      this.domainForm.get('scheme').patchValue(
        url.protocol.startsWith('https') ? DomainSchema.HTTPS : DomainSchema.HTTP, {emitEvent: false}
      );
      this.domainForm.get('name').patchValue(url.host, {emitEvent: false});
    }
  }

  get accessTokenButtonName(): string {
    return this.translate.instant(
      this.adminSettings.jsonValue.tokenGenerated ? 'admin.oauth2.update-access-token' : 'admin.oauth2.generate-access-token'
    );
  }

  get accessTokenStatus(): string {
    return this.translate.instant(
      this.adminSettings.jsonValue.tokenGenerated ? 'admin.oauth2.token-status-generated' : 'admin.oauth2.token-status-not-generated'
    );
  }

  confirmForm(): FormGroup {
    return this.mailSettings;
  }

  private get mailSettingsFormValue(): MailServerSettings {
    const formValue = this.mailSettings.getRawValue() as Required<typeof this.mailSettings.value>;
    delete formValue.changePassword;
    return formValue;
  }

  trackByParams(index: number): number {
    return index;
  }

  removeScope(i: number): void {
    const controller = this.mailSettings.get('scope') as UntypedFormArray;
    controller.removeAt(i);
    controller.markAsTouched();
    controller.markAsDirty();
  }

  addScope(event: MatChipInputEvent): void {
    const input = event.chipInput.inputElement;
    const value = event.value;
    const controller = this.mailSettings.get('scope') as UntypedFormArray;
    if ((value.trim() !== '')) {
      controller.push(this.fb.control(value.trim()));
      controller.markAsDirty();
    }

    if (input) {
      input.value = '';
    }
  }

  toggleEditMode(path: string): void {
    this.mailSettings.get(path).disabled ? this.mailSettings.get(path).enable() : this.mailSettings.get(path).disable();
  }

}

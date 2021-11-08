///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { AbstractControl, FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import {
  ClientAuthenticationMethod,
  DomainSchema,
  domainSchemaTranslations,
  MapperConfig,
  MapperConfigBasic,
  MapperConfigCustom,
  MapperConfigType,
  OAuth2ClientRegistrationTemplate,
  OAuth2DomainInfo,
  OAuth2Info, OAuth2MobileInfo,
  OAuth2ParamsInfo,
  OAuth2RegistrationInfo, PlatformType,
  platformTypeTranslations,
  TenantNameStrategy
} from '@shared/models/oauth2.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { WINDOW } from '@core/services/window.service';
import { forkJoin, Subscription } from 'rxjs';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { isDefined, isDefinedAndNotNull, randomAlphanumeric } from '@core/utils';
import { OAuth2Service } from '@core/http/oauth2.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-oauth2-settings',
  templateUrl: './oauth2-settings.component.html',
  styleUrls: ['./oauth2-settings.component.scss', './settings-card.scss']
})
export class OAuth2SettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private oauth2Service: OAuth2Service,
              private fb: FormBuilder,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  get oauth2ParamsInfos(): FormArray {
    return this.oauth2SettingsForm.get('oauth2ParamsInfos') as FormArray;
  }

  private URL_REGEXP = /^[A-Za-z][A-Za-z\d.+-]*:\/*(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?(?:\/[\w#!:.,?+=&%@\-/]*)?$/;
  private DOMAIN_AND_PORT_REGEXP = /^(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?$/;
  private subscriptions: Subscription[] = [];
  private templates = new Map<string, OAuth2ClientRegistrationTemplate>();
  private defaultProvider = {
    additionalInfo: {
      providerName: 'Custom'
    },
    clientAuthenticationMethod: ClientAuthenticationMethod.POST,
    userNameAttributeName: 'email',
    mapperConfig: {
      allowUserCreation: true,
      activateUser: false,
      type: MapperConfigType.BASIC,
      basic: {
        emailAttributeKey: 'email',
        tenantNameStrategy: TenantNameStrategy.DOMAIN,
        alwaysFullScreen: false
      }
    }
  };

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  oauth2SettingsForm: FormGroup;
  oauth2Info: OAuth2Info;

  clientAuthenticationMethods = Object.keys(ClientAuthenticationMethod);
  mapperConfigType = MapperConfigType;
  mapperConfigTypes = Object.keys(MapperConfigType);
  tenantNameStrategies = Object.keys(TenantNameStrategy);
  protocols = Object.values(DomainSchema);
  domainSchemaTranslations = domainSchemaTranslations;
  platformTypes = Object.values(PlatformType);
  platformTypeTranslations = platformTypeTranslations;

  templateProvider = ['Custom'];

  private loginProcessingUrl: string = this.route.snapshot.data.loginProcessingUrl;

  private static validateScope(control: AbstractControl): ValidationErrors | null {
    const scope: string[] = control.value;
    if (!scope || !scope.length) {
      return {
        required: true
      };
    }
    return null;
  }

  ngOnInit(): void {
    this.buildOAuth2SettingsForm();
    forkJoin([
      this.oauth2Service.getOAuth2Template(),
      this.oauth2Service.getOAuth2Settings()
    ]).subscribe(
      ([templates, oauth2Info]) => {
        this.initTemplates(templates);
        this.oauth2Info = oauth2Info;
        this.initOAuth2Settings(this.oauth2Info);
      }
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
  }

  private initTemplates(templates: OAuth2ClientRegistrationTemplate[]): void {
    templates.map(provider => {
      delete provider.additionalInfo;
      this.templates.set(provider.name, provider);
    });
    this.templateProvider.push(...Array.from(this.templates.keys()));
    this.templateProvider.sort();
  }

  private formBasicGroup(mapperConfigBasic?: MapperConfigBasic): FormGroup {
    let tenantNamePattern;
    if (mapperConfigBasic?.tenantNamePattern) {
      tenantNamePattern = mapperConfigBasic.tenantNamePattern;
    } else {
      tenantNamePattern = {value: null, disabled: true};
    }
    const basicGroup = this.fb.group({
      emailAttributeKey: [mapperConfigBasic?.emailAttributeKey ? mapperConfigBasic.emailAttributeKey : 'email',
        [Validators.required, Validators.maxLength(31)]],
      firstNameAttributeKey: [mapperConfigBasic?.firstNameAttributeKey ? mapperConfigBasic.firstNameAttributeKey : '',
        Validators.maxLength(31)],
      lastNameAttributeKey: [mapperConfigBasic?.lastNameAttributeKey ? mapperConfigBasic.lastNameAttributeKey : '',
        Validators.maxLength(31)],
      tenantNameStrategy: [mapperConfigBasic?.tenantNameStrategy ? mapperConfigBasic.tenantNameStrategy : TenantNameStrategy.DOMAIN],
      tenantNamePattern: [tenantNamePattern, [Validators.required, Validators.maxLength(255)]],
      customerNamePattern: [mapperConfigBasic?.customerNamePattern ? mapperConfigBasic.customerNamePattern : null,
        Validators.maxLength(255)],
      defaultDashboardName: [mapperConfigBasic?.defaultDashboardName ? mapperConfigBasic.defaultDashboardName : null,
        Validators.maxLength(255)],
      alwaysFullScreen: [isDefinedAndNotNull(mapperConfigBasic?.alwaysFullScreen) ? mapperConfigBasic.alwaysFullScreen : false]
    });

    this.subscriptions.push(basicGroup.get('tenantNameStrategy').valueChanges.subscribe((domain) => {
      if (domain === 'CUSTOM') {
        basicGroup.get('tenantNamePattern').enable();
      } else {
        basicGroup.get('tenantNamePattern').disable();
      }
    }));

    return basicGroup;
  }

  private formCustomGroup(mapperConfigCustom?: MapperConfigCustom): FormGroup {
    return this.fb.group({
      url: [mapperConfigCustom?.url ? mapperConfigCustom.url : null,
        [Validators.required, Validators.pattern(this.URL_REGEXP), Validators.maxLength(255)]],
      username: [mapperConfigCustom?.username ? mapperConfigCustom.username : null, Validators.maxLength(255)],
      password: [mapperConfigCustom?.password ? mapperConfigCustom.password : null, Validators.maxLength(255)]
    });
  }

  private buildOAuth2SettingsForm(): void {
    this.oauth2SettingsForm = this.fb.group({
      oauth2ParamsInfos: this.fb.array([]),
      enabled: [false]
    });
  }

  private initOAuth2Settings(oauth2Info: OAuth2Info): void {
    if (oauth2Info) {
      this.oauth2SettingsForm.patchValue({enabled: oauth2Info.enabled}, {emitEvent: false});
      oauth2Info.oauth2ParamsInfos.forEach((oauth2ParamsInfo) => {
        this.oauth2ParamsInfos.push(this.buildOAuth2ParamsInfoForm(oauth2ParamsInfo));
      });
    }
  }

  private uniqueDomainValidator(control: FormGroup): { [key: string]: boolean } | null {
    if (control.parent?.value) {
      const domain = control.value.name;
      const listProtocols = control.parent.getRawValue()
        .filter((domainInfo) => domainInfo.name === domain)
        .map((domainInfo) => domainInfo.scheme);
      if (listProtocols.length > 1 && listProtocols.indexOf(DomainSchema.MIXED) > -1 ||
        new Set(listProtocols).size !== listProtocols.length) {
        return {unique: true};
      }
    }
    return null;
  }

  private uniquePkgNameValidator(control: FormGroup): { [key: string]: boolean } | null {
    if (control.parent?.value) {
      const pkgName = control.value.pkgName;
      const mobileInfosList = control.parent.getRawValue()
        .filter((mobileInfo) => mobileInfo.pkgName === pkgName);
      if (mobileInfosList.length > 1) {
        return {unique: true};
      }
    }
    return null;
  }

  public domainListTittle(control: AbstractControl): string {
    const domainInfos = control.get('domainInfos').value as OAuth2DomainInfo[];
    if (domainInfos.length) {
      const domainList = new Set<string>();
      domainInfos.forEach((domain) => {
        domainList.add(domain.name);
      });
      return Array.from(domainList).join(', ');
    }
    return this.translate.instant('admin.oauth2.new-domain');
  }

  private buildOAuth2ParamsInfoForm(oauth2ParamsInfo?: OAuth2ParamsInfo): FormGroup {
    const formOAuth2Params = this.fb.group({
      domainInfos: this.fb.array([], Validators.required),
      mobileInfos: this.fb.array([]),
      clientRegistrations: this.fb.array([], Validators.required)
    });

    if (oauth2ParamsInfo) {
      oauth2ParamsInfo.domainInfos.forEach((domain) => {
        this.domainInfos(formOAuth2Params).push(this.buildDomainInfoForm(domain));
      });
      oauth2ParamsInfo.mobileInfos.forEach((mobile) => {
        this.mobileInfos(formOAuth2Params).push(this.buildMobileInfoForm(mobile));
      });
      oauth2ParamsInfo.clientRegistrations.forEach((registration) => {
        this.clientRegistrations(formOAuth2Params).push(this.buildRegistrationForm(registration));
      });
    } else {
      this.clientRegistrations(formOAuth2Params).push(this.buildRegistrationForm());
      this.domainInfos(formOAuth2Params).push(this.buildDomainInfoForm());
    }

    return formOAuth2Params;
  }

  private buildDomainInfoForm(domainInfo?: OAuth2DomainInfo): FormGroup {
    return this.fb.group({
      name: [domainInfo ? domainInfo.name : this.window.location.hostname, [
        Validators.required, Validators.maxLength(255),
        Validators.pattern(this.DOMAIN_AND_PORT_REGEXP)]],
      scheme: [domainInfo?.scheme ? domainInfo.scheme : DomainSchema.HTTPS, Validators.required]
    }, {validators: this.uniqueDomainValidator});
  }

  private buildMobileInfoForm(mobileInfo?: OAuth2MobileInfo): FormGroup {
    return this.fb.group({
      pkgName: [mobileInfo?.pkgName, [Validators.required]],
      appSecret: [mobileInfo?.appSecret, [Validators.required, Validators.minLength(16), Validators.maxLength(2048),
        Validators.pattern(/^[A-Za-z0-9]+$/)]],
    }, {validators: this.uniquePkgNameValidator});
  }

  private buildRegistrationForm(registration?: OAuth2RegistrationInfo): FormGroup {
    let additionalInfo = null;
    if (isDefinedAndNotNull(registration?.additionalInfo)) {
      additionalInfo = registration.additionalInfo;
      if (this.templateProvider.indexOf(additionalInfo.providerName) === -1) {
        additionalInfo.providerName = 'Custom';
      }
    }
    let defaultProviderName = 'Custom';
    if (this.templateProvider.indexOf('Google')) {
      defaultProviderName = 'Google';
    }

    const clientRegistrationFormGroup = this.fb.group({
      additionalInfo: this.fb.group({
        providerName: [additionalInfo?.providerName ? additionalInfo?.providerName : defaultProviderName, Validators.required]
      }),
      platforms: [registration?.platforms ? registration.platforms : []],
      loginButtonLabel: [registration?.loginButtonLabel ? registration.loginButtonLabel : null, Validators.required],
      loginButtonIcon: [registration?.loginButtonIcon ? registration.loginButtonIcon : null],
      clientId: [registration?.clientId ? registration.clientId : '', [Validators.required, Validators.maxLength(255)]],
      clientSecret: [registration?.clientSecret ? registration.clientSecret : '', [Validators.required, Validators.maxLength(2048)]],
      accessTokenUri: [registration?.accessTokenUri ? registration.accessTokenUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      authorizationUri: [registration?.authorizationUri ? registration.authorizationUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      scope: this.fb.array(registration?.scope ? registration.scope : [], OAuth2SettingsComponent.validateScope),
      jwkSetUri: [registration?.jwkSetUri ? registration.jwkSetUri : '', Validators.pattern(this.URL_REGEXP)],
      userInfoUri: [registration?.userInfoUri ? registration.userInfoUri : '',
        [Validators.pattern(this.URL_REGEXP)]],
      clientAuthenticationMethod: [
        registration?.clientAuthenticationMethod ? registration.clientAuthenticationMethod : ClientAuthenticationMethod.POST,
        Validators.required],
      userNameAttributeName: [
        registration?.userNameAttributeName ? registration.userNameAttributeName : 'email', Validators.required],
      mapperConfig: this.fb.group({
          allowUserCreation: [
            isDefinedAndNotNull(registration?.mapperConfig?.allowUserCreation) ?
              registration.mapperConfig.allowUserCreation : true
          ],
          activateUser: [
            isDefinedAndNotNull(registration?.mapperConfig?.activateUser) ? registration.mapperConfig.activateUser : false
          ],
          type: [
            registration?.mapperConfig?.type ? registration.mapperConfig.type : MapperConfigType.BASIC, Validators.required
          ]
        }
      )
    });

    if (registration) {
      this.changeMapperConfigType(clientRegistrationFormGroup, registration.mapperConfig.type, registration.mapperConfig);
    } else {
      this.changeMapperConfigType(clientRegistrationFormGroup, MapperConfigType.BASIC);
      this.setProviderDefaultValue(defaultProviderName, clientRegistrationFormGroup);
    }

    this.subscriptions.push(clientRegistrationFormGroup.get('mapperConfig.type').valueChanges.subscribe((value) => {
      this.changeMapperConfigType(clientRegistrationFormGroup, value);
    }));

    this.subscriptions.push(clientRegistrationFormGroup.get('additionalInfo.providerName').valueChanges.subscribe((provider) => {
      (clientRegistrationFormGroup.get('scope') as FormArray).clear();
      this.setProviderDefaultValue(provider, clientRegistrationFormGroup);
    }));

    return clientRegistrationFormGroup;
  }

  private setProviderDefaultValue(provider: string, clientRegistration: FormGroup) {
    if (provider === 'Custom') {
      clientRegistration.reset(this.defaultProvider, {emitEvent: false});
      clientRegistration.get('accessTokenUri').enable();
      clientRegistration.get('authorizationUri').enable();
      clientRegistration.get('jwkSetUri').enable();
      clientRegistration.get('userInfoUri').enable();
    } else {
      const template = this.templates.get(provider);
      template.clientId = '';
      template.clientSecret = '';
      template.scope.forEach(() => {
        (clientRegistration.get('scope') as FormArray).push(this.fb.control(''));
      });
      clientRegistration.get('accessTokenUri').disable();
      clientRegistration.get('authorizationUri').disable();
      clientRegistration.get('jwkSetUri').disable();
      clientRegistration.get('userInfoUri').disable();
      clientRegistration.patchValue(template);
    }
  }

  private changeMapperConfigType(control: AbstractControl, type: MapperConfigType, predefinedValue?: MapperConfig) {
    const mapperConfig = control.get('mapperConfig') as FormGroup;
    if (type === MapperConfigType.CUSTOM) {
      mapperConfig.removeControl('basic');
      mapperConfig.addControl('custom', this.formCustomGroup(predefinedValue?.custom));
    } else {
      mapperConfig.removeControl('custom');
      if (!mapperConfig.get('basic')) {
        mapperConfig.addControl('basic', this.formBasicGroup(predefinedValue?.basic));
      }
      if (type === MapperConfigType.GITHUB) {
        mapperConfig.get('basic.emailAttributeKey').disable();
        mapperConfig.get('basic.emailAttributeKey').patchValue(null, {emitEvent: false});
      } else {
        mapperConfig.get('basic.emailAttributeKey').enable();
      }
    }
  }

  save(): void {
    const setting = this.oauth2SettingsForm.getRawValue();
    this.oauth2Service.saveOAuth2Settings(setting).subscribe(
      (oauth2Settings) => {
        this.oauth2Info = oauth2Settings;
        this.oauth2SettingsForm.patchValue(this.oauth2SettingsForm, {emitEvent: false});
        this.oauth2SettingsForm.markAsUntouched();
        this.oauth2SettingsForm.markAsPristine();
      }
    );
  }

  confirmForm(): FormGroup {
    return this.oauth2SettingsForm;
  }

  addScope(event: MatChipInputEvent, control: AbstractControl): void {
    const input = event.input;
    const value = event.value;
    const controller = control.get('scope') as FormArray;
    if ((value.trim() !== '')) {
      controller.push(this.fb.control(value.trim()));
      controller.markAsDirty();
    }

    if (input) {
      input.value = '';
    }
  }

  removeScope(i: number, control: AbstractControl): void {
    const controller = control.get('scope') as FormArray;
    controller.removeAt(i);
    controller.markAsTouched();
    controller.markAsDirty();
  }

  addOAuth2ParamsInfo(): void {
    this.oauth2ParamsInfos.push(this.buildOAuth2ParamsInfoForm());
  }

  deleteOAuth2ParamsInfo($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const domainName = this.domainListTittle(this.oauth2ParamsInfos.at(index));
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-domain-title', {domainName: domainName || ''}),
      this.translate.instant('admin.oauth2.delete-domain-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.oauth2ParamsInfos.removeAt(index);
        this.oauth2ParamsInfos.markAsTouched();
        this.oauth2ParamsInfos.markAsDirty();
      }
    });
  }

  clientRegistrations(control: AbstractControl): FormArray {
    return control.get('clientRegistrations') as FormArray;
  }

  domainInfos(control: AbstractControl): FormArray {
    return control.get('domainInfos') as FormArray;
  }

  mobileInfos(control: AbstractControl): FormArray {
    return control.get('mobileInfos') as FormArray;
  }

  addRegistration(control: AbstractControl): void {
    this.clientRegistrations(control).push(this.buildRegistrationForm());
  }

  deleteRegistration($event: Event, control: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const providerName = this.clientRegistrations(control).at(index).get('additionalInfo.providerName').value;
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-registration-title', {name: providerName || ''}),
      this.translate.instant('admin.oauth2.delete-registration-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.clientRegistrations(control).removeAt(index);
        this.clientRegistrations(control).markAsTouched();
        this.clientRegistrations(control).markAsDirty();
      }
    });
  }

  toggleEditMode(control: AbstractControl, path: string) {
    control.get(path).disabled ? control.get(path).enable() : control.get(path).disable();
  }

  getProviderName(controller: AbstractControl): string {
    return controller.get('additionalInfo.providerName').value;
  }

  isCustomProvider(controller: AbstractControl): boolean {
    return this.getProviderName(controller) === 'Custom';
  }

  getHelpLink(controller: AbstractControl): string {
    const provider = controller.get('additionalInfo.providerName').value;
    if (provider === null || provider === 'Custom') {
      return '';
    }
    return this.templates.get(provider).helpLink;
  }

  addDomainInfo(control: AbstractControl): void {
    this.domainInfos(control).push(this.buildDomainInfoForm({
      name: '',
      scheme: DomainSchema.HTTPS
    }));
  }

  removeDomainInfo($event: Event, control: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.domainInfos(control).removeAt(index);
    this.domainInfos(control).markAsTouched();
    this.domainInfos(control).markAsDirty();
  }

  addMobileInfo(control: AbstractControl): void {
    this.mobileInfos(control).push(this.buildMobileInfoForm({
      pkgName: '',
      appSecret: randomAlphanumeric(24)
    }));
  }

  removeMobileInfo($event: Event, control: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.mobileInfos(control).removeAt(index);
    this.mobileInfos(control).markAsTouched();
    this.mobileInfos(control).markAsDirty();
  }

  redirectURI(control: AbstractControl, schema?: DomainSchema): string {
    const domainInfo = control.value as OAuth2DomainInfo;
    if (domainInfo.name !== '') {
      let protocol;
      if (isDefined(schema)) {
        protocol = schema.toLowerCase();
      } else {
        protocol = domainInfo.scheme === DomainSchema.MIXED ? DomainSchema.HTTPS.toLowerCase() : domainInfo.scheme.toLowerCase();
      }
      return `${protocol}://${domainInfo.name}${this.loginProcessingUrl}`;
    }
    return '';
  }

  redirectURIMixed(control: AbstractControl): string {
    return this.redirectURI(control, DomainSchema.HTTP);
  }

  trackByParams(index: number): number {
    return index;
  }
}

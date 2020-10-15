///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  ClientRegistration,
  DomainInfo,
  DomainSchema,
  domainSchemaTranslations,
  MapperConfig,
  MapperConfigBasic,
  MapperConfigCustom,
  MapperConfigType,
  OAuth2ClientRegistrationTemplate,
  OAuth2ClientsDomainParams,
  OAuth2ClientsParams,
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
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import { OAuth2Service } from '@core/http/oauth2.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-oauth2-settings',
  templateUrl: './oauth2-settings.component.html',
  styleUrls: ['./oauth2-settings.component.scss', './settings-card.scss']
})
export class OAuth2SettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

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
  auth2ClientsParams: OAuth2ClientsParams;

  clientAuthenticationMethods = Object.keys(ClientAuthenticationMethod);
  mapperConfigType = MapperConfigType;
  mapperConfigTypes = Object.keys(MapperConfigType);
  tenantNameStrategies = Object.keys(TenantNameStrategy);
  protocols = Object.keys(DomainSchema);
  domainSchemaTranslations = domainSchemaTranslations;

  templateProvider = ['Custom'];

  private loginProcessingUrl: string = this.route.snapshot.data.loginProcessingUrl;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private oauth2Service: OAuth2Service,
              private fb: FormBuilder,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit(): void {
    this.buildOAuth2SettingsForm();
    forkJoin([
      this.oauth2Service.getOAuth2Template(),
      this.oauth2Service.getOAuth2Settings()
    ]).subscribe(
      ([templates, auth2ClientsParams]) => {
        this.initTemplates(templates);
        this.auth2ClientsParams = auth2ClientsParams;
        this.initOAuth2Settings(this.auth2ClientsParams);
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

  get domainsParams(): FormArray {
    return this.oauth2SettingsForm.get('domainsParams') as FormArray;
  }

  private formBasicGroup(type: MapperConfigType, mapperConfigBasic?: MapperConfigBasic): FormGroup {
    let tenantNamePattern;
    if (mapperConfigBasic?.tenantNamePattern) {
      tenantNamePattern = mapperConfigBasic.tenantNamePattern;
    } else {
      tenantNamePattern = {value: null, disabled: true};
    }
    const basicGroup = this.fb.group({
      firstNameAttributeKey: [mapperConfigBasic?.firstNameAttributeKey ? mapperConfigBasic.firstNameAttributeKey : ''],
      lastNameAttributeKey: [mapperConfigBasic?.lastNameAttributeKey ? mapperConfigBasic.lastNameAttributeKey : ''],
      tenantNameStrategy: [mapperConfigBasic?.tenantNameStrategy ? mapperConfigBasic.tenantNameStrategy : TenantNameStrategy.DOMAIN],
      tenantNamePattern: [tenantNamePattern, Validators.required],
      customerNamePattern: [mapperConfigBasic?.customerNamePattern ? mapperConfigBasic.customerNamePattern : null],
      defaultDashboardName: [mapperConfigBasic?.defaultDashboardName ? mapperConfigBasic.defaultDashboardName : null],
      alwaysFullScreen: [isDefinedAndNotNull(mapperConfigBasic?.alwaysFullScreen) ? mapperConfigBasic.alwaysFullScreen : false]
    });

    if (MapperConfigType.GITHUB !== type) {
      basicGroup.addControl('emailAttributeKey',
        this.fb.control( mapperConfigBasic?.emailAttributeKey ? mapperConfigBasic.emailAttributeKey : 'email', Validators.required));
    }

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
      url: [mapperConfigCustom?.url ? mapperConfigCustom.url : null, [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      username: [mapperConfigCustom?.username ? mapperConfigCustom.username : null],
      password: [mapperConfigCustom?.password ? mapperConfigCustom.password : null]
    });
  }

  private buildOAuth2SettingsForm(): void {
    this.oauth2SettingsForm = this.fb.group({
      domainsParams: this.fb.array([]),
      enabled: [false]
    });
  }

  private initOAuth2Settings(auth2ClientsParams: OAuth2ClientsParams): void {
    if (auth2ClientsParams) {
      this.oauth2SettingsForm.patchValue({enabled: auth2ClientsParams.enabled}, {emitEvent: false});
      auth2ClientsParams.domainsParams.forEach((domain) => {
        this.domainsParams.push(this.buildDomainsForm(domain));
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

  public domainListTittle(control: AbstractControl): string {
    const domainInfos = control.get('domainInfos').value as DomainInfo[];
    if (domainInfos.length) {
      const domainList = new Set<string>();
      domainInfos.forEach((domain) => {
        domainList.add(domain.name);
      });
      return Array.from(domainList).join(', ');
    }
    return this.translate.instant('admin.oauth2.new-domain');
  }

  private buildDomainsForm(auth2ClientsDomainParams?: OAuth2ClientsDomainParams): FormGroup {
    const formDomain = this.fb.group({
      domainInfos: this.fb.array([], Validators.required),
      clientRegistrations: this.fb.array([], Validators.required)
    });

    if (auth2ClientsDomainParams) {
      auth2ClientsDomainParams.domainInfos.forEach((domain) => {
        this.clientDomainInfos(formDomain).push(this.buildDomainForm(domain));
      });
      auth2ClientsDomainParams.clientRegistrations.forEach((registration) => {
        this.clientDomainProviders(formDomain).push(this.buildProviderForm(registration));
      });
    } else {
      this.clientDomainProviders(formDomain).push(this.buildProviderForm());
      this.clientDomainInfos(formDomain).push(this.buildDomainForm());
    }

    return formDomain;
  }

  private buildDomainForm(domainInfo?: DomainInfo): FormGroup {
    const domain = this.fb.group({
      name: [domainInfo ? domainInfo.name : this.window.location.hostname, [
        Validators.required,
        Validators.pattern(this.DOMAIN_AND_PORT_REGEXP)]],
      scheme: [domainInfo?.scheme ? domainInfo.scheme : DomainSchema.HTTPS, Validators.required]
    }, {validators: this.uniqueDomainValidator});
    return domain;
  }

  private buildProviderForm(clientRegistration?: ClientRegistration): FormGroup {
    let additionalInfo = null;
    if (isDefinedAndNotNull(clientRegistration?.additionalInfo)) {
      additionalInfo = clientRegistration.additionalInfo;
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
      loginButtonLabel: [clientRegistration?.loginButtonLabel ? clientRegistration.loginButtonLabel : null, Validators.required],
      loginButtonIcon: [clientRegistration?.loginButtonIcon ? clientRegistration.loginButtonIcon : null],
      clientId: [clientRegistration?.clientId ? clientRegistration.clientId : '', Validators.required],
      clientSecret: [clientRegistration?.clientSecret ? clientRegistration.clientSecret : '', Validators.required],
      accessTokenUri: [clientRegistration?.accessTokenUri ? clientRegistration.accessTokenUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      authorizationUri: [clientRegistration?.authorizationUri ? clientRegistration.authorizationUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      scope: this.fb.array(clientRegistration?.scope ? clientRegistration.scope : [], this.validateScope),
      jwkSetUri: [clientRegistration?.jwkSetUri ? clientRegistration.jwkSetUri : '', Validators.pattern(this.URL_REGEXP)],
      userInfoUri: [clientRegistration?.userInfoUri ? clientRegistration.userInfoUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      clientAuthenticationMethod: [
        clientRegistration?.clientAuthenticationMethod ? clientRegistration.clientAuthenticationMethod : ClientAuthenticationMethod.POST,
        Validators.required],
      userNameAttributeName: [
        clientRegistration?.userNameAttributeName ? clientRegistration.userNameAttributeName : 'email', Validators.required],
      mapperConfig: this.fb.group({
          allowUserCreation: [
            isDefinedAndNotNull(clientRegistration?.mapperConfig?.allowUserCreation) ?
              clientRegistration.mapperConfig.allowUserCreation : true
          ],
          activateUser: [
            isDefinedAndNotNull(clientRegistration?.mapperConfig?.activateUser) ? clientRegistration.mapperConfig.activateUser : false
          ],
          type: [
            clientRegistration?.mapperConfig?.type ? clientRegistration.mapperConfig.type : MapperConfigType.BASIC, Validators.required
          ]
        }
      )
    });

    if (clientRegistration) {
      this.changeMapperConfigType(clientRegistrationFormGroup, clientRegistration.mapperConfig.type, clientRegistration.mapperConfig);
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

  private validateScope(control: AbstractControl): ValidationErrors | null {
    const scope: string[] = control.value;
    if (!scope || !scope.length) {
      return {
        required: true
      };
    }
    return null;
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
      if (mapperConfig.get('basic')) {
        mapperConfig.setControl('basic', this.formBasicGroup(type, predefinedValue?.basic));
      } else {
        mapperConfig.addControl('basic', this.formBasicGroup(type, predefinedValue?.basic));
      }
    }
  }

  save(): void {
    const setting = this.oauth2SettingsForm.getRawValue();
    this.oauth2Service.saveOAuth2Settings(setting).subscribe(
      (oauth2Settings) => {
        this.auth2ClientsParams = oauth2Settings;
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

  addDomain(): void {
    this.domainsParams.push(this.buildDomainsForm());
  }

  deleteDomain($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const domainName = this.domainListTittle(this.domainsParams.at(index));
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-domain-title', {domainName: domainName || ''}),
      this.translate.instant('admin.oauth2.delete-domain-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.domainsParams.removeAt(index);
        this.domainsParams.markAsTouched();
        this.domainsParams.markAsDirty();
      }
    });
  }

  clientDomainProviders(control: AbstractControl): FormArray {
    return control.get('clientRegistrations') as FormArray;
  }

  clientDomainInfos(control: AbstractControl): FormArray {
    return control.get('domainInfos') as FormArray;
  }

  addProvider(control: AbstractControl): void {
    this.clientDomainProviders(control).push(this.buildProviderForm());
  }

  deleteProvider($event: Event, control: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const providerName = this.clientDomainProviders(control).at(index).get('additionalInfo.providerName').value;
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-registration-title', {name: providerName || ''}),
      this.translate.instant('admin.oauth2.delete-registration-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.clientDomainProviders(control).removeAt(index);
        this.clientDomainProviders(control).markAsTouched();
        this.clientDomainProviders(control).markAsDirty();
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
    this.clientDomainInfos(control).push(this.buildDomainForm({
      name: '',
      scheme: DomainSchema.HTTPS
    }));
  }

  removeDomain($event: Event, control: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.clientDomainInfos(control).removeAt(index);
    this.clientDomainInfos(control).markAsTouched();
    this.clientDomainInfos(control).markAsDirty();
  }

  redirectURI(control: AbstractControl, schema?: DomainSchema): string {
    const domainInfo = control.value as DomainInfo;
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

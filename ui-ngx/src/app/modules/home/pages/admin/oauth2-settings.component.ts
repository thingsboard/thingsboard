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

import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, ViewContainerRef } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  ClientAuthenticationMethod,
  ClientProviderTemplated,
  ClientRegistration,
  MapperConfig,
  MapperConfigBasic,
  MapperConfigCustom,
  MapperConfigType,
  OAuth2Settings,
  TenantNameStrategy
} from '@shared/models/settings.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { WINDOW } from '@core/services/window.service';
import { forkJoin, Subscription } from 'rxjs';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import {
  EDIT_REDIRECT_URI_PANEL_DATA,
  EditRedirectUriPanelComponent,
  EditRedirectUriPanelData
} from './edit-redirect-uri-panel.component';

@Component({
  selector: 'tb-oauth2-settings',
  templateUrl: './oauth2-settings.component.html',
  styleUrls: ['./oauth2-settings.component.scss', './settings-card.scss']
})
export class OAuth2SettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

  private URL_REGEXP = /^[A-Za-z][A-Za-z\d.+-]*:\/*(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?(?:\/[\w#!:.,?+=&%@\-/]*)?$/;
  private subscriptions: Subscription[] = [];
  private templates = new Map<string, ClientProviderTemplated>();
  private defaultProvider = {
    additionalInfo: {
      providerName: 'Custom'
    },
    clientAuthenticationMethod: 'POST',
    userNameAttributeName: 'email',
    mapperConfig: {
      allowUserCreation: true,
      activateUser: false,
      type: 'BASIC',
      basic: {
        emailAttributeKey: 'email',
        tenantNameStrategy: 'DOMAIN',
        alwaysFullScreen: false
      }
    }
  };

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  oauth2SettingsForm: FormGroup;
  oauth2Settings: OAuth2Settings[];

  clientAuthenticationMethods: ClientAuthenticationMethod[] = ['BASIC', 'POST'];
  converterTypesExternalUser: MapperConfigType[] = ['BASIC', 'CUSTOM'];
  tenantNameStrategies: TenantNameStrategy[] = ['DOMAIN', 'EMAIL', 'CUSTOM'];

  templateProvider = ['Custom'];

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit(): void {
    this.buildOAuth2SettingsForm();
    forkJoin([
      this.adminService.getOAuth2Template(),
      this.adminService.getOAuth2Settings()
    ]).subscribe(
      ([templates, oauth2Settings]) => {
        this.initTemplates(templates);
        this.oauth2Settings = oauth2Settings;
        this.initOAuth2Settings(this.oauth2Settings);
      }
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
  }

  private initTemplates(templates: ClientProviderTemplated[]): void {
    templates.map(provider => this.templates.set(provider.name, provider));
    this.templateProvider.push(...Array.from(this.templates.keys()));
    this.templateProvider.sort();
  }

  get clientDomains(): FormArray {
    return this.oauth2SettingsForm.get('clientDomains') as FormArray;
  }

  private formBasicGroup(mapperConfigBasic?: MapperConfigBasic): FormGroup {
    let tenantNamePattern;
    if (mapperConfigBasic?.tenantNamePattern) {
      tenantNamePattern = mapperConfigBasic.tenantNamePattern;
    } else {
      tenantNamePattern = {value: null, disabled: true};
    }
    const basicGroup = this.fb.group({
      emailAttributeKey: [mapperConfigBasic?.emailAttributeKey ? mapperConfigBasic.emailAttributeKey : 'email', Validators.required],
      firstNameAttributeKey: [mapperConfigBasic?.firstNameAttributeKey ? mapperConfigBasic.firstNameAttributeKey : ''],
      lastNameAttributeKey: [mapperConfigBasic?.lastNameAttributeKey ? mapperConfigBasic.lastNameAttributeKey : ''],
      tenantNameStrategy: [mapperConfigBasic?.tenantNameStrategy ? mapperConfigBasic.tenantNameStrategy : 'DOMAIN'],
      tenantNamePattern: [tenantNamePattern, Validators.required],
      customerNamePattern: [mapperConfigBasic?.customerNamePattern ? mapperConfigBasic.customerNamePattern : null],
      defaultDashboardName: [mapperConfigBasic?.defaultDashboardName ? mapperConfigBasic.defaultDashboardName : null],
      alwaysFullScreen: [mapperConfigBasic?.alwaysFullScreen ? mapperConfigBasic.alwaysFullScreen : false]
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
      url: [mapperConfigCustom?.url ? mapperConfigCustom.url : null, [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      username: [mapperConfigCustom?.username ? mapperConfigCustom.username : null],
      password: [mapperConfigCustom?.password ? mapperConfigCustom.password : null]
    });
  }

  private buildOAuth2SettingsForm(): void {
    this.oauth2SettingsForm = this.fb.group({
      clientDomains: this.fb.array([])
    });
  }

  private initOAuth2Settings(oauth2Settings: OAuth2Settings[]): void {
    if (oauth2Settings) {
      oauth2Settings.forEach((domain) => {
        this.clientDomains.push(this.buildSettingsDomain(domain));
      });
    }
  }

  private uniqueDomainValidator(control: AbstractControl): { [key: string]: boolean } | null {
    if (control.root.value.clientDomains?.length > 1) {
      const listDomainName = [];
      control.root.value.clientDomains.forEach((domain) => {
        listDomainName.push(domain.domainName);
      });
      if (listDomainName.indexOf(control.value) > -1) {
        return {unique: true};
      }
    }
    return null;
  }

  private buildSettingsDomain(domainParams?: OAuth2Settings): FormGroup {
    let url = this.window.location.protocol + '//' + this.window.location.hostname;
    const port = this.window.location.port;
    if (port !== '80' && port !== '443') {
      url += ':' + port;
    }
    url += '/login/oauth2/code/';
    const formDomain = this.fb.group({
      domainName: [domainParams?.domainName ? domainParams.domainName : this.window.location.hostname, [
        Validators.required,
        Validators.pattern('((?![:/]).)*$'),
        this.uniqueDomainValidator]],
      redirectUriTemplate: [domainParams?.redirectUriTemplate ? domainParams.redirectUriTemplate : url, [
        Validators.required,
        Validators.pattern(this.URL_REGEXP)]],
      clientRegistrations: this.fb.array([], Validators.required)
    });

    this.subscriptions.push(formDomain.get('domainName').valueChanges.subscribe((domain) => {
      if (!domain) {
        domain = this.window.location.hostname;
      }
      const uri = this.window.location.protocol + `//${domain}/login/oauth2/code/`;
      formDomain.get('redirectUriTemplate').patchValue(uri, {emitEvent: false});
    }));

    if (domainParams) {
      domainParams.clientRegistrations.forEach((registration) => {
        this.clientDomainRegistrations(formDomain).push(this.buildSettingsRegistration(registration));
      });
    } else {
      this.clientDomainRegistrations(formDomain).push(this.buildSettingsRegistration());
    }

    return formDomain;
  }

  private buildSettingsRegistration(registrationData?: ClientRegistration): FormGroup {
    let additionalInfo = null;
    if (registrationData?.additionalInfo) {
      additionalInfo = JSON.parse(registrationData.additionalInfo);
      if (this.templateProvider.indexOf(additionalInfo.providerName) === -1) {
        additionalInfo.providerName = 'Custom';
      }
    }
    const clientRegistration = this.fb.group({
      id: this.fb.group({
        id: [registrationData?.id?.id ? registrationData.id.id : null],
        entityType: [registrationData?.id?.entityType ? registrationData.id.entityType : null]
      }),
      additionalInfo: this.fb.group({
        providerName: [additionalInfo?.providerName ? additionalInfo?.providerName : 'Custom', Validators.required]
      }),
      loginButtonLabel: [registrationData?.loginButtonLabel ? registrationData.loginButtonLabel : null, Validators.required],
      loginButtonIcon: [registrationData?.loginButtonIcon ? registrationData.loginButtonIcon : null],
      clientId: [registrationData?.clientId ? registrationData.clientId : '', Validators.required],
      clientSecret: [registrationData?.clientSecret ? registrationData.clientSecret : '', Validators.required],
      accessTokenUri: [registrationData?.accessTokenUri ? registrationData.accessTokenUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      authorizationUri: [registrationData?.authorizationUri ? registrationData.authorizationUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      scope: this.fb.array(registrationData?.scope ? registrationData.scope : []),
      jwkSetUri: [registrationData?.jwkSetUri ? registrationData.jwkSetUri : '', Validators.pattern(this.URL_REGEXP)],
      userInfoUri: [registrationData?.userInfoUri ? registrationData.userInfoUri : '',
        [Validators.required,
          Validators.pattern(this.URL_REGEXP)]],
      clientAuthenticationMethod: [
        registrationData?.clientAuthenticationMethod ? registrationData.clientAuthenticationMethod : 'POST', Validators.required],
      userNameAttributeName: [
        registrationData?.userNameAttributeName ? registrationData.userNameAttributeName : 'email', Validators.required],
      mapperConfig: this.fb.group({
          allowUserCreation: [registrationData?.mapperConfig?.allowUserCreation ? registrationData.mapperConfig.allowUserCreation : true],
          activateUser: [registrationData?.mapperConfig?.activateUser ? registrationData.mapperConfig.activateUser : false],
          type: [registrationData?.mapperConfig?.type ? registrationData.mapperConfig.type : 'BASIC', Validators.required]
        }
      )
    });

    if (registrationData) {
      this.changeMapperConfigType(clientRegistration, registrationData.mapperConfig.type, registrationData.mapperConfig);
    } else {
      this.changeMapperConfigType(clientRegistration, 'BASIC');
    }

    this.subscriptions.push(clientRegistration.get('mapperConfig.type').valueChanges.subscribe((value) => {
      this.changeMapperConfigType(clientRegistration, value);
    }));

    this.subscriptions.push(clientRegistration.get('additionalInfo.providerName').valueChanges.subscribe((provider) => {
      (clientRegistration.get('scope') as FormArray).clear();
      if (provider === 'Custom') {
        const defaultSettings = {...this.defaultProvider, ...{id: clientRegistration.get('id').value}};
        clientRegistration.reset(defaultSettings, {emitEvent: false});
        clientRegistration.get('accessTokenUri').enable();
        clientRegistration.get('authorizationUri').enable();
        clientRegistration.get('jwkSetUri').enable();
        clientRegistration.get('userInfoUri').enable();
      } else {
        const template = this.templates.get(provider);
        delete template.id;
        delete template.additionalInfo;
        template.clientId = '';
        template.clientSecret = '';
        template.scope.forEach(() => {
          (clientRegistration.get('scope') as FormArray).push(this.fb.control(''));
        });
        clientRegistration.get('accessTokenUri').disable();
        clientRegistration.get('authorizationUri').disable();
        clientRegistration.get('jwkSetUri').disable();
        clientRegistration.get('userInfoUri').disable();
        clientRegistration.patchValue(template, {emitEvent: false});
      }
    }));

    return clientRegistration;
  }

  private changeMapperConfigType(control: AbstractControl, type: MapperConfigType, predefinedValue?: MapperConfig) {
    const mapperConfig = control.get('mapperConfig') as FormGroup;
    if (type === 'BASIC') {
      mapperConfig.removeControl('custom');
      mapperConfig.addControl('basic', this.formBasicGroup(predefinedValue?.basic));
    } else {
      mapperConfig.removeControl('basic');
      mapperConfig.addControl('custom', this.formCustomGroup(predefinedValue?.custom));
    }
  }

  save(): void {
    const setting = this.prepareFormValue(this.oauth2SettingsForm.getRawValue().clientDomains);
    this.adminService.saveOAuth2Settings(setting).subscribe(
      (oauth2Settings) => {
        this.oauth2Settings = oauth2Settings;
        this.oauth2SettingsForm.markAsPristine();
        this.oauth2SettingsForm.markAsUntouched();
      }
    );
  }

  private prepareFormValue(formValue: OAuth2Settings[]): OAuth2Settings[]{
    formValue.forEach((setting) => {
      setting.clientRegistrations.forEach((registration) => {
        registration.additionalInfo = JSON.stringify(registration.additionalInfo);
        if (registration.id.id === null) {
          delete registration.id;
        }
      });
    });
    return formValue;
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
    }

    if (input) {
      input.value = '';
    }
  }

  removeScope(i: number, control: AbstractControl): void {
    const controller = control.get('scope') as FormArray;
    controller.removeAt(i);
  }

  addDomain(): void {
    this.clientDomains.push(this.buildSettingsDomain());
  }

  deleteDomain($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const domainName = this.clientDomains.at(index).get('domainName').value;
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-domain-title', {domainName: domainName || ''}),
      this.translate.instant('admin.oauth2.delete-domain-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        if (index < this.oauth2Settings.length) {
          this.adminService.deleteOAuth2Domain(this.oauth2Settings[index].domainName).subscribe(() => {
            this.oauth2Settings.splice(index, 1);
            this.clientDomains.removeAt(index);
          });
        } else {
          this.clientDomains.removeAt(index);
        }
      }
    });
  }

  clientDomainRegistrations(control: AbstractControl): FormArray {
    return control.get('clientRegistrations') as FormArray;
  }

  addRegistration(control: AbstractControl): void {
    this.clientDomainRegistrations(control).push(this.buildSettingsRegistration());
  }

  deleteRegistration($event: Event, control: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const providerName = this.clientDomainRegistrations(control).at(index).get('additionalInfo.providerName').value;
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-registration-title', {name: providerName || ''}),
      this.translate.instant('admin.oauth2.delete-registration-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        const registrationId = this.clientDomainRegistrations(control).at(index).get('id.id').value;
        if (registrationId) {
          this.adminService.deleteOAuthCclientRegistrationId(registrationId).subscribe(() => {
            this.clientDomainRegistrations(control).removeAt(index);
          });
        } else {
          this.clientDomainRegistrations(control).removeAt(index);
        }
      }
    });
  }

  editRedirectURI($event: MouseEvent, index: number) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'bottom',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const redirectURI = this.clientDomains.at(index).get('redirectUriTemplate').value;

    const injectionTokens = new WeakMap<any, any>([
      [EDIT_REDIRECT_URI_PANEL_DATA, {
        redirectURI
      } as EditRedirectUriPanelData],
      [OverlayRef, overlayRef]
    ]);
    const injector = new PortalInjector(this.viewContainerRef.injector, injectionTokens);
    const componentRef = overlayRef.attach(new ComponentPortal(EditRedirectUriPanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result !== null) {
        const attributeValue = componentRef.instance.result;
        this.clientDomains.at(index).get('redirectUriTemplate').patchValue(attributeValue);
        this.clientDomains.at(index).get('redirectUriTemplate').markAsDirty();
      }
    });
  }

  toggleEditMode(control: AbstractControl, path: string) {
    control.get(path).disabled ? control.get(path).enable() : control.get(path).disable();
  }

  getProviderName(controller: AbstractControl): string {
    return controller.get('additionalInfo.providerName').value;
  }

  getHelpLink(controller: AbstractControl): string {
    const provider = controller.get('additionalInfo.providerName').value;
    if (provider === null || provider === 'Custom') {
      return '';
    }
    return this.templates.get(provider).helpLink;
  }
}

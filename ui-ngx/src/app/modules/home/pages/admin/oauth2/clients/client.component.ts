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

import { ChangeDetectorRef, Component, Inject, Input, OnDestroy, Optional } from '@angular/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import {
  ClientAuthenticationMethod,
  MapperType,
  OAuth2BasicMapperConfig,
  OAuth2Client,
  OAuth2ClientInfo,
  OAuth2ClientRegistrationTemplate,
  OAuth2CustomMapperConfig,
  OAuth2MapperConfig,
  PlatformType,
  platformTypeTranslations,
  TenantNameStrategyType
} from '@shared/models/oauth2.models';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AbstractControl, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import { OAuth2Service } from '@core/http/oauth2.service';
import { Subscription } from 'rxjs';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { PageLink } from '@shared/models/page/page-link';
import { coerceBoolean } from '@app/shared/decorators/coercion';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
    selector: 'tb-client',
    templateUrl: './client.component.html',
    styleUrls: ['./client.component.scss'],
    standalone: false
})
export class ClientComponent extends EntityComponent<OAuth2Client, PageLink, OAuth2ClientInfo> implements OnDestroy {

  @Input()
  @coerceBoolean()
  readonly createNewDialog = false;

  templateProvider = ['Custom'];
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
      type: MapperType.BASIC,
      basic: {
        emailAttributeKey: 'email',
        tenantNameStrategy: TenantNameStrategyType.DOMAIN,
        alwaysFullScreen: false
      }
    }
  };

  private URL_REGEXP = /^[A-Za-z][A-Za-z\d.+-]*:\/*(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?(?:\/[\w#!:.,?+=&%@\-/]*)?$/;

  private subscriptions: Array<Subscription> = [];

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  clientAuthenticationMethods = Object.keys(ClientAuthenticationMethod);
  mapperType = MapperType;
  mapperTypes = Object.keys(MapperType);
  tenantNameStrategies = Object.keys(TenantNameStrategyType);
  platformTypes = Object.values(PlatformType);
  platformTypeTranslations = platformTypeTranslations;
  generalSettingsMode = true;
  advancedExpanded = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private oauth2Service: OAuth2Service,
              @Optional() @Inject('entity') protected entityValue: OAuth2Client,
              @Optional() @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo>,
              protected cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
    this.oauth2Service.getOAuth2Template().subscribe(templates => {
      this.initTemplates(templates);
    });
    if (getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN) {
      this.platformTypes = this.platformTypes.filter(item => item !== PlatformType.WEB);
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
  }

  buildForm(entity: OAuth2Client): UntypedFormGroup {
    return this.fb.group({
      title: [entity?.title ? entity.title : '', [Validators.required, Validators.maxLength(100)]],
      additionalInfo: this.fb.group({
        providerName: [entity?.additionalInfo?.providerName ? entity?.additionalInfo?.providerName : '', Validators.required]
      }),
      platforms: [entity?.platforms ? entity.platforms : []],
      clientId: [entity?.clientId ? entity.clientId : '', [Validators.required, Validators.maxLength(255)]],
      clientSecret: [entity?.clientSecret ? entity.clientSecret : '', [Validators.required, Validators.maxLength(2048)]],
      accessTokenUri: [entity?.accessTokenUri ? entity.accessTokenUri : '',
        [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      authorizationUri: [entity?.authorizationUri ? entity.authorizationUri : '',
        [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      jwkSetUri: [entity?.jwkSetUri ? entity.jwkSetUri : '', Validators.pattern(this.URL_REGEXP)],
      userInfoUri: [entity?.userInfoUri ? entity.userInfoUri : '', [Validators.pattern(this.URL_REGEXP)]],
      clientAuthenticationMethod: [entity?.clientAuthenticationMethod ?
        entity.clientAuthenticationMethod : ClientAuthenticationMethod.POST, Validators.required],
      loginButtonLabel: [entity?.loginButtonLabel ? entity.loginButtonLabel : null, Validators.required],
      loginButtonIcon: [entity?.loginButtonIcon ? entity.loginButtonIcon : null],
      userNameAttributeName: [entity?.userNameAttributeName ? entity.userNameAttributeName : 'email', Validators.required],
      scope: [entity?.scope ? entity.scope : [], Validators.required],
      mapperConfig: this.fb.group({
        allowUserCreation: [isDefinedAndNotNull(entity?.mapperConfig?.allowUserCreation) ?
          entity.mapperConfig.allowUserCreation : true],
        activateUser: [isDefinedAndNotNull(entity?.mapperConfig?.activateUser) ? entity.mapperConfig.activateUser : false],
        type: [entity?.mapperConfig?.type ? entity.mapperConfig.type : MapperType.BASIC, Validators.required]
      })
    });
  }

  updateForm(entity: OAuth2Client) {
    this.entityForm.patchValue({
      title: entity.title,
      additionalInfo: {
        providerName: entity.additionalInfo.providerName
      },
      platforms: entity.platforms,
      clientId: entity.clientId,
      clientSecret: entity.clientSecret,
      accessTokenUri: entity.accessTokenUri,
      scope: entity.scope,
      authorizationUri: entity.authorizationUri,
      jwkSetUri: entity.jwkSetUri,
      userInfoUri: entity.userInfoUri,
      clientAuthenticationMethod: entity.clientAuthenticationMethod,
      loginButtonLabel: entity.loginButtonLabel,
      loginButtonIcon: entity.loginButtonIcon,
      userNameAttributeName: entity.userNameAttributeName,
      mapperConfig: {
        allowUserCreation: entity.mapperConfig.allowUserCreation,
        activateUser: entity.mapperConfig.activateUser,
        type: entity.mapperConfig.type
      }
    }, {emitEvent: false});

    this.changeMapperConfigType(this.entityForm, this.entityValue.mapperConfig.type, this.entityValue.mapperConfig);
  }

  getProviderName(): string {
    return this.entityForm.get('additionalInfo.providerName').value;
  }

  isCustomProvider(): boolean {
    return this.getProviderName() === 'Custom';
  }

  private initTemplates(templates: OAuth2ClientRegistrationTemplate[]): void {
    templates.map(provider => {
      delete provider.additionalInfo;
      this.templates.set(provider.name, provider);
    });
    this.templateProvider.push(...Array.from(this.templates.keys()));
    this.templateProvider.sort();

    let additionalInfo = null;
    if (isDefinedAndNotNull(this.entityValue?.additionalInfo)) {
      additionalInfo = this.entityValue.additionalInfo;
      if (this.templateProvider.indexOf(additionalInfo.providerName) === -1) {
        additionalInfo.providerName = 'Custom';
      }
    }
    let defaultProviderName = 'Custom';
    if (this.templateProvider.indexOf('Google')) {
      defaultProviderName = 'Google';
    }

    this.entityForm.get('additionalInfo.providerName').setValue(additionalInfo?.providerName ?
      additionalInfo?.providerName : defaultProviderName);

    this.changeMapperConfigType(this.entityForm, MapperType.BASIC);
    if (this.createNewDialog || this.isAdd) {
      this.setProviderDefaultValue(defaultProviderName, this.entityForm);
    }

    this.subscriptions.push(this.entityForm.get('mapperConfig.type').valueChanges.subscribe((value) => {
      this.changeMapperConfigType(this.entityForm, value);
    }));

    this.subscriptions.push(this.entityForm.get('additionalInfo.providerName').valueChanges.subscribe((provider) => {
      this.entityForm.get('scope').setValue([]);
      this.setProviderDefaultValue(provider, this.entityForm);
    }));
  }

  private changeMapperConfigType(control: AbstractControl, type: MapperType, predefinedValue?: OAuth2MapperConfig) {
    const mapperConfig = control.get('mapperConfig') as UntypedFormGroup;
    if (type === MapperType.CUSTOM) {
      mapperConfig.removeControl('basic');
      if (!mapperConfig.get('custom')) {
        mapperConfig.addControl('custom', this.formCustomGroup(predefinedValue?.custom));
      } else {
        mapperConfig.get('custom').patchValue(predefinedValue.custom, {emitEvent: false});
      }
    } else {
      mapperConfig.removeControl('custom');
      if (!mapperConfig.get('basic')) {
        mapperConfig.addControl('basic', this.formBasicGroup(predefinedValue?.basic));
      } else if (predefinedValue?.basic) {
        mapperConfig.get('basic').patchValue(predefinedValue.basic, {emitEvent: false});
        mapperConfig.get('basic.tenantNameStrategy').updateValueAndValidity({onlySelf: true});
      }
      if (type === MapperType.GITHUB) {
        mapperConfig.get('basic.emailAttributeKey').disable();
        mapperConfig.get('basic.emailAttributeKey').patchValue(null, {emitEvent: false});
      } else {
        if (this.createNewDialog || this.isEdit || this.isAdd) {
          mapperConfig.get('basic.emailAttributeKey').enable();
        }
      }
    }
  }

  private formCustomGroup(mapperConfigCustom?: OAuth2CustomMapperConfig): UntypedFormGroup {
    const customGroup = this.fb.group({
      url: [mapperConfigCustom?.url ? mapperConfigCustom.url : null,
        [Validators.required, Validators.pattern(this.URL_REGEXP), Validators.maxLength(255)]],
      username: [mapperConfigCustom?.username ? mapperConfigCustom.username : null, Validators.maxLength(255)],
      password: [mapperConfigCustom?.password ? mapperConfigCustom.password : null, Validators.maxLength(255)],
      sendToken: [isDefinedAndNotNull(mapperConfigCustom?.sendToken) ? mapperConfigCustom.sendToken : false]
    });
    if (!this.createNewDialog && !(this.isEdit || this.isAdd)) {
      customGroup.disable();
    }
    return customGroup;
  }

  private formBasicGroup(mapperConfigBasic?: OAuth2BasicMapperConfig): UntypedFormGroup {
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
      tenantNameStrategy: [mapperConfigBasic?.tenantNameStrategy ? mapperConfigBasic.tenantNameStrategy : TenantNameStrategyType.DOMAIN],
      tenantNamePattern: [tenantNamePattern, [Validators.required, Validators.maxLength(255)]],
      customerNamePattern: [mapperConfigBasic?.customerNamePattern ? mapperConfigBasic.customerNamePattern : null,
        Validators.maxLength(255)],
      defaultDashboardName: [mapperConfigBasic?.defaultDashboardName ? mapperConfigBasic.defaultDashboardName : null,
        Validators.maxLength(255)],
      alwaysFullScreen: [isDefinedAndNotNull(mapperConfigBasic?.alwaysFullScreen) ? mapperConfigBasic.alwaysFullScreen : false]
    });

    if (!this.createNewDialog && !(this.isEdit || this.isAdd)) {
      basicGroup.disable();
    }

    this.subscriptions.push(basicGroup.get('tenantNameStrategy').valueChanges.subscribe((domain) => {
      if (domain === 'CUSTOM' && (this.createNewDialog || this.isEdit || this.isAdd)) {
        basicGroup.get('tenantNamePattern').enable();
      } else {
        basicGroup.get('tenantNamePattern').disable();
      }
    }));

    return basicGroup;
  }

  private setProviderDefaultValue(provider: string, clientRegistration: UntypedFormGroup) {
    if (provider === 'Custom') {
      const title = clientRegistration.get('title').value;
      clientRegistration.reset(this.defaultProvider, {emitEvent: false});
      clientRegistration.patchValue({title}, {emitEvent: false});
      this.advancedExpanded = true;
    } else {
      const template = this.templates.get(provider);
      template.clientId = '';
      template.clientSecret = '';
      clientRegistration.patchValue(template);
    }
  }

}

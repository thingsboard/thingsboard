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
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  ClientAuthenticationMethod,
  ClientRegistration,
  DomainParams,
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
import { Subscription } from 'rxjs';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-oauth2-settings',
  templateUrl: './oauth2-settings.component.html',
  styleUrls: ['./oauth2-settings.component.scss', './settings-card.scss']
})
export class OAuth2SettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

  private URL_REGEXP = /^[A-Za-z][A-Za-z\d.+-]*:\/*(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?(?:\/[\w#!:.?+=&%@\-/]*)?$/;
  private subscriptions: Subscription[] = [];

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  oauth2SettingsForm: FormGroup;
  oauth2Settings: OAuth2Settings;

  clientAuthenticationMethods: ClientAuthenticationMethod[] = ['BASIC', 'POST'];
  converterTypesExternalUser: MapperConfigType[] = ['BASIC', 'CUSTOM'];
  tenantNameStrategies: TenantNameStrategy[] = ['DOMAIN', 'EMAIL', 'CUSTOM'];

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private fb: FormBuilder,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit(): void {
    this.buildOAuth2SettingsForm();
    this.adminService.getOAuth2Settings().subscribe(
      (oauth2Settings) => {
        this.oauth2Settings = oauth2Settings;
        this.initOAuth2Settings(this.oauth2Settings);
        this.oauth2SettingsForm.reset(this.oauth2Settings);
      }
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    })
  }

  get clientsDomainsParams(): FormArray {
    return this.oauth2SettingsForm.get('clientsDomainsParams') as FormArray;
  }

  private get formBasicGroup(): FormGroup {
    const basicGroup = this.fb.group({
      emailAttributeKey: ['email', [Validators.required]],
      firstNameAttributeKey: [''],
      lastNameAttributeKey: [''],
      tenantNameStrategy: ['DOMAIN'],
      tenantNamePattern: [null],
      customerNamePattern: [null],
      defaultDashboardName: [null],
      alwaysFullScreen: [false],
    });

    this.subscriptions.push(basicGroup.get('tenantNameStrategy').valueChanges.subscribe((domain) => {
      if (domain === 'CUSTOM') {
        basicGroup.get('tenantNamePattern').setValidators(Validators.required);
      } else {
        basicGroup.get('tenantNamePattern').clearValidators();
      }
    }));

    return basicGroup;
  }

  get formCustomGroup(): FormGroup {
    return this.fb.group({
      url: [null, [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      username: [null],
      password: [null]
    })
  }

  private buildOAuth2SettingsForm(): void {
    this.oauth2SettingsForm = this.fb.group({
      clientsDomainsParams: this.fb.array([], Validators.required)
    });
  }

  private initOAuth2Settings(oauth2Settings: OAuth2Settings): void {
    if (oauth2Settings.clientsDomainsParams) {
      oauth2Settings.clientsDomainsParams.forEach((domaindomain) => {
        this.clientsDomainsParams.push(this.buildSettingsDomain(domaindomain));
      });
    }
  }

  private uniqueDomainValidator(control: AbstractControl): { [key: string]: boolean } | null {
    if (control.value !== null && control?.root) {
      const listDomainName = [];
      control.root.value.clientsDomainsParams.forEach((domain) => {
        listDomainName.push(domain.domainName);
      })
      if (listDomainName.indexOf(control.value) > -1) {
        return {unique: true};
      }
    }
    return null;
  }

  private uniqueRegistrationIdValidator(control: AbstractControl): { [key: string]: boolean } | null {
    if (control.value !== null && control?.root) {
      const listRegistration = [];
      control.root.value.clientsDomainsParams.forEach((domain) => {
        domain.clientRegistrations.forEach((client) => {
          listRegistration.push(client.registrationId);
        })
      })
      if (listRegistration.indexOf(control.value) > -1) {
        return {unique: true};
      }
    }
    return null;
  }

  private buildSettingsDomain(domainParams?: DomainParams): FormGroup {
    let url = this.window.location.protocol + '//' + this.window.location.hostname;
    const port = this.window.location.port;
    if (port !== '80' && port !== '443') {
      url += ':' + port;
    }
    url += '/login/oauth2/code/';
    const formDomain = this.fb.group({
      domainName: [null, [Validators.required, Validators.pattern('((?![:/]).)*$'), this.uniqueDomainValidator]],
      redirectUriTemplate: [url, [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      clientRegistrations: this.fb.array([], Validators.required)
    });

    this.subscriptions.push(formDomain.get('domainName').valueChanges.subscribe((domain) => {
      if (!domain) {
        domain = this.window.location.hostname
      }
      const uri = this.window.location.protocol + `//${domain}/login/oauth2/code/`;
      formDomain.get('redirectUriTemplate').patchValue(uri);
    }));

    if (domainParams) {
      domainParams.clientRegistrations.forEach((registration) => {
        this.clientDomainRegistrations(formDomain).push(this.buildSettingsRegistration(registration));
      })
    } else {
      this.clientDomainRegistrations(formDomain).push(this.buildSettingsRegistration());
    }

    return formDomain;
  }

  private buildSettingsRegistration(registrationData?: ClientRegistration): FormGroup {
    const clientRegistration = this.fb.group({
      registrationId: [null, [Validators.required, this.uniqueRegistrationIdValidator]],
      loginButtonLabel: [null, [Validators.required]],
      loginButtonIcon: [null],
      clientId: ['', [Validators.required]],
      clientSecret: ['', [Validators.required]],
      accessTokenUri: ['', [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      authorizationUri: ['', [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      scope: this.fb.array([], [Validators.required]),
      jwkSetUri: ['', [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      userInfoUri: ['', [Validators.required, Validators.pattern(this.URL_REGEXP)]],
      clientAuthenticationMethod: ['POST', [Validators.required]],
      userNameAttributeName: ['email', [Validators.required]],
      mapperConfig: this.fb.group({
          allowUserCreation: [true],
          activateUser: [false],
          type: ['BASIC', [Validators.required]],
          basic: this.formBasicGroup
        }
      )
    });

    this.subscriptions.push(clientRegistration.get('mapperConfig.type').valueChanges.subscribe((value) => {
      const mapperConfig = clientRegistration.get('mapperConfig') as FormGroup;
      if (value === 'BASIC') {
        mapperConfig.removeControl('custom');
        mapperConfig.addControl('basic', this.formBasicGroup);
      } else {
        mapperConfig.removeControl('basic');
        mapperConfig.addControl('custom', this.formCustomGroup);
      }
    }));

    if (registrationData) {
      registrationData.scope.forEach(() => {
        (clientRegistration.get('scope') as FormArray).push(this.fb.control(''))
      })
      if (registrationData.mapperConfig.type !== 'BASIC') {
        clientRegistration.get('mapperConfig.type').patchValue('CUSTOM');
      }
    }

    return clientRegistration;
  }

  save(): void {
    this.adminService.saveOAuth2Settings(this.oauth2SettingsForm.value).subscribe(
      (oauth2Settings) => {
        this.oauth2Settings = oauth2Settings;
        this.oauth2SettingsForm.markAsPristine();
        this.oauth2SettingsForm.markAsUntouched();
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
    this.clientsDomainsParams.push(this.buildSettingsDomain());
  }

  deleteDomain($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const domainName = this.clientsDomainsParams.at(index).get('domainName').value;
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-domain-title', {domainName: domainName || ''}),
      this.translate.instant('admin.oauth2.delete-domain-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.clientsDomainsParams.removeAt(index);
      }
    })
  }

  clientDomainRegistrations(control: AbstractControl): FormArray {
    return control.get('clientRegistrations') as FormArray;
  }

  addRegistration(control: AbstractControl): void {
    this.clientDomainRegistrations(control).push(this.buildSettingsRegistration());
  }

  deleteRegistration($event: Event, controler: AbstractControl, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }

    const registrationId = this.clientDomainRegistrations(controler).at(index).get('registrationId').value;
    this.dialogService.confirm(
      this.translate.instant('admin.oauth2.delete-registration-title', {name: registrationId || ''}),
      this.translate.instant('admin.oauth2.delete-registration-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.clientDomainRegistrations(controler).removeAt(index);
      }
    })
  }
}

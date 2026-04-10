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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup, NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  defaultGoogleMapProviderSettings,
  defaultHereMapProviderSettings,
  defaultImageMapProviderSettings,
  defaultOpenStreetMapProviderSettings, defaultTencentMapProviderSettings,
  GoogleMapProviderSettings,
  HereMapProviderSettings,
  ImageMapProviderSettings,
  MapProviders,
  MapProviderSettings,
  mapProviderTranslationMap,
  OpenStreetMapProviderSettings,
  TencentMapProviderSettings
} from '@home/components/widget/lib/maps-legacy/map-models';
import { extractType } from '@core/utils';
import { IAliasController } from '@core/api/widget-api.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-map-provider-settings',
    templateUrl: './map-provider-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapProviderSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MapProviderSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MapProviderSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  aliasController: IAliasController;

  @Input()
  disabled: boolean;

  @Input()
  ignoreImageMap = false;

  private modelValue: MapProviderSettings;

  private propagateChange = null;

  public providerSettingsFormGroup: UntypedFormGroup;

  mapProviders = Object.values(MapProviders);

  mapProvider = MapProviders;

  mapProviderTranslations = mapProviderTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    if (this.ignoreImageMap) {
      this.mapProviders = this.mapProviders.filter((provider) => provider !== MapProviders.image);
    }
    this.providerSettingsFormGroup = this.fb.group({
      provider: [null, [Validators.required]],
      googleProviderSettings: [null, []],
      openstreetProviderSettings: [null, []],
      hereProviderSettings: [null, []],
      imageMapProviderSettings: [null, []],
      tencentMapProviderSettings: [null, []]
    });
    this.providerSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.providerSettingsFormGroup.get('provider').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.providerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.providerSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapProviderSettings): void {
    this.modelValue = value;
    const provider = value?.provider;
    const googleProviderSettings = extractType<GoogleMapProviderSettings>(value, Object.keys(defaultGoogleMapProviderSettings) as (keyof GoogleMapProviderSettings)[]);
    const openstreetProviderSettings = extractType<OpenStreetMapProviderSettings>(value, Object.keys(defaultOpenStreetMapProviderSettings) as (keyof OpenStreetMapProviderSettings)[]);
    const hereProviderSettings = extractType<HereMapProviderSettings>(value, Object.keys(defaultHereMapProviderSettings) as (keyof HereMapProviderSettings)[]);
    const imageMapProviderSettings = extractType<ImageMapProviderSettings>(value, Object.keys(defaultImageMapProviderSettings) as (keyof ImageMapProviderSettings)[]);
    const tencentMapProviderSettings = extractType<TencentMapProviderSettings>(value, Object.keys(defaultTencentMapProviderSettings) as (keyof TencentMapProviderSettings)[]);
    this.providerSettingsFormGroup.patchValue(
      {
        provider,
        googleProviderSettings,
        openstreetProviderSettings,
        hereProviderSettings,
        imageMapProviderSettings,
        tencentMapProviderSettings
      }, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.providerSettingsFormGroup.valid ? null : {
      mapProviderSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: {
      provider: MapProviders,
      googleProviderSettings: GoogleMapProviderSettings,
      openstreetProviderSettings: OpenStreetMapProviderSettings,
      hereProviderSettings: HereMapProviderSettings,
      imageMapProviderSettings: ImageMapProviderSettings,
      tencentMapProviderSettings: TencentMapProviderSettings
    } = this.providerSettingsFormGroup.value;
    this.modelValue = {
      provider: value.provider,
      ...value.googleProviderSettings,
      ...value.openstreetProviderSettings,
      ...value.hereProviderSettings,
      ...value.imageMapProviderSettings,
      ...value.tencentMapProviderSettings
    };
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const provider: MapProviders = this.providerSettingsFormGroup.get('provider').value;
    this.providerSettingsFormGroup.disable({emitEvent: false});
    this.providerSettingsFormGroup.get('provider').enable({emitEvent: false});
    switch (provider) {
      case MapProviders.google:
        this.providerSettingsFormGroup.get('googleProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('googleProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.openstreet:
        this.providerSettingsFormGroup.get('openstreetProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('openstreetProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.here:
        this.providerSettingsFormGroup.get('hereProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('hereProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.image:
        this.providerSettingsFormGroup.get('imageMapProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('imageMapProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.tencent:
        this.providerSettingsFormGroup.get('tencentMapProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('tencentMapProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
    }
  }
}

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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  defaultLayerTitle,
  defaultMapLayerSettings,
  googleMapLayerTranslationMap,
  googleMapLayerTypes,
  hereLayerTranslationMap,
  hereLayerTypes,
  MapLayerSettings,
  MapProvider,
  mapProviders,
  mapProviderTranslationMap,
  openStreetLayerTypes,
  openStreetMapLayerTranslationMap, referenceLayerTypes, referenceLayerTypeTranslationMap,
  tencentLayerTranslationMap,
  tencentLayerTypes
} from '@shared/models/widget/maps/map.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-map-layer-settings-panel',
  templateUrl: './map-layer-settings-panel.component.html',
  providers: [],
  styleUrls: ['./map-layer-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapLayerSettingsPanelComponent implements OnInit {

  MapProvider = MapProvider;

  mapProviders = mapProviders;

  mapProviderTranslationMap = mapProviderTranslationMap;

  openStreetLayerTypes = openStreetLayerTypes;

  openStreetMapLayerTranslationMap = openStreetMapLayerTranslationMap;

  googleMapLayerTypes = googleMapLayerTypes;

  googleMapLayerTranslationMap = googleMapLayerTranslationMap;

  hereLayerTypes = hereLayerTypes;

  hereLayerTranslationMap = hereLayerTranslationMap;

  tencentLayerTypes = tencentLayerTypes;

  tencentLayerTranslationMap = tencentLayerTranslationMap;

  referenceLayerTypes = referenceLayerTypes;

  referenceLayerTypeTranslationMap = referenceLayerTypeTranslationMap;

  @Input()
  mapLayerSettings: MapLayerSettings;

  @Input()
  popover: TbPopoverComponent<MapLayerSettingsPanelComponent>;

  @Output()
  mapLayerSettingsApplied = new EventEmitter<MapLayerSettings>();

  layerFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.layerFormGroup = this.fb.group(
      {
        label: [null, []],
        provider: [null, [Validators.required]],
        layerType: [null, [Validators.required]],
        tileUrl: [null, [Validators.required]],
        apiKey: [null, [Validators.required]],
        referenceLayer: [null, []]
      }
    );
    this.layerFormGroup.patchValue(
      this.mapLayerSettings, {emitEvent: false}
    );
    this.layerFormGroup.get('provider').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((newProvider: MapProvider) => {
      this.onProviderChanged(newProvider);
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  labelPlaceholder(): string {
    let translationKey = defaultLayerTitle(this.layerFormGroup.value);
    if (!translationKey) {
      translationKey = 'widget-config.set';
    }
    return this.translate.instant(translationKey);
  }

  applyLayerSettings() {
    const layerSettings: MapLayerSettings = this.layerFormGroup.value;
    this.mapLayerSettingsApplied.emit(layerSettings);
  }

  private onProviderChanged(newProvider: MapProvider) {
    let modelValue: MapLayerSettings = this.layerFormGroup.value;
    modelValue = {...defaultMapLayerSettings(newProvider), label: modelValue.label};
    this.layerFormGroup.patchValue(
      modelValue, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const provider: MapProvider = this.layerFormGroup.get('provider').value;
    if (provider === MapProvider.custom) {
      this.layerFormGroup.get('tileUrl').enable({emitEvent: false});
      this.layerFormGroup.get('layerType').disable({emitEvent: false});
    } else {
      this.layerFormGroup.get('tileUrl').disable({emitEvent: false});
      this.layerFormGroup.get('layerType').enable({emitEvent: false});
    }
    if ([MapProvider.google, MapProvider.here].includes(provider)) {
      this.layerFormGroup.get('apiKey').enable({emitEvent: false});
    } else {
      this.layerFormGroup.get('apiKey').disable({emitEvent: false});
    }
  }
}

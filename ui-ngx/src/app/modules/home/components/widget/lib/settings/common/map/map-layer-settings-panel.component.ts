// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    encapsulation: ViewEncapsulation.None,
    standalone: false
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

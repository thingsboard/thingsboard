///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  DataLayerEditAction,
  defaultImageMapSourceSettings,
  ImageMapSourceSettings,
  imageMapSourceSettingsValidator,
  mapControlPositions,
  mapControlsPositionTranslationMap,
  MapDataLayerSettings,
  MapDataLayerType, mapScales, mapScaleTranslationMap,
  MapSetting,
  MapType,
  mapZoomActions,
  mapZoomActionTranslationMap
} from '@shared/models/widget/maps/map.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { merge, Observable } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { DataKey, DataKeyConfigMode, Widget, widgetType } from '@shared/models/widget.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component';
import { deepClone, mergeDeep } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-map-settings',
  templateUrl: './map-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapSettingsComponent),
      multi: true
    }
  ]
})
export class MapSettingsComponent implements OnInit, ControlValueAccessor, Validator, OnChanges {

  mapControlPositions = mapControlPositions;

  mapControlsPositionTranslationMap = mapControlsPositionTranslationMap;

  mapZoomActions = mapZoomActions;

  mapZoomActionTranslationMap = mapZoomActionTranslationMap;

  mapScales = mapScales;

  mapScaleTranslationMap = mapScaleTranslationMap;

  MapType = MapType;

  @Input()
  @coerceBoolean()
  trip = false;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  functionsOnly = false;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: WidgetConfigCallbacks;

  @Input()
  widget: Widget;

  context: MapSettingsContext;

  private modelValue: MapSetting;

  private propagateChange = null;

  public mapSettingsFormGroup: UntypedFormGroup;

  dataLayerMode: MapDataLayerType = 'markers';

  showDragButtonModeButtonSettings = false;

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    if (this.trip) {
      this.dataLayerMode = 'trips';
    }

    this.context = {
      functionsOnly: this.functionsOnly,
      aliasController: this.aliasController,
      callbacks: this.callbacks,
      widget: this.widget,
      editKey: this.editKey.bind(this),
      generateDataKey: this.generateDataKey.bind(this)
    };

    this.mapSettingsFormGroup = this.fb.group({
      mapType: [null, []],
      layers: [null, []],
      imageSource: [null, [imageMapSourceSettingsValidator]],
      markers: [null, []],
      polygons: [null, []],
      circles: [null, []],
      polylines: [null, []],
      additionalDataSources: [null, []],
      controlsPosition: [null, []],
      zoomActions: [null, []],
      scales: [null, []],
      dragModeButton: [null, []],
      fitMapBounds: [null, []],
      useDefaultCenterPosition: [null, []],
      defaultCenterPosition: [null, []],
      defaultZoomLevel: [null, [Validators.min(0), Validators.max(20)]],
      mapPageSize: [null, [Validators.min(1), Validators.required]],
      mapActionButtons: [null]
    });
    if (this.trip) {
      this.mapSettingsFormGroup.addControl('trips', this.fb.control(null));
      this.mapSettingsFormGroup.addControl('tripTimeline', this.fb.control(null));
    }
    this.mapSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.mapSettingsFormGroup.get('mapType').valueChanges,
      this.mapSettingsFormGroup.get('useDefaultCenterPosition').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.mapSettingsFormGroup.get('mapType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((mapType: MapType) => {
      this.mapTypeChanged(mapType);
    });
    merge(this.mapSettingsFormGroup.get('markers').valueChanges,
          this.mapSettingsFormGroup.get('polygons').valueChanges,
          this.mapSettingsFormGroup.get('circles').valueChanges,
          this.mapSettingsFormGroup.get('polylines').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateDragButtonModeSettings();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mapSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.mapSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.trip) {
      const tripChange = changes.trip;
      if (!tripChange.firstChange && tripChange.currentValue !== tripChange.previousValue) {
        if (this.trip) {
          this.dataLayerMode = 'trips'
          this.mapSettingsFormGroup.addControl('trips', this.fb.control(this.modelValue.trips), {emitEvent: false});
          this.mapSettingsFormGroup.addControl('tripTimeline', this.fb.control(this.modelValue.tripTimeline), {emitEvent: false});
        } else {
          this.dataLayerMode = 'markers';
          this.mapSettingsFormGroup.removeControl('trips', {emitEvent: false});
          this.mapSettingsFormGroup.removeControl('tripTimeline', {emitEvent: false});
        }
      }
    }
  }

  writeValue(value: MapSetting): void {
    this.modelValue = value;
    this.mapSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.updateDragButtonModeSettings();
  }

  public validate(_c: UntypedFormControl) {
    const valid = this.mapSettingsFormGroup.valid;
    return valid ? null : {
      mapSettings: {
        valid: false,
      },
    };
  }

  private updateValidators() {
    const mapType: MapType = this.mapSettingsFormGroup.get('mapType').value;
    if (mapType === MapType.geoMap) {
      this.mapSettingsFormGroup.get('layers').enable({emitEvent: false});
      this.mapSettingsFormGroup.get('imageSource').disable({emitEvent: false});
      this.mapSettingsFormGroup.get('fitMapBounds').enable({emitEvent: false});
      this.mapSettingsFormGroup.get('useDefaultCenterPosition').enable({emitEvent: false});
      const useDefaultCenterPosition: boolean = this.mapSettingsFormGroup.get('useDefaultCenterPosition').value;
      if (useDefaultCenterPosition) {
        this.mapSettingsFormGroup.get('defaultCenterPosition').enable({emitEvent: false});
      } else {
        this.mapSettingsFormGroup.get('defaultCenterPosition').disable({emitEvent: false});
      }
      this.mapSettingsFormGroup.get('defaultZoomLevel').enable({emitEvent: false});
    } else {
      this.mapSettingsFormGroup.get('layers').disable({emitEvent: false});
      this.mapSettingsFormGroup.get('imageSource').enable({emitEvent: false});
      this.mapSettingsFormGroup.get('fitMapBounds').disable({emitEvent: false});
      this.mapSettingsFormGroup.get('useDefaultCenterPosition').disable({emitEvent: false});
      this.mapSettingsFormGroup.get('defaultCenterPosition').disable({emitEvent: false});
      this.mapSettingsFormGroup.get('defaultZoomLevel').disable({emitEvent: false});
    }
  }

  private mapTypeChanged(mapType: MapType): void {
    if (mapType === MapType.image) {
      let imageSource: ImageMapSourceSettings = this.mapSettingsFormGroup.get('imageSource').value;
      if (!imageSource?.sourceType) {
        imageSource = mergeDeep({} as ImageMapSourceSettings, defaultImageMapSourceSettings);
        this.mapSettingsFormGroup.get('imageSource').patchValue(imageSource);
      }
    }
  }

  private updateDragButtonModeSettings() {
    const markers: MapDataLayerSettings[] = this.mapSettingsFormGroup.get('markers').value;
    let dragModeButtonSettingsEnabled = markers.some(d => d.edit && d.edit.enabledActions && d.edit.enabledActions.includes(DataLayerEditAction.move));
    if (!dragModeButtonSettingsEnabled) {
      const polygons: MapDataLayerSettings[] = this.mapSettingsFormGroup.get('polygons').value;
      dragModeButtonSettingsEnabled = polygons.some(d => d.edit && d.edit.enabledActions && d.edit.enabledActions.includes(DataLayerEditAction.move));
    }
    if (!dragModeButtonSettingsEnabled) {
      const polylines: MapDataLayerSettings[] = this.mapSettingsFormGroup.get('polylines').value;
      dragModeButtonSettingsEnabled = polylines.some(d => d.edit && d.edit.enabledActions && d.edit.enabledActions.includes(DataLayerEditAction.move));
    }
    if (!dragModeButtonSettingsEnabled) {
      const circles: MapDataLayerSettings[] = this.mapSettingsFormGroup.get('circles').value;
      dragModeButtonSettingsEnabled = circles.some(d => d.edit && d.edit.enabledActions && d.edit.enabledActions.includes(DataLayerEditAction.move));
    }
    this.showDragButtonModeButtonSettings = dragModeButtonSettingsEnabled;
    if (dragModeButtonSettingsEnabled) {
      this.mapSettingsFormGroup.get('dragModeButton').enable({emitEvent: false});
    } else {
      this.mapSettingsFormGroup.get('dragModeButton').disable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.mapSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private editKey(key: DataKey, deviceId: string, entityAliasId: string, _widgetType = widgetType.latest): Observable<DataKey> {
    return this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(key),
          dataKeyConfigMode: DataKeyConfigMode.general,
          aliasController: this.aliasController,
          widgetType: _widgetType,
          deviceId,
          entityAliasId,
          showPostProcessing: true,
          callbacks: this.callbacks,
          hideDataKeyColor: true,
          hideDataKeyDecimals: true,
          hideDataKeyUnits: true,
          widget: this.widget,
          dashboard: null,
          dataKeySettingsForm: null,
          dataKeySettingsDirective: null
        }
      }).afterClosed();
  }

  private generateDataKey(key: DataKey): DataKey {
    return this.callbacks.generateDataKey(key.name, key.type, null, false, null);
  }
}

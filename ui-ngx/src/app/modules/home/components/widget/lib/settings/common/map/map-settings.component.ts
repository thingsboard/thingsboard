///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  defaultImageMapSourceSettings,
  ImageMapSourceSettings, imageMapSourceSettingsValidator,
  mapControlPositions,
  mapControlsPositionTranslationMap,
  MapDataLayerType,
  MapSetting,
  MapType,
  mapZoomActions,
  mapZoomActionTranslationMap
} from '@home/components/widget/lib/maps/models/map.models';
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
export class MapSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  mapControlPositions = mapControlPositions;

  mapZoomActions = mapZoomActions;

  mapControlsPositionTranslationMap = mapControlsPositionTranslationMap;

  mapZoomActionTranslationMap = mapZoomActionTranslationMap;

  MapType = MapType;

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

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

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
      additionalDataSources: [null, []],
      controlsPosition: [null, []],
      zoomActions: [null, []],
      fitMapBounds: [null, []],
      useDefaultCenterPosition: [null, []],
      defaultCenterPosition: [null, []],
      defaultZoomLevel: [null, [Validators.min(0), Validators.max(20)]],
      mapPageSize: [null, [Validators.min(1), Validators.required]],
      mapActionButtons: [null]
    });
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

  writeValue(value: MapSetting): void {
    this.modelValue = value;
    this.mapSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
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

  private updateModel() {
    this.modelValue = this.mapSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private editKey(key: DataKey, deviceId: string, entityAliasId: string): Observable<DataKey> {
    return this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(key),
          dataKeyConfigMode: DataKeyConfigMode.general,
          aliasController: this.aliasController,
          widgetType: widgetType.latest,
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

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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CirclesDataLayerSettings, defaultBaseMapDataLayerSettings,
  MapDataLayerSettings,
  MapDataLayerType,
  MapType,
  MarkersDataLayerSettings,
  MarkerType,
  PolygonsDataLayerSettings, ShapeDataLayerSettings
} from '@home/components/widget/lib/maps/map.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DataKey, DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityType } from '@shared/models/entity-type.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { genNextLabelForDataKeys, mergeDeepIgnoreArray } from '@core/utils';

export interface MapDataLayerDialogData {
  settings: MapDataLayerSettings;
  mapType: MapType;
  dataLayerType: MapDataLayerType;
  context: MapSettingsContext;
}

@Component({
  selector: 'tb-map-data-layer-dialog',
  templateUrl: './map-data-layer-dialog.component.html',
  styleUrls: ['./map-data-layer-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapDataLayerDialogComponent extends DialogComponent<MapDataLayerDialogComponent, MapDataLayerSettings> {

  DatasourceType = DatasourceType;

  EntityType = EntityType;

  MapType = MapType;

  DataKeyType = DataKeyType;

  widgetType = widgetType;

  MarkerType = MarkerType;

  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  dataLayerFormGroup: UntypedFormGroup;

  settings = this.data.settings;
  mapType = this.data.mapType;
  dataLayerType = this.data.dataLayerType;
  context = this.data.context;

  generateAdditionalDataKey = this.generateDataKey.bind(this);

  dialogTitle: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MapDataLayerDialogData,
              public dialogRef: MatDialogRef<MapDataLayerDialogComponent, MapDataLayerSettings>,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);

    if (this.context.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
    }

    this.settings = mergeDeepIgnoreArray({} as MapDataLayerSettings,
      defaultBaseMapDataLayerSettings<MapDataLayerSettings>(this.dataLayerType), this.settings);

    this.dataLayerFormGroup = this.fb.group({
      dsType: [this.settings.dsType, [Validators.required]],
      dsLabel: [this.settings.dsLabel, []],
      dsDeviceId: [this.settings.dsDeviceId, [Validators.required]],
      dsEntityAliasId: [this.settings.dsEntityAliasId, [Validators.required]],
      dsFilterId: [this.settings.dsFilterId, []],
      additionalDataKeys: [this.settings.additionalDataKeys, []],
      label: [this.settings.label, []],
      tooltip: [this.settings.tooltip, []],
      groups: [this.settings.groups, []]
    });

    switch (this.dataLayerType) {
      case 'markers':
        const markersDataLayer = this.settings as MarkersDataLayerSettings;
        this.dialogTitle = 'widgets.maps.data-layer.marker.marker-configuration';
        this.dataLayerFormGroup.addControl('xKey', this.fb.control(markersDataLayer.xKey, Validators.required));
        this.dataLayerFormGroup.addControl('yKey', this.fb.control(markersDataLayer.yKey, Validators.required))
        this.dataLayerFormGroup.addControl('markerType', this.fb.control(markersDataLayer.markerType, Validators.required));
        this.dataLayerFormGroup.addControl('markerColor', this.fb.control(markersDataLayer.markerColor, Validators.required));
        this.dataLayerFormGroup.addControl('markerImage', this.fb.control(markersDataLayer.markerImage, Validators.required));
        this.dataLayerFormGroup.addControl('markerOffsetX', this.fb.control(markersDataLayer.markerOffsetX));
        this.dataLayerFormGroup.addControl('markerOffsetY', this.fb.control(markersDataLayer.markerOffsetY));
        this.dataLayerFormGroup.get('markerType').valueChanges.pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe(() =>
          this.updateValidators()
        );
        break;
      case 'polygons':
      case 'circles':
        const shapeDataLayer = this.settings as ShapeDataLayerSettings;
        this.dataLayerFormGroup.addControl('fillColor', this.fb.control(shapeDataLayer.fillColor, Validators.required));
        this.dataLayerFormGroup.addControl('strokeColor', this.fb.control(shapeDataLayer.strokeColor, Validators.required));
        this.dataLayerFormGroup.addControl('strokeWeight', this.fb.control(shapeDataLayer.strokeWeight, [Validators.required, Validators.min(0)]));
        if (this.dataLayerType === 'polygons') {
          const polygonsDataLayer = this.settings as PolygonsDataLayerSettings;
          this.dialogTitle = 'widgets.maps.data-layer.polygon.polygon-configuration';
          this.dataLayerFormGroup.addControl('polygonKey', this.fb.control(polygonsDataLayer.polygonKey, Validators.required));
        } else {
          const circlesDataLayer = this.settings as CirclesDataLayerSettings;
          this.dialogTitle = 'widgets.maps.data-layer.circle.circle-configuration';
          this.dataLayerFormGroup.addControl('circleKey', this.fb.control(circlesDataLayer.circleKey, Validators.required));
        }
        break;
    }
    this.dataLayerFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (newDsType: DatasourceType) => this.onDsTypeChanged(newDsType)
    );
    this.updateValidators();
  }

  private onDsTypeChanged(newDsType: DatasourceType) {
    switch (this.dataLayerType) {
      case 'markers':
        const xKey: DataKey = this.dataLayerFormGroup.get('xKey').value;
        if (this.updateDataKeyToNewDsType(xKey, newDsType)) {
          this.dataLayerFormGroup.get('xKey').patchValue(xKey, {emitEvent: false});
        }
        const yKey: DataKey = this.dataLayerFormGroup.get('yKey').value;
        if (this.updateDataKeyToNewDsType(yKey, newDsType)) {
          this.dataLayerFormGroup.get('yKey').patchValue(yKey, {emitEvent: false});
        }
        break;
      case 'polygons':
        const polygonKey: DataKey = this.dataLayerFormGroup.get('polygonKey').value;
        if (this.updateDataKeyToNewDsType(polygonKey, newDsType)) {
          this.dataLayerFormGroup.get('polygonKey').patchValue(polygonKey, {emitEvent: false});
        }
        break;
      case 'circles':
        const circleKey: DataKey = this.dataLayerFormGroup.get('circleKey').value;
        if (this.updateDataKeyToNewDsType(circleKey, newDsType)) {
          this.dataLayerFormGroup.get('circleKey').patchValue(circleKey, {emitEvent: false});
        }
        break;
    }
    this.updateValidators();
  }

  private updateDataKeyToNewDsType(dataKey: DataKey, newDsType: DatasourceType): boolean {
    if (newDsType === DatasourceType.function) {
      if (dataKey.type !== DataKeyType.function) {
        dataKey.type = DataKeyType.function;
        return true;
      }
    } else {
      if (dataKey.type === DataKeyType.function) {
        dataKey.type = DataKeyType.attribute;
        return true;
      }
    }
    return false;
  }

  private updateValidators() {
    const dsType: DatasourceType = this.dataLayerFormGroup.get('dsType').value;
    if (dsType === DatasourceType.function) {
      this.dataLayerFormGroup.get('dsLabel').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else if (dsType === DatasourceType.device) {
      this.dataLayerFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
    if (this.dataLayerType === 'markers') {
      const markerType: MarkerType = this.dataLayerFormGroup.get('markerType').value;
      if (markerType === MarkerType.default) {
        this.dataLayerFormGroup.get('markerColor').enable({emitEvent: false});
        this.dataLayerFormGroup.get('markerImage').disable({emitEvent: false});
      } else {
        this.dataLayerFormGroup.get('markerColor').disable({emitEvent: false});
        this.dataLayerFormGroup.get('markerImage').enable({emitEvent: false});
      }
    }
  }

  editKey(keyType: 'xKey' | 'yKey' | 'polygonKey' | 'circleKey') {
    const targetDataKey: DataKey = this.dataLayerFormGroup.get(keyType).value;
    this.context.editKey(targetDataKey,
      this.dataLayerFormGroup.get('dsDeviceId').value, this.dataLayerFormGroup.get('dsEntityAliasId').value).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.dataLayerFormGroup.get(keyType).patchValue(updatedDataKey);
        }
      }
    );
  }

  private generateDataKey(key: DataKey): DataKey {
    const dataKey = this.context.callbacks.generateDataKey(key.name, key.type, null, false, null);
    const dataKeys: DataKey[] = [];
    switch (this.dataLayerType) {
      case 'markers':
        const xKey: DataKey = this.dataLayerFormGroup.get('xKey').value;
        if (xKey) {
          dataKeys.push(xKey);
        }
        const yKey: DataKey = this.dataLayerFormGroup.get('yKey').value;
        if (yKey) {
          dataKeys.push(yKey);
        }
        break;
      case 'polygons':
        const polygonKey: DataKey = this.dataLayerFormGroup.get('polygonKey').value;
        if (polygonKey) {
          dataKeys.push(polygonKey);
        }
        break;
      case 'circles':
        const circleKey: DataKey = this.dataLayerFormGroup.get('circleKey').value;
        if (circleKey) {
          dataKeys.push(circleKey);
        }
        break;
    }
    const additionalKeys: DataKey[] = this.dataLayerFormGroup.get('additionalDataKeys').value;
    if (additionalKeys) {
      dataKeys.push(...additionalKeys);
    }
    dataKey.label = genNextLabelForDataKeys(dataKey.label, dataKeys);
    return dataKey;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const settings: MapDataLayerSettings = this.dataLayerFormGroup.getRawValue();
    this.dialogRef.close(settings);
  }
}

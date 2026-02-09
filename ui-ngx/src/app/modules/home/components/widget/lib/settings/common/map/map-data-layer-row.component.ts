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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatIconButton } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  CirclesDataLayerSettings,
  MapDataLayerSettings,
  MapDataLayerType,
  MapType,
  MarkersDataLayerSettings,
  PolygonsDataLayerSettings, PolylinesDataLayerSettings,
  TripsDataLayerSettings,
  updateDataKeyToNewDsType
} from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { MapSettingsComponent } from '@home/components/widget/lib/settings/common/map/map-settings.component';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  MapDataLayerDialogComponent,
  MapDataLayerDialogData
} from '@home/components/widget/lib/settings/common/map/map-data-layer-dialog.component';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
    selector: 'tb-map-data-layer-row',
    templateUrl: './map-data-layer-row.component.html',
    styleUrls: ['./map-data-layer-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapDataLayerRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MapDataLayerRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;
  DataKeyType = DataKeyType;

  EntityType = EntityType;

  MapType = MapType;

  widgetType = widgetType;

  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  @Input()
  disabled: boolean;

  @Input()
  mapType: MapType = MapType.geoMap;

  @Input()
  dataLayerType: MapDataLayerType = 'markers';

  @Input()
  context: MapSettingsContext;

  @Output()
  dataLayerRemoved = new EventEmitter();

  dataLayerFormGroup: UntypedFormGroup;

  modelValue: MapDataLayerSettings;

  editDataLayerText: string;

  removeDataLayerText: string;

  private propagateChange = (_val: any) => {};

  constructor(private mapSettingsComponent: MapSettingsComponent,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    if (this.context.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
    }
    this.dataLayerFormGroup = this.fb.group({
      dsType: [null, [Validators.required]],
      dsLabel: [null, []],
      dsDeviceId: [null, [Validators.required]],
      dsEntityAliasId: [null, [Validators.required]]
    });
    switch (this.dataLayerType) {
      case 'trips':
        this.editDataLayerText = 'widgets.maps.data-layer.trip.trip-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.trip.remove-trip';
        this.dataLayerFormGroup.addControl('xKey', this.fb.control(null, Validators.required));
        this.dataLayerFormGroup.addControl('yKey', this.fb.control(null, Validators.required));
        break;
      case 'markers':
        this.editDataLayerText = 'widgets.maps.data-layer.marker.marker-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.marker.remove-marker';
        this.dataLayerFormGroup.addControl('xKey', this.fb.control(null, Validators.required));
        this.dataLayerFormGroup.addControl('yKey', this.fb.control(null, Validators.required));
        break;
      case 'polygons':
        this.editDataLayerText = 'widgets.maps.data-layer.polygon.polygon-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.polygon.remove-polygon';
        this.dataLayerFormGroup.addControl('polygonKey', this.fb.control(null, Validators.required));
        break;
      case 'circles':
        this.editDataLayerText = 'widgets.maps.data-layer.circle.circle-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.circle.remove-circle';
        this.dataLayerFormGroup.addControl('circleKey', this.fb.control(null, Validators.required));
        break;
      case 'polylines':
        this.editDataLayerText = 'widgets.maps.data-layer.polyline.polyline-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.polyline.remove-polyline';
        this.dataLayerFormGroup.addControl('polylineKey', this.fb.control(null, Validators.required));
        break;
    }
    this.dataLayerFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.dataLayerFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (newDsType: DatasourceType) => this.onDsTypeChanged(newDsType)
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataLayerFormGroup.disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: MapDataLayerSettings): void {
    this.modelValue = value;
    this.dataLayerFormGroup.patchValue(
      {
        dsType: value?.dsType,
        dsLabel: value?.dsLabel,
        dsDeviceId: value?.dsDeviceId,
        dsEntityAliasId: value?.dsEntityAliasId
      }, {emitEvent: false}
    );
    switch (this.dataLayerType) {
      case 'trips':
        const tripsDataLayer = value as TripsDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            xKey: tripsDataLayer?.xKey,
            yKey: tripsDataLayer?.yKey
          }, {emitEvent: false}
        );
        break;
      case 'markers':
        const markersDataLayer = value as MarkersDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            xKey: markersDataLayer?.xKey,
            yKey: markersDataLayer?.yKey
          }, {emitEvent: false}
        );
        break;
      case 'polygons':
        const polygonsDataLayer = value as PolygonsDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            polygonKey: polygonsDataLayer?.polygonKey
          }, {emitEvent: false}
        );
        break;
      case 'circles':
        const circlesDataLayer = value as CirclesDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            circleKey: circlesDataLayer?.circleKey
          }, {emitEvent: false}
        );
        break;
      case 'polylines':
        const polylinesDataLayer = value as PolylinesDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            polylineKey: polylinesDataLayer?.polylineKey
          }, {emitEvent: false}
        );
        break;
    }
    this.updateValidators();
    this.cd.markForCheck();
  }

  editKey(keyType: 'xKey' | 'yKey' | 'polygonKey' | 'circleKey' | 'polylineKey') {
    const targetDataKey: DataKey = this.dataLayerFormGroup.get(keyType).value;
    this.context.editKey(targetDataKey,
      this.dataLayerFormGroup.get('dsDeviceId').value, this.dataLayerFormGroup.get('dsEntityAliasId').value,
      this.dataLayerType === 'trips' ? widgetType.timeseries : widgetType.latest, false).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.dataLayerFormGroup.get(keyType).patchValue(updatedDataKey);
        }
      }
    );
  }

  editDataLayer($event: Event, matButton: MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<MapDataLayerDialogComponent, MapDataLayerDialogData,
      MapDataLayerSettings>(MapDataLayerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        settings: deepClone(this.modelValue),
        mapType: this.mapType,
        dataLayerType: this.dataLayerType,
        context: this.context
      }
    }).afterClosed().subscribe((settings) => {
      if (settings) {
        this.modelValue = settings;
        this.dataLayerFormGroup.patchValue(settings);
        this.updateValidators();
      }
    });
  }

  private onDsTypeChanged(newDsType: DatasourceType) {
    let updateModel = false;
    switch (this.dataLayerType) {
      case 'trips':
      case 'markers':
        const xKey: DataKey = this.dataLayerFormGroup.get('xKey').value;
        if (updateDataKeyToNewDsType(xKey, newDsType, this.dataLayerType === 'trips')) {
          this.dataLayerFormGroup.get('xKey').patchValue(xKey, {emitEvent: false});
          updateModel = true;
        }
        const yKey: DataKey = this.dataLayerFormGroup.get('yKey').value;
        if (updateDataKeyToNewDsType(yKey, newDsType, this.dataLayerType === 'trips')) {
          this.dataLayerFormGroup.get('yKey').patchValue(yKey, {emitEvent: false});
          updateModel = true;
        }
        break;
      case 'polygons':
        const polygonKey: DataKey = this.dataLayerFormGroup.get('polygonKey').value;
        if (updateDataKeyToNewDsType(polygonKey, newDsType)) {
          this.dataLayerFormGroup.get('polygonKey').patchValue(polygonKey, {emitEvent: false});
          updateModel = true;
        }
        break;
      case 'circles':
        const circleKey: DataKey = this.dataLayerFormGroup.get('circleKey').value;
        if (updateDataKeyToNewDsType(circleKey, newDsType)) {
          this.dataLayerFormGroup.get('circleKey').patchValue(circleKey, {emitEvent: false});
          updateModel = true;
        }
        break;
      case 'polylines':
        const polylineKey: DataKey = this.dataLayerFormGroup.get('polylineKey').value;
        if (updateDataKeyToNewDsType(polylineKey, newDsType)) {
          this.dataLayerFormGroup.get('polylineKey').patchValue(polylineKey, {emitEvent: false});
          updateModel = true;
        }
        break;
    }
    this.updateValidators();
    if (updateModel) {
      this.updateModel();
    }
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
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataLayerFormGroup.value};
    if (this.modelValue.dsType === DatasourceType.function) {
      delete this.modelValue.additionalDataSources;
    }
    this.propagateChange(this.modelValue);
  }
}

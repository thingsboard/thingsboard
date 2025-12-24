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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CirclesDataLayerSettings,
  DataLayerEditAction,
  dataLayerEditActions,
  dataLayerEditActionTranslationMap,
  defaultBaseMapDataLayerSettings,
  MapDataLayerSettings,
  MapDataLayerType,
  MapType,
  MarkersDataLayerSettings,
  MarkerType,
  pathDecoratorSymbols,
  pathDecoratorSymbolTranslationMap,
  PolygonsDataLayerSettings, PolylinesDataLayerSettings,
  ShapeDataLayerSettings, ShapeFillType,
  TripsDataLayerSettings,
  updateDataKeyToNewDsType
} from '@shared/models/widget/maps/map.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DataKey, DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslationsShort } from '@shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityType } from '@shared/models/entity-type.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { genNextLabelForDataKeys, mergeDeepIgnoreArray } from '@core/utils';
import { WidgetService } from '@core/http/widget.service';
import { merge } from 'rxjs';

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

  AttributeScope = AttributeScope;
  telemetryTypeTranslationsShort = telemetryTypeTranslationsShort;

  widgetType = widgetType;

  MarkerType = MarkerType;

  ShapeFillType = ShapeFillType;

  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  dataLayerEditTitle: string;
  dataLayerEditActions: Array<DataLayerEditAction> = [];
  dataLayerEditActionTranslationMap = dataLayerEditActionTranslationMap;

  pathDecoratorSymbols = pathDecoratorSymbols;
  pathDecoratorSymbolTranslationMap = pathDecoratorSymbolTranslationMap;

  dataLayerFormGroup: UntypedFormGroup;

  settings = this.data.settings;
  mapType = this.data.mapType;
  dataLayerType = this.data.dataLayerType;
  context = this.data.context;

  generateAdditionalDataKey = this.generateDataKey.bind(this);

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  dialogTitle: string;

  get labelHelpId(): string {
    switch (this.dataLayerType) {
      case 'trips':
        return 'widget/lib/map/label_fn';
      case 'markers':
        return 'widget/lib/map/label_fn';
      case 'polygons':
        return 'widget/lib/map/polygon_label_fn';
      case 'circles':
        return 'widget/lib/map/circle_label_fn';
      case 'polylines':
        return 'widget/lib/map/polyline_label_fn';
      default:
        return 'widget/lib/map/label_fn';
    }
  }

  get tooltipHelpId(): string {
    switch (this.dataLayerType) {
      case 'trips':
        return 'widget/lib/map/tooltip_fn';
      case 'markers':
        return 'widget/lib/map/tooltip_fn';
      case 'polygons':
        return 'widget/lib/map/polygon_tooltip_fn';
      case 'circles':
        return 'widget/lib/map/circle_tooltip_fn';
      case 'polylines':
        return 'widget/lib/map/polyline_tooltip_fn';
      default:
        return 'widget/lib/map/tooltip_fn';
    }
  }

  get showEditAttributeScope(): boolean {
    if (this.dataLayerType !== 'trips') {
      const editEnabledActions: DataLayerEditAction[] =
        this.dataLayerFormGroup.get('edit').get('enabledActions').value;
      if (editEnabledActions && editEnabledActions.length) {
        switch (this.dataLayerType) {
          case 'markers':
            const xKey: DataKey = this.dataLayerFormGroup.get('xKey').value;
            const yKey: DataKey = this.dataLayerFormGroup.get('yKey').value;
            return (xKey?.type === DataKeyType.attribute || yKey?.type === DataKeyType.attribute);
          case 'polygons':
            const polygonKey: DataKey = this.dataLayerFormGroup.get('polygonKey').value;
            return polygonKey?.type === DataKeyType.attribute;
          case 'circles':
            const circleKey: DataKey = this.dataLayerFormGroup.get('circleKey').value;
            return circleKey?.type === DataKeyType.attribute;
          case 'polylines':
            const polylineKey: DataKey = this.dataLayerFormGroup.get('polylineKey').value;
            return polylineKey?.type === DataKeyType.attribute;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MapDataLayerDialogData,
              public dialogRef: MatDialogRef<MapDataLayerDialogComponent, MapDataLayerSettings>,
              private fb: FormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);

    if (this.context.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
    }

    this.settings = mergeDeepIgnoreArray({} as MapDataLayerSettings,
      defaultBaseMapDataLayerSettings<MapDataLayerSettings>(this.mapType, this.dataLayerType), this.settings);

    this.dataLayerFormGroup = this.fb.group({
      dsType: [this.settings.dsType, [Validators.required]],
      dsLabel: [this.settings.dsLabel, []],
      dsDeviceId: [this.settings.dsDeviceId, [Validators.required]],
      dsEntityAliasId: [this.settings.dsEntityAliasId, [Validators.required]],
      dsFilterId: [this.settings.dsFilterId, []],
      additionalDataSources: [this.settings.additionalDataSources, []],
      additionalDataKeys: [this.settings.additionalDataKeys, []],
      label: [this.settings.label, []],
      tooltip: [this.settings.tooltip, []],
      click: [this.settings.click, []],
      groups: [this.settings.groups, []]
    });

    if (this.dataLayerType !== 'trips') {
      this.dataLayerFormGroup.addControl('edit', this.fb.group({
        enabledActions: [this.settings.edit?.enabledActions, []],
        attributeScope: [this.settings.edit?.attributeScope, []],
        snappable: [this.settings.edit?.snappable, []]
      }));
    }

    switch (this.dataLayerType) {
      case 'trips':
        this.dialogTitle = 'widgets.maps.data-layer.trip.trip-configuration';
        const tripsDataLayer = this.settings as TripsDataLayerSettings;
        this.dataLayerFormGroup.addControl('xKey', this.fb.control(tripsDataLayer.xKey, Validators.required));
        this.dataLayerFormGroup.addControl('yKey', this.fb.control(tripsDataLayer.yKey, Validators.required));
        this.dataLayerFormGroup.addControl('showMarker', this.fb.control(tripsDataLayer.showMarker));
        this.dataLayerFormGroup.addControl('markerType', this.fb.control(tripsDataLayer.markerType, Validators.required));
        this.dataLayerFormGroup.addControl('markerShape', this.fb.control(tripsDataLayer.markerShape, Validators.required));
        this.dataLayerFormGroup.addControl('markerIcon', this.fb.control(tripsDataLayer.markerIcon, Validators.required));
        this.dataLayerFormGroup.addControl('markerImage', this.fb.control(tripsDataLayer.markerImage, Validators.required));
        this.dataLayerFormGroup.addControl('markerOffsetX', this.fb.control(tripsDataLayer.markerOffsetX));
        this.dataLayerFormGroup.addControl('markerOffsetY', this.fb.control(tripsDataLayer.markerOffsetY));
        this.dataLayerFormGroup.addControl('rotateMarker', this.fb.control(tripsDataLayer.rotateMarker));
        this.dataLayerFormGroup.addControl('offsetAngle', this.fb.control(tripsDataLayer.offsetAngle, [Validators.min(0), Validators.max(360)]));
        if (this.mapType === MapType.image) {
          this.dataLayerFormGroup.addControl('positionFunction', this.fb.control(tripsDataLayer.positionFunction));
        }
        this.dataLayerFormGroup.addControl('showPath', this.fb.control(tripsDataLayer.showPath));
        this.dataLayerFormGroup.addControl('pathStrokeWeight', this.fb.control(tripsDataLayer.pathStrokeWeight, [Validators.required, Validators.min(0)]));
        this.dataLayerFormGroup.addControl('pathStrokeColor', this.fb.control(tripsDataLayer.pathStrokeColor, Validators.required));
        this.dataLayerFormGroup.addControl('usePathDecorator', this.fb.control(tripsDataLayer.usePathDecorator));
        this.dataLayerFormGroup.addControl('pathDecoratorSymbol', this.fb.control(tripsDataLayer.pathDecoratorSymbol, Validators.required));
        this.dataLayerFormGroup.addControl('pathDecoratorSymbolSize', this.fb.control(tripsDataLayer.pathDecoratorSymbolSize, [Validators.required, Validators.min(0)]));
        this.dataLayerFormGroup.addControl('pathDecoratorSymbolColor', this.fb.control(tripsDataLayer.pathDecoratorSymbolColor));
        this.dataLayerFormGroup.addControl('pathDecoratorOffset', this.fb.control(tripsDataLayer.pathDecoratorOffset, [Validators.required, Validators.min(0)]));
        this.dataLayerFormGroup.addControl('pathEndDecoratorOffset', this.fb.control(tripsDataLayer.pathEndDecoratorOffset, [Validators.required, Validators.min(0)]));
        this.dataLayerFormGroup.addControl('pathDecoratorRepeat', this.fb.control(tripsDataLayer.pathDecoratorRepeat, [Validators.required, Validators.min(0)]));
        this.dataLayerFormGroup.addControl('showPoints', this.fb.control(tripsDataLayer.showPoints));
        this.dataLayerFormGroup.addControl('pointSize', this.fb.control(tripsDataLayer.pointSize, [Validators.required, Validators.min(0)]));
        this.dataLayerFormGroup.addControl('pointColor', this.fb.control(tripsDataLayer.pointColor, Validators.required));
        this.dataLayerFormGroup.addControl('pointTooltip', this.fb.control(tripsDataLayer.pointTooltip));
        merge(this.dataLayerFormGroup.get('showMarker').valueChanges,
          this.dataLayerFormGroup.get('markerType').valueChanges,
          this.dataLayerFormGroup.get('rotateMarker').valueChanges,
          this.dataLayerFormGroup.get('showPath').valueChanges,
          this.dataLayerFormGroup.get('usePathDecorator').valueChanges,
          this.dataLayerFormGroup.get('showPoints').valueChanges).pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe(() =>
          this.updateValidators()
        );
        break;
      case 'markers':
        this.dialogTitle = 'widgets.maps.data-layer.marker.marker-configuration';
        this.dataLayerEditTitle = 'widgets.maps.data-layer.marker.edit';
        this.dataLayerEditActions = [DataLayerEditAction.add, DataLayerEditAction.move, DataLayerEditAction.remove];
        const markersDataLayer = this.settings as MarkersDataLayerSettings;
        this.dataLayerFormGroup.addControl('xKey', this.fb.control(markersDataLayer.xKey, Validators.required));
        this.dataLayerFormGroup.addControl('yKey', this.fb.control(markersDataLayer.yKey, Validators.required))
        this.dataLayerFormGroup.addControl('markerType', this.fb.control(markersDataLayer.markerType, Validators.required));
        this.dataLayerFormGroup.addControl('markerShape', this.fb.control(markersDataLayer.markerShape, Validators.required));
        this.dataLayerFormGroup.addControl('markerIcon', this.fb.control(markersDataLayer.markerIcon, Validators.required));
        this.dataLayerFormGroup.addControl('markerImage', this.fb.control(markersDataLayer.markerImage, Validators.required));
        this.dataLayerFormGroup.addControl('markerOffsetX', this.fb.control(markersDataLayer.markerOffsetX));
        this.dataLayerFormGroup.addControl('markerOffsetY', this.fb.control(markersDataLayer.markerOffsetY));
        if (this.mapType === MapType.image) {
          this.dataLayerFormGroup.addControl('positionFunction', this.fb.control(markersDataLayer.positionFunction));
        }
        this.dataLayerFormGroup.addControl('markerClustering', this.fb.control(markersDataLayer.markerClustering));
        this.dataLayerFormGroup.get('markerType').valueChanges.pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe(() =>
          this.updateValidators()
        );
        break;
      case 'polygons':
      case 'circles':
        this.dataLayerEditActions = dataLayerEditActions;
        const shapeDataLayer = this.settings as ShapeDataLayerSettings;
        this.dataLayerFormGroup.addControl('fillType', this.fb.control(shapeDataLayer.fillType, Validators.required));
        this.dataLayerFormGroup.addControl('fillColor', this.fb.control(shapeDataLayer.fillColor, Validators.required));
        this.dataLayerFormGroup.addControl('fillStripe', this.fb.control(shapeDataLayer.fillStripe, Validators.required));
        this.dataLayerFormGroup.addControl('fillImage', this.fb.control(shapeDataLayer.fillImage, Validators.required));
        this.dataLayerFormGroup.addControl('strokeColor', this.fb.control(shapeDataLayer.strokeColor, Validators.required));
        this.dataLayerFormGroup.addControl('strokeWeight', this.fb.control(shapeDataLayer.strokeWeight, [Validators.required, Validators.min(0)]));
        if (this.dataLayerType === 'polygons') {
          this.dialogTitle = 'widgets.maps.data-layer.polygon.polygon-configuration';
          this.dataLayerEditTitle = 'widgets.maps.data-layer.polygon.edit';
          const polygonsDataLayer = this.settings as PolygonsDataLayerSettings;
          this.dataLayerFormGroup.addControl('polygonKey', this.fb.control(polygonsDataLayer.polygonKey, Validators.required));
        } else {
          this.dialogTitle = 'widgets.maps.data-layer.circle.circle-configuration';
          this.dataLayerEditTitle = 'widgets.maps.data-layer.circle.edit';
          const circlesDataLayer = this.settings as CirclesDataLayerSettings;
          this.dataLayerFormGroup.addControl('circleKey', this.fb.control(circlesDataLayer.circleKey, Validators.required));
        }
        this.dataLayerFormGroup.get('fillType').valueChanges.pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe(() =>
          this.updateValidators()
        );
        break;
      case 'polylines':
        this.dataLayerEditActions = dataLayerEditActions;
        const polylineShapeDataLayer = this.settings as PolylinesDataLayerSettings;
        this.dataLayerFormGroup.addControl('strokeColor', this.fb.control(polylineShapeDataLayer.strokeColor, Validators.required));
        this.dataLayerFormGroup.addControl('strokeWeight', this.fb.control(polylineShapeDataLayer.strokeWeight, [Validators.required, Validators.min(0)]));
        this.dialogTitle = 'widgets.maps.data-layer.polyline.polyline-configuration';
        this.dataLayerEditTitle = 'widgets.maps.data-layer.polyline.edit';
        this.dataLayerFormGroup.addControl('polylineKey', this.fb.control(polylineShapeDataLayer.polylineKey, Validators.required));
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
      case 'trips':
      case 'markers':
        const xKey: DataKey = this.dataLayerFormGroup.get('xKey').value;
        if (updateDataKeyToNewDsType(xKey, newDsType, this.dataLayerType === 'trips')) {
          this.dataLayerFormGroup.get('xKey').patchValue(xKey, {emitEvent: false});
        }
        const yKey: DataKey = this.dataLayerFormGroup.get('yKey').value;
        if (updateDataKeyToNewDsType(yKey, newDsType, this.dataLayerType === 'trips')) {
          this.dataLayerFormGroup.get('yKey').patchValue(yKey, {emitEvent: false});
        }
        break;
      case 'polygons':
        const polygonKey: DataKey = this.dataLayerFormGroup.get('polygonKey').value;
        if (updateDataKeyToNewDsType(polygonKey, newDsType)) {
          this.dataLayerFormGroup.get('polygonKey').patchValue(polygonKey, {emitEvent: false});
        }
        break;
      case 'circles':
        const circleKey: DataKey = this.dataLayerFormGroup.get('circleKey').value;
        if (updateDataKeyToNewDsType(circleKey, newDsType)) {
          this.dataLayerFormGroup.get('circleKey').patchValue(circleKey, {emitEvent: false});
        }
        break;
      case 'polylines':
        const polylineKey: DataKey = this.dataLayerFormGroup.get('polylineKey').value;
        if (updateDataKeyToNewDsType(polylineKey, newDsType)) {
          this.dataLayerFormGroup.get('polylineKey').patchValue(polylineKey, {emitEvent: false});
        }
        break;
    }
    const additionalDataKeys: DataKey[] = this.dataLayerFormGroup.get('additionalDataKeys').value;
    if (additionalDataKeys?.length) {
      let updated = false;
      for (const key of additionalDataKeys) {
        updated = updateDataKeyToNewDsType(key, newDsType) || updated;
      }
      if (updated) {
        this.dataLayerFormGroup.get('additionalDataKeys').patchValue(additionalDataKeys, {emitEvent: false});
      }
    }
    this.updateValidators();
  }

  private updateValidators() {
    const dsType: DatasourceType = this.dataLayerFormGroup.get('dsType').value;
    if (dsType === DatasourceType.function) {
      this.dataLayerFormGroup.get('additionalDataSources').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsLabel').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else if (dsType === DatasourceType.device) {
      this.dataLayerFormGroup.get('additionalDataSources').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.get('additionalDataSources').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
    if (this.dataLayerType === 'markers') {
      this.updateMarkerTypeValidators();
    } else if (['polygons', 'circles'].includes(this.dataLayerType)) {
      this.updateFillTypeValidators();
    } else if (this.dataLayerType === 'trips') {
      const showMarker: boolean = this.dataLayerFormGroup.get('showMarker').value;
      if (showMarker) {
        this.dataLayerFormGroup.get('markerType').enable({emitEvent: false});
        this.updateMarkerTypeValidators();
        this.dataLayerFormGroup.get('markerOffsetX').enable({emitEvent: false});
        this.dataLayerFormGroup.get('markerOffsetY').enable({emitEvent: false});
        this.dataLayerFormGroup.get('rotateMarker').enable({emitEvent: false});
        const rotateMarker: boolean = this.dataLayerFormGroup.get('rotateMarker').value;
        if (rotateMarker) {
          this.dataLayerFormGroup.get('offsetAngle').enable({emitEvent: false});
        } else {
          this.dataLayerFormGroup.get('offsetAngle').disable({emitEvent: false});
        }
        this.dataLayerFormGroup.get('label').enable({emitEvent: false});
        this.dataLayerFormGroup.get('tooltip').enable({emitEvent: false});
        this.dataLayerFormGroup.get('click').enable({emitEvent: false});
      } else {
        this.dataLayerFormGroup.get('markerType').disable({emitEvent: false});
        this.dataLayerFormGroup.get('markerShape').disable({emitEvent: false});
        this.dataLayerFormGroup.get('markerIcon').disable({emitEvent: false});
        this.dataLayerFormGroup.get('markerImage').disable({emitEvent: false});
        this.dataLayerFormGroup.get('markerOffsetX').disable({emitEvent: false});
        this.dataLayerFormGroup.get('markerOffsetY').disable({emitEvent: false});
        this.dataLayerFormGroup.get('rotateMarker').disable({emitEvent: false});
        this.dataLayerFormGroup.get('offsetAngle').disable({emitEvent: false});
        this.dataLayerFormGroup.get('label').disable({emitEvent: false});
        this.dataLayerFormGroup.get('tooltip').disable({emitEvent: false});
        this.dataLayerFormGroup.get('click').disable({emitEvent: false});
      }
      const showPath: boolean = this.dataLayerFormGroup.get('showPath').value;
      const usePathDecorator: boolean = this.dataLayerFormGroup.get('usePathDecorator').value;
      const showPoints: boolean = this.dataLayerFormGroup.get('showPoints').value;
      if (showPath) {
        this.dataLayerFormGroup.get('pathStrokeWeight').enable({emitEvent: false});
        this.dataLayerFormGroup.get('pathStrokeColor').enable({emitEvent: false});
        this.dataLayerFormGroup.get('usePathDecorator').enable({emitEvent: false});
        if (usePathDecorator) {
          this.dataLayerFormGroup.get('pathDecoratorSymbol').enable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorSymbolSize').enable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorSymbolColor').enable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorOffset').enable({emitEvent: false});
          this.dataLayerFormGroup.get('pathEndDecoratorOffset').enable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorRepeat').enable({emitEvent: false});
        } else {
          this.dataLayerFormGroup.get('pathDecoratorSymbol').disable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorSymbolSize').disable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorSymbolColor').disable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorOffset').disable({emitEvent: false});
          this.dataLayerFormGroup.get('pathEndDecoratorOffset').disable({emitEvent: false});
          this.dataLayerFormGroup.get('pathDecoratorRepeat').disable({emitEvent: false});
        }
      } else {
        this.dataLayerFormGroup.get('pathStrokeWeight').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathStrokeColor').disable({emitEvent: false});
        this.dataLayerFormGroup.get('usePathDecorator').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathDecoratorSymbol').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathDecoratorSymbolSize').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathDecoratorSymbolColor').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathDecoratorOffset').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathEndDecoratorOffset').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pathDecoratorRepeat').disable({emitEvent: false});
      }
      if (showPoints) {
        this.dataLayerFormGroup.get('pointSize').enable({emitEvent: false});
        this.dataLayerFormGroup.get('pointColor').enable({emitEvent: false});
        this.dataLayerFormGroup.get('pointTooltip').enable({emitEvent: false});
      } else {
        this.dataLayerFormGroup.get('pointSize').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pointColor').disable({emitEvent: false});
        this.dataLayerFormGroup.get('pointTooltip').disable({emitEvent: false});
      }
    }
  }

  private updateMarkerTypeValidators(): void {
    const markerType: MarkerType = this.dataLayerFormGroup.get('markerType').value;
    if (markerType === MarkerType.shape) {
      this.dataLayerFormGroup.get('markerShape').enable({emitEvent: false});
      this.dataLayerFormGroup.get('markerIcon').disable({emitEvent: false});
      this.dataLayerFormGroup.get('markerImage').disable({emitEvent: false});
    } else if (markerType === MarkerType.icon) {
      this.dataLayerFormGroup.get('markerShape').disable({emitEvent: false});
      this.dataLayerFormGroup.get('markerIcon').enable({emitEvent: false});
      this.dataLayerFormGroup.get('markerImage').disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.get('markerShape').disable({emitEvent: false});
      this.dataLayerFormGroup.get('markerIcon').disable({emitEvent: false});
      this.dataLayerFormGroup.get('markerImage').enable({emitEvent: false});
    }
  }

  private updateFillTypeValidators(): void {
    const fillType: ShapeFillType = this.dataLayerFormGroup.get('fillType').value;
    if (fillType === ShapeFillType.color) {
      this.dataLayerFormGroup.get('fillColor').enable({emitEvent: false});
      this.dataLayerFormGroup.get('fillStripe').disable({emitEvent: false});
      this.dataLayerFormGroup.get('fillImage').disable({emitEvent: false});
    } else if (fillType === ShapeFillType.stripe) {
      this.dataLayerFormGroup.get('fillColor').disable({emitEvent: false});
      this.dataLayerFormGroup.get('fillStripe').enable({emitEvent: false});
      this.dataLayerFormGroup.get('fillImage').disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.get('fillColor').disable({emitEvent: false});
      this.dataLayerFormGroup.get('fillStripe').disable({emitEvent: false});
      this.dataLayerFormGroup.get('fillImage').enable({emitEvent: false});
    }
  }

  editKey(keyType: 'xKey' | 'yKey' | 'polygonKey' | 'circleKey' | 'polylineKey') {
    const targetDataKey: DataKey = this.dataLayerFormGroup.get(keyType).value;
    this.context.editKey(targetDataKey,
      this.dataLayerFormGroup.get('dsDeviceId').value, this.dataLayerFormGroup.get('dsEntityAliasId').value,
      this.dataLayerType === 'trips' ? widgetType.timeseries : widgetType.latest, false).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.dataLayerFormGroup.get(keyType).patchValue(updatedDataKey);
          this.dataLayerFormGroup.markAsDirty();
        }
      }
    );
  }

  private generateDataKey(key: DataKey): DataKey {
    const dataKey = this.context.callbacks.generateDataKey(key.name, key.type, null, false, null);
    const dataKeys: DataKey[] = [];
    switch (this.dataLayerType) {
      case 'trips':
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
      case 'polylines':
        const polylineKey: DataKey = this.dataLayerFormGroup.get('polylineKey').value;
        if (polylineKey) {
          dataKeys.push(polylineKey);
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

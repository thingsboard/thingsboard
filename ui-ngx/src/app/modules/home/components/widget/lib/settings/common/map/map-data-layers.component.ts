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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { mergeDeep } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  defaultMapDataLayerSettings,
  MapDataLayerSettings,
  MapDataLayerType,
  mapDataLayerValid,
  mapDataLayerValidator,
  MapType
} from '@shared/models/widget/maps/map.models';
import { MapSettingsComponent } from '@home/components/widget/lib/settings/common/map/map-settings.component';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-map-data-layers',
  templateUrl: './map-data-layers.component.html',
  styleUrls: ['./map-data-layers.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapDataLayersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapDataLayersComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapDataLayersComponent implements ControlValueAccessor, OnInit, Validator {

  MapType = MapType;

  @Input()
  disabled: boolean;

  @Input()
  mapType: MapType = MapType.geoMap;

  @Input()
  dataLayerType: MapDataLayerType = 'markers';

  @Input()
  context: MapSettingsContext;

  dataLayersFormGroup: UntypedFormGroup;

  addDataLayerText: string;

  noDataLayersText: string;

  get dragEnabled(): boolean {
    return this.dataLayersFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private mapSettingsComponent: MapSettingsComponent,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    switch (this.dataLayerType) {
      case 'trips':
        this.addDataLayerText = 'widgets.maps.data-layer.trip.add-trip';
        this.noDataLayersText = 'widgets.maps.data-layer.trip.no-trips';
        break;
      case 'markers':
        this.addDataLayerText = 'widgets.maps.data-layer.marker.add-marker';
        this.noDataLayersText = 'widgets.maps.data-layer.marker.no-markers';
        break;
      case 'polygons':
        this.addDataLayerText = 'widgets.maps.data-layer.polygon.add-polygon';
        this.noDataLayersText = 'widgets.maps.data-layer.polygon.no-polygons';
        break;
      case 'circles':
        this.addDataLayerText = 'widgets.maps.data-layer.circle.add-circle';
        this.noDataLayersText = 'widgets.maps.data-layer.circle.no-circles';
        break;
    }
    this.dataLayersFormGroup = this.fb.group({
      dataLayers: [this.fb.array([]), []]
    });
    this.dataLayersFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let layers: MapDataLayerSettings[] = this.dataLayersFormGroup.get('dataLayers').value;
        if (layers) {
          layers = layers.filter(layer => mapDataLayerValid(layer, this.dataLayerType));
        }
        this.propagateChange(layers);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataLayersFormGroup.disable({emitEvent: false});
    } else {
      this.dataLayersFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapDataLayerSettings[] | undefined): void {
    const dataLayers: MapDataLayerSettings[] = value || [];
    this.dataLayersFormGroup.setControl('dataLayers', this.prepareDataLayersFormArray(dataLayers), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.dataLayersFormGroup.valid;
    return valid ? null : {
      dataLayers: {
        valid: false,
      },
    };
  }

  dataLayersFormArray(): UntypedFormArray {
    return this.dataLayersFormGroup.get('dataLayers') as UntypedFormArray;
  }

  trackByDataLayer(index: number, dataLayerControl: AbstractControl): any {
    return dataLayerControl;
  }

  removeDataLayer(index: number) {
    (this.dataLayersFormGroup.get('dataLayers') as UntypedFormArray).removeAt(index);
  }
  
  layerDrop(event: CdkDragDrop<string[]>) {
    const layersArray = this.dataLayersFormArray();
    const layer = layersArray.at(event.previousIndex);
    layersArray.removeAt(event.previousIndex);
    layersArray.insert(event.currentIndex, layer);
  }

  addDataLayer() {
    const dataLayer = mergeDeep<MapDataLayerSettings>({} as MapDataLayerSettings,
      defaultMapDataLayerSettings(this.mapType, this.dataLayerType, this.context.functionsOnly));
    const dataLayersArray = this.dataLayersFormGroup.get('dataLayers') as UntypedFormArray;
    const dataLayerControl = this.fb.control(dataLayer, [mapDataLayerValidator(this.dataLayerType)]);
    dataLayersArray.push(dataLayerControl);
  }

  private prepareDataLayersFormArray(dataLayers: MapDataLayerSettings[]): UntypedFormArray {
    const dataLayersControls: Array<AbstractControl> = [];
    dataLayers.forEach((dataLayer) => {
      dataLayersControls.push(this.fb.control(dataLayer, [mapDataLayerValidator(this.dataLayerType)]));
    });
    return this.fb.array(dataLayersControls);
  }
}

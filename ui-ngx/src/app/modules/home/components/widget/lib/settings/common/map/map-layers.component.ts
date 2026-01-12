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
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  defaultMapLayerSettings,
  MapLayerSettings,
  mapLayerValid,
  mapLayerValidator,
  MapProvider
} from '@shared/models/widget/maps/map.models';

@Component({
  selector: 'tb-map-layers',
  templateUrl: './map-layers.component.html',
  styleUrls: ['./map-layers.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapLayersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapLayersComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapLayersComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  layersFormGroup: UntypedFormGroup;

  get dragEnabled(): boolean {
    return this.layersFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.layersFormGroup = this.fb.group({
      layers: [this.fb.array([]), []]
    });
    this.layersFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let layers: MapLayerSettings[] = this.layersFormGroup.get('layers').value;
        if (layers) {
          layers = layers.filter(layer => mapLayerValid(layer));
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
      this.layersFormGroup.disable({emitEvent: false});
    } else {
      this.layersFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapLayerSettings[] | undefined): void {
    const layers: MapLayerSettings[] = value || [];
    this.layersFormGroup.setControl('layers', this.prepareLayersFormArray(layers), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.layersFormGroup.valid;
    return valid ? null : {
      layers: {
        valid: false,
      },
    };
  }

  layerDrop(event: CdkDragDrop<string[]>) {
    const layersArray = this.layersFormGroup.get('layers') as UntypedFormArray;
    const layer = layersArray.at(event.previousIndex);
    layersArray.removeAt(event.previousIndex);
    layersArray.insert(event.currentIndex, layer);
  }

  layersFormArray(): UntypedFormArray {
    return this.layersFormGroup.get('layers') as UntypedFormArray;
  }

  trackByLayer(index: number, layerControl: AbstractControl): any {
    return layerControl;
  }

  removeLayer(index: number) {
    (this.layersFormGroup.get('layers') as UntypedFormArray).removeAt(index);
  }

  addLayer() {
    const layer = mergeDeep<MapLayerSettings>({} as MapLayerSettings,
      defaultMapLayerSettings(MapProvider.openstreet));
    const layersArray = this.layersFormGroup.get('layers') as UntypedFormArray;
    const layerControl = this.fb.control(layer, [mapLayerValidator]);
    layersArray.push(layerControl);
  }

  private prepareLayersFormArray(layers: MapLayerSettings[]): UntypedFormArray {
    const layersControls: Array<AbstractControl> = [];
    layers.forEach((layer) => {
      layersControls.push(this.fb.control(layer, [mapLayerValidator]));
    });
    return this.fb.array(layersControls);
  }
}

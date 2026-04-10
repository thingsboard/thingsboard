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

import { Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MapItemTooltips, MapItemType, mapItemTooltipsTranslation } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepTrim, isEqual } from '@core/utils';

@Component({
    selector: 'tb-map-item-tooltips',
    templateUrl: './map-item-tooltips.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapItemTooltipsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MapItemTooltipsComponent implements ControlValueAccessor, OnChanges {

  @Input({required: true})
  mapItemType: MapItemType;

  tooltipsForm: FormGroup;
  MapItemType = MapItemType;
  readonly mapItemTooltipsDefaultTranslate = mapItemTooltipsTranslation;

  private modelValue: MapItemTooltips;
  private propagateChange = (_val: any) => {};

  constructor(private fd: FormBuilder) {
    this.tooltipsForm = this.fd.group({
      placeMarker: [''],
      firstVertex: [''],
      continueLine: [''],
      finishPoly: [''],
      startRect: [''],
      finishRect: [''],
      startCircle: [''],
      finishCircle: ['']
    });

    this.tooltipsForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(this.updatedModel.bind(this));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.mapItemType) {
      const mapItemTypeChanges = changes.mapItemType;
      if (!mapItemTypeChanges.firstChange && mapItemTypeChanges.currentValue !== mapItemTypeChanges.previousValue) {
        this.updatedValidators(true);
      }
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.tooltipsForm.disable({emitEvent: false});
    } else {
      this.tooltipsForm.enable({emitEvent: false});
    }
  }

  writeValue(obj: MapItemTooltips) {
    this.modelValue = obj;
    this.tooltipsForm.patchValue(obj, {emitEvent: false});
    this.updatedValidators();
  }

  private updatedValidators(emitNewValue = false) {
    this.tooltipsForm.disable({emitEvent: false});
    switch (this.mapItemType) {
      case MapItemType.marker:
        this.tooltipsForm.get('placeMarker').enable({emitEvent: false});
        break;
      case MapItemType.rectangle:
        this.tooltipsForm.get('startRect').enable({emitEvent: false});
        this.tooltipsForm.get('finishRect').enable({emitEvent: false});
        break;
      case MapItemType.polygon:
        this.tooltipsForm.get('firstVertex').enable({emitEvent: false});
        this.tooltipsForm.get('continueLine').enable({emitEvent: false});
        this.tooltipsForm.get('finishPoly').enable({emitEvent: false});
        break;
      case MapItemType.circle:
        this.tooltipsForm.get('startCircle').enable({emitEvent: false});
        this.tooltipsForm.get('finishCircle').enable({emitEvent: false});
        break;
    }
    this.tooltipsForm.updateValueAndValidity({emitEvent: emitNewValue})
  }

  private updatedModel(value: MapItemTooltips) {
    const currentValue = Object.fromEntries(Object.entries(deepTrim(value)).filter(([_, v]) => v != ''));
    if (!isEqual(currentValue, this.modelValue)) {
      this.modelValue = currentValue;
      this.propagateChange(currentValue);
    }
  }
}

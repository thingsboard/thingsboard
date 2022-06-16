///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  CircleSettings,
  CommonMapSettings,
  MapEditorSettings,
  MapProviders,
  MapProviderSettings,
  MarkerClusteringSettings,
  MarkersSettings,
  PolygonSettings,
  PolylineSettings,
  UnitedMapSettings
} from '@home/components/widget/lib/maps/map-models';
import { extractType } from '@core/utils';
import { keys } from 'ts-transformer-keys';
import { IAliasController } from '@core/api/widget-api.models';
import { Widget } from '@shared/models/widget.models';

@Component({
  selector: 'tb-map-settings',
  templateUrl: './map-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
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
export class MapSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  widget: Widget;

  @Input()
  routeMap = false;

  mapProvider = MapProviders;

  private modelValue: UnitedMapSettings;

  private propagateChange = null;

  public mapSettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.mapSettingsFormGroup = this.fb.group({
      mapProviderSettings: [null, []],
      commonMapSettings: [null, []],
      markersSettings: [null, []],
      polygonSettings: [null, []],
      circleSettings: [null, []],
    });
    if (this.routeMap) {
      this.mapSettingsFormGroup.addControl('routeMapSettings', this.fb.control(null, []));
    } else {
      this.mapSettingsFormGroup.addControl('markerClusteringSettings', this.fb.control(null, []));
    }
    this.mapSettingsFormGroup.addControl('mapEditorSettings', this.fb.control(null, []));
    this.mapSettingsFormGroup.get('mapProviderSettings').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.mapSettingsFormGroup.get('markersSettings').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.mapSettingsFormGroup.get('polygonSettings').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.mapSettingsFormGroup.get('circleSettings').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.mapSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mapSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.mapSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: UnitedMapSettings): void {
    this.modelValue = value;
    const mapProviderSettings = extractType<MapProviderSettings>(value, keys<MapProviderSettings>());
    const commonMapSettings = extractType<CommonMapSettings>(value, keys<CommonMapSettings>());
    const markersSettings = extractType<MarkersSettings>(value, keys<MarkersSettings>());
    const polygonSettings = extractType<PolygonSettings>(value, keys<PolygonSettings>());
    const circleSettings = extractType<CircleSettings>(value, keys<CircleSettings>());
    const mapEditorSettings = extractType<MapEditorSettings>(value, keys<MapEditorSettings>());
    const formValue = {
      mapProviderSettings,
      commonMapSettings,
      markersSettings,
      polygonSettings,
      circleSettings,
      mapEditorSettings
    } as any;
    if (this.routeMap) {
      formValue.routeMapSettings = extractType<PolylineSettings>(value, ['strokeWeight', 'strokeOpacity']);
    } else {
      formValue.markerClusteringSettings = extractType<MarkerClusteringSettings>(value, keys<MarkerClusteringSettings>());
    }
    this.mapSettingsFormGroup.patchValue( formValue, {emitEvent: false} );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.mapSettingsFormGroup.valid ? null : {
      mapSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.mapSettingsFormGroup.value;
    this.modelValue = {
      ...value.mapProviderSettings,
      ...value.commonMapSettings,
      ...value.markersSettings,
      ...value.polygonSettings,
      ...value.circleSettings,
      ...value.mapEditorSettings
    };
    if (this.routeMap) {
      this.modelValue = {...this.modelValue, ...value.routeMapSettings};
    } else {
      this.modelValue = {...this.modelValue, ...value.markerClusteringSettings};
    }
    this.propagateChange(this.modelValue);
  }

  displayEditorSettings(): boolean {
    const markersSettings: MarkersSettings = this.mapSettingsFormGroup.get('markersSettings').value;
    const polygonSettings: PolygonSettings = this.mapSettingsFormGroup.get('polygonSettings').value;
    const circleSettings: CircleSettings = this.mapSettingsFormGroup.get('circleSettings').value;
    return markersSettings?.draggableMarker || polygonSettings?.editablePolygon || circleSettings?.editableCircle;
  }

  private updateValidators(emitEvent?: boolean): void {
    const displayEditorSettings = this.displayEditorSettings();
    if (displayEditorSettings) {
      this.mapSettingsFormGroup.get('mapEditorSettings').enable({emitEvent});
    } else {
      this.mapSettingsFormGroup.get('mapEditorSettings').disable({emitEvent});
    }
    if (!this.routeMap) {
      const mapProviderSettings: MapProviderSettings = this.mapSettingsFormGroup.get('mapProviderSettings').value;
      if (mapProviderSettings?.provider === MapProviders.image) {
        this.mapSettingsFormGroup.get('markerClusteringSettings').disable({emitEvent});
      } else {
        this.mapSettingsFormGroup.get('markerClusteringSettings').enable({emitEvent});
      }
      this.mapSettingsFormGroup.get('markerClusteringSettings').updateValueAndValidity({emitEvent: false});
    }
    this.mapSettingsFormGroup.get('mapEditorSettings').updateValueAndValidity({emitEvent: false});
  }
}

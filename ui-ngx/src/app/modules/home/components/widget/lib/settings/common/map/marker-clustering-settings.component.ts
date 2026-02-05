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
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarkerClusteringSettings } from '@shared/models/widget/maps/map.models';
import { merge } from 'rxjs';

@Component({
    selector: 'tb-marker-clustering-settings',
    templateUrl: './marker-clustering-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MarkerClusteringSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MarkerClusteringSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MarkerClusteringSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  settingsExpanded = false;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  @Input()
  disabled: boolean;

  private modelValue: MarkerClusteringSettings;

  private propagateChange = null;

  public clusteringSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    this.clusteringSettingsFormGroup = this.fb.group({
      enable: [null, []],
      zoomOnClick: [null, []],
      maxZoom: [null, [Validators.min(0), Validators.max(18)]],
      maxClusterRadius: [null, [Validators.min(0)]],
      zoomAnimation: [null, []],
      showCoverageOnHover: [null, []],
      spiderfyOnMaxZoom: [null, []],
      chunkedLoad: [null, []],
      lazyLoad: [null, []],
      useClusterMarkerColorFunction: [null, []],
      clusterMarkerColorFunction: [null, []]
    });
    this.clusteringSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.clusteringSettingsFormGroup.get('enable').valueChanges,
          this.clusteringSettingsFormGroup.get('useClusterMarkerColorFunction').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
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
      this.clusteringSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.clusteringSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: MarkerClusteringSettings): void {
    this.modelValue = value;
    this.clusteringSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.settingsExpanded = this.clusteringSettingsFormGroup.get('enable').value;
    this.updateValidators();
    this.clusteringSettingsFormGroup.get('enable').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((enable) => {
      this.settingsExpanded = enable;
    });
  }

  public validate(c: UntypedFormControl) {
    const valid = this.clusteringSettingsFormGroup.valid;
    return valid ? null : {
      markerClustering: {
        valid: false,
      },
    };
  }

  private updateValidators() {
    const enable: boolean = this.clusteringSettingsFormGroup.get('enable').value;
    const useClusterMarkerColorFunction: boolean = this.clusteringSettingsFormGroup.get('useClusterMarkerColorFunction').value;
    if (enable) {
      this.clusteringSettingsFormGroup.enable({emitEvent: false});
      if (!useClusterMarkerColorFunction) {
        this.clusteringSettingsFormGroup.get('clusterMarkerColorFunction').disable({emitEvent: false});
      }
    } else {
      this.clusteringSettingsFormGroup.disable({emitEvent: false});
      this.clusteringSettingsFormGroup.get('enable').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.clusteringSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}

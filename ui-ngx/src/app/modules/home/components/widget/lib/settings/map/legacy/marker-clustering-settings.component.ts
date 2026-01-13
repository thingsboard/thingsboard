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
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MarkerClusteringSettings } from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  ]
})
export class MarkerClusteringSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: MarkerClusteringSettings;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private propagateChange = null;

  public markerClusteringSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.markerClusteringSettingsFormGroup = this.fb.group({
      useClusterMarkers: [null, []],
      zoomOnClick: [null, []],
      maxZoom: [null, [Validators.min(0), Validators.max(18)]],
      maxClusterRadius: [null, [Validators.min(0)]],
      animate: [null, []],
      spiderfyOnMaxZoom: [null, []],
      showCoverageOnHover: [null, []],
      chunkedLoading: [null, []],
      removeOutsideVisibleBounds: [null, []],
      useIconCreateFunction: [null, []],
      clusterMarkerFunction: [null, []]
    });
    this.markerClusteringSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.markerClusteringSettingsFormGroup.get('useClusterMarkers').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
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
      this.markerClusteringSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.markerClusteringSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators(false);
    }
  }

  writeValue(value: MarkerClusteringSettings): void {
    this.modelValue = value;
    this.markerClusteringSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.markerClusteringSettingsFormGroup.valid ? null : {
      markerClusteringSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: MarkerClusteringSettings = this.markerClusteringSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const useClusterMarkers: boolean = this.markerClusteringSettingsFormGroup.get('useClusterMarkers').value;

    this.markerClusteringSettingsFormGroup.disable({emitEvent: false});
    this.markerClusteringSettingsFormGroup.get('useClusterMarkers').enable({emitEvent: false});

    if (useClusterMarkers) {
      this.markerClusteringSettingsFormGroup.enable({emitEvent: false});
    }
    this.markerClusteringSettingsFormGroup.updateValueAndValidity({emitEvent: false});
  }
}

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
import { TripAnimationMarkerSettings } from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-trip-animation-marker-settings',
  templateUrl: './trip-animation-marker-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TripAnimationMarkerSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TripAnimationMarkerSettingsComponent),
      multi: true
    }
  ]
})
export class TripAnimationMarkerSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: TripAnimationMarkerSettings;

  private propagateChange = null;

  public tripAnimationMarkerSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.tripAnimationMarkerSettingsFormGroup = this.fb.group({
      rotationAngle: [null, [Validators.min(0), Validators.max(360)]],
      showLabel: [null, []],
      useLabelFunction: [null, []],
      label: [null, []],
      labelFunction: [null, []],
      useMarkerImageFunction: [null, []],
      markerImage: [null, []],
      markerImageSize: [null, [Validators.min(1)]],
      markerImageFunction: [null, []],
      markerImages: [null, []]
    });
    this.tripAnimationMarkerSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.tripAnimationMarkerSettingsFormGroup.get('showLabel').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationMarkerSettingsFormGroup.get('useMarkerImageFunction').valueChanges.pipe(
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
      this.tripAnimationMarkerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripAnimationMarkerSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TripAnimationMarkerSettings): void {
    this.modelValue = value;
    this.tripAnimationMarkerSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.tripAnimationMarkerSettingsFormGroup.valid ? null : {
      tripAnimationMarkerSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TripAnimationMarkerSettings = this.tripAnimationMarkerSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showLabel: boolean = this.tripAnimationMarkerSettingsFormGroup.get('showLabel').value;
    const useLabelFunction: boolean = this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').value;
    const useMarkerImageFunction: boolean = this.tripAnimationMarkerSettingsFormGroup.get('useMarkerImageFunction').value;
    if (showLabel) {
      this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').enable({emitEvent: false});
      if (useLabelFunction) {
        this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').enable({emitEvent});
        this.tripAnimationMarkerSettingsFormGroup.get('label').disable({emitEvent});
      } else {
        this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').disable({emitEvent});
        this.tripAnimationMarkerSettingsFormGroup.get('label').enable({emitEvent});
      }
    } else {
      this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').disable({emitEvent: false});
      this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('label').disable({emitEvent});
    }
    if (useMarkerImageFunction) {
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageFunction').enable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImages').enable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImage').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageSize').disable({emitEvent});
    } else {
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageFunction').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImages').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImage').enable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageSize').enable({emitEvent});
    }
    this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('label').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImageFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImages').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImage').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImageSize').updateValueAndValidity({emitEvent: false});
  }
}

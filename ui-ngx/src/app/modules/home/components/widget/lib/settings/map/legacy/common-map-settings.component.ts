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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
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
import { CommonMapSettings, MapProviders } from '@home/components/widget/lib/maps-legacy/map-models';
import { Widget } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-common-map-settings',
    templateUrl: './common-map-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CommonMapSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CommonMapSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CommonMapSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator, OnChanges {

  @Input()
  disabled: boolean;

  @Input()
  provider: MapProviders;

  @Input()
  widget: Widget;

  mapProvider = MapProviders;

  private modelValue: CommonMapSettings;

  private propagateChange = null;

  public commonMapSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.commonMapSettingsFormGroup = this.fb.group({
      latKeyName: [null, [Validators.required]],
      lngKeyName: [null, [Validators.required]],
      xPosKeyName: [null, [Validators.required]],
      yPosKeyName: [null, [Validators.required]],
      defaultZoomLevel: [null, [Validators.min(0), Validators.max(20)]],
      defaultCenterPosition: [null, []],
      disableScrollZooming: [null, []],
      disableDoubleClickZooming: [null, []],
      disableZoomControl: [null, []],
      fitMapBounds: [null, []],
      useDefaultCenterPosition: [null, []],
      mapPageSize: [null, [Validators.min(1), Validators.required]]
    });
    this.commonMapSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.updateValidators(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'provider') {
          this.updateValidators(false);
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.commonMapSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.commonMapSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: CommonMapSettings): void {
    this.modelValue = value;
    this.commonMapSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.commonMapSettingsFormGroup.valid ? null : {
      commonMapSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: CommonMapSettings = this.commonMapSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    if (this.provider === MapProviders.image) {
      this.commonMapSettingsFormGroup.get('latKeyName').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('lngKeyName').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultZoomLevel').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('useDefaultCenterPosition').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultCenterPosition').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('fitMapBounds').disable({emitEvent});

      this.commonMapSettingsFormGroup.get('xPosKeyName').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('yPosKeyName').enable({emitEvent});
    } else {
      this.commonMapSettingsFormGroup.get('latKeyName').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('lngKeyName').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultZoomLevel').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('useDefaultCenterPosition').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultCenterPosition').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('fitMapBounds').enable({emitEvent});


      this.commonMapSettingsFormGroup.get('xPosKeyName').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('yPosKeyName').disable({emitEvent});
    }
    this.commonMapSettingsFormGroup.get('latKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('lngKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('xPosKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('yPosKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('defaultZoomLevel').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('useDefaultCenterPosition').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('defaultCenterPosition').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('fitMapBounds').updateValueAndValidity({emitEvent: false});
  }
}

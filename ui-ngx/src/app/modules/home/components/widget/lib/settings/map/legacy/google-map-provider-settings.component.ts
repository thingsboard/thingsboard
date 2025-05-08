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
import {
  GoogleMapProviderSettings,
  GoogleMapType,
  googleMapTypeProviderTranslationMap
} from '@home/components/widget/lib/maps-legacy/map-models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-google-map-provider-settings',
  templateUrl: './google-map-provider-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GoogleMapProviderSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => GoogleMapProviderSettingsComponent),
      multi: true
    }
  ]
})
export class GoogleMapProviderSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: GoogleMapProviderSettings;

  private propagateChange = null;

  public providerSettingsFormGroup: UntypedFormGroup;

  googleMapTypes = Object.values(GoogleMapType);

  googleMapTypeTranslations = googleMapTypeProviderTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.providerSettingsFormGroup = this.fb.group({
      gmApiKey: [null, [Validators.required]],
      gmDefaultMapType: [null, [Validators.required]]
    });
    this.providerSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.providerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.providerSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: GoogleMapProviderSettings): void {
    this.modelValue = value;
    this.providerSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  public validate(c: UntypedFormControl) {
    return this.providerSettingsFormGroup.valid ? null : {
      googleMapProviderSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: GoogleMapProviderSettings = this.providerSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}

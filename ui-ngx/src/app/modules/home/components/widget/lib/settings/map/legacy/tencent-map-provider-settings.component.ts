// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
  TencentMapProviderSettings,
  TencentMapType,
  tencentMapTypeProviderTranslationMap
} from '@home/components/widget/lib/maps-legacy/map-models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-tencent-map-provider-settings',
    templateUrl: './tencent-map-provider-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TencentMapProviderSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TencentMapProviderSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TencentMapProviderSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: TencentMapProviderSettings;

  private propagateChange = null;

  public providerSettingsFormGroup: UntypedFormGroup;

  tencentMapTypes = Object.values(TencentMapType);

  tencentMapTypeTranslations = tencentMapTypeProviderTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.providerSettingsFormGroup = this.fb.group({
      tmApiKey: [null, [Validators.required]],
      tmDefaultMapType: [null, [Validators.required]]
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

  writeValue(value: TencentMapProviderSettings): void {
    this.modelValue = value;
    this.providerSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  public validate(c: UntypedFormControl) {
    return this.providerSettingsFormGroup.valid ? null : {
      tencentMapProviderSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TencentMapProviderSettings = this.providerSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}

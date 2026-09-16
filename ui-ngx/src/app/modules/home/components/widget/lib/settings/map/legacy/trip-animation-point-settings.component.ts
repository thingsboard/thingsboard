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
  Validator, Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  PointsSettings,
  PolylineDecoratorSymbol,
  polylineDecoratorSymbolTranslationMap,
  PolylineSettings
} from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-trip-animation-point-settings',
    templateUrl: './trip-animation-point-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TripAnimationPointSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TripAnimationPointSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TripAnimationPointSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: PointsSettings;

  private propagateChange = null;

  public tripAnimationPointSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.tripAnimationPointSettingsFormGroup = this.fb.group({
      showPoints: [null, []],
      pointColor: [null, []],
      pointSize: [null, [Validators.min(1)]],
      useColorPointFunction: [null, []],
      colorPointFunction: [null, []],
      usePointAsAnchor: [null, []],
      pointAsAnchorFunction: [null, []],
      pointTooltipOnRightPanel: [null, []]
    });
    this.tripAnimationPointSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.tripAnimationPointSettingsFormGroup.get('showPoints').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationPointSettingsFormGroup.get('useColorPointFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationPointSettingsFormGroup.get('usePointAsAnchor').valueChanges.pipe(
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
      this.tripAnimationPointSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripAnimationPointSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: PointsSettings): void {
    this.modelValue = value;
    this.tripAnimationPointSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.tripAnimationPointSettingsFormGroup.valid ? null : {
      tripAnimationPointSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: PointsSettings = this.tripAnimationPointSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showPoints: boolean = this.tripAnimationPointSettingsFormGroup.get('showPoints').value;
    const useColorPointFunction: boolean = this.tripAnimationPointSettingsFormGroup.get('useColorPointFunction').value;
    const usePointAsAnchor: boolean = this.tripAnimationPointSettingsFormGroup.get('usePointAsAnchor').value;

    this.tripAnimationPointSettingsFormGroup.disable({emitEvent: false});
    this.tripAnimationPointSettingsFormGroup.get('showPoints').enable({emitEvent: false});

    if (showPoints) {
      this.tripAnimationPointSettingsFormGroup.get('pointColor').enable({emitEvent: false});
      this.tripAnimationPointSettingsFormGroup.get('pointSize').enable({emitEvent: false});
      this.tripAnimationPointSettingsFormGroup.get('useColorPointFunction').enable({emitEvent: false});
      if (useColorPointFunction) {
        this.tripAnimationPointSettingsFormGroup.get('colorPointFunction').enable({emitEvent: false});
      }
      this.tripAnimationPointSettingsFormGroup.get('usePointAsAnchor').enable({emitEvent: false});
      if (usePointAsAnchor) {
        this.tripAnimationPointSettingsFormGroup.get('pointAsAnchorFunction').enable({emitEvent: false});
      }
      this.tripAnimationPointSettingsFormGroup.get('pointTooltipOnRightPanel').enable({emitEvent: false});
    }
    this.tripAnimationPointSettingsFormGroup.updateValueAndValidity({emitEvent: false});
  }
}

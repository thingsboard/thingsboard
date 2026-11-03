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
import { PolylineSettings } from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-route-map-settings',
    templateUrl: './route-map-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RouteMapSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => RouteMapSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class RouteMapSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: PolylineSettings;

  private propagateChange = null;

  public routeMapSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.routeMapSettingsFormGroup = this.fb.group({
      strokeWeight: [null, [Validators.min(0)]],
      strokeOpacity: [null, [Validators.min(0), Validators.max(1)]]
    });
    this.routeMapSettingsFormGroup.valueChanges.pipe(
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
      this.routeMapSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.routeMapSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: PolylineSettings): void {
    this.modelValue = value;
    this.routeMapSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  public validate(c: UntypedFormControl) {
    return this.routeMapSettingsFormGroup.valid ? null : {
      routeMapSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: PolylineSettings = this.routeMapSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}

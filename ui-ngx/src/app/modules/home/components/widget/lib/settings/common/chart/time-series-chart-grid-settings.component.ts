// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { TimeSeriesChartGridSettings } from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-time-series-chart-grid-settings',
    templateUrl: './time-series-chart-grid-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TimeSeriesChartGridSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TimeSeriesChartGridSettingsComponent implements OnInit, ControlValueAccessor {

  settingsExpanded = false;

  @Input()
  disabled: boolean;

  private modelValue: TimeSeriesChartGridSettings;

  private propagateChange = null;

  public gridSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.gridSettingsFormGroup = this.fb.group({
      show: [null, []],
      backgroundColor: [null, []],
      borderWidth: [null, [Validators.min(0)]],
      borderColor: [null, []],
    });
    this.gridSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.gridSettingsFormGroup.get('show').valueChanges.pipe(
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
      this.gridSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.gridSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartGridSettings): void {
    this.modelValue = value;
    this.gridSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.gridSettingsFormGroup.get('show').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((show) => {
      this.settingsExpanded = show;
    });
  }

  private updateValidators() {
    const show: boolean = this.gridSettingsFormGroup.get('show').value;
    if (show) {
      this.gridSettingsFormGroup.enable({emitEvent: false});
    } else {
      this.gridSettingsFormGroup.disable({emitEvent: false});
      this.gridSettingsFormGroup.get('show').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.gridSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}

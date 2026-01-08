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

import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TimeSeriesChartAxisSettings } from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  TimeSeriesChartAxisSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings-panel.component';

@Component({
  selector: 'tb-time-series-chart-axis-settings-button',
  templateUrl: './time-series-chart-axis-settings-button.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartAxisSettingsButtonComponent),
      multi: true
    }
  ]
})
export class TimeSeriesChartAxisSettingsButtonComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  axisType: 'xAxis' | 'yAxis' = 'xAxis';

  @Input()
  panelTitle: string;

  @Input()
  @coerceBoolean()
  advanced = false;

  private modelValue: TimeSeriesChartAxisSettings;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: TimeSeriesChartAxisSettings): void {
    this.modelValue = value;
  }

  openAxisSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const axisSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: TimeSeriesChartAxisSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          axisSettings: this.modelValue,
          axisType: this.axisType,
          panelTitle: this.panelTitle,
          advanced: this.advanced
        },
        isModal: true
      });
      axisSettingsPanelPopover.tbComponentRef.instance.popover = axisSettingsPanelPopover;
      axisSettingsPanelPopover.tbComponentRef.instance.axisSettingsApplied.subscribe((axisSettings) => {
        axisSettingsPanelPopover.hide();
        this.modelValue = axisSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }

}

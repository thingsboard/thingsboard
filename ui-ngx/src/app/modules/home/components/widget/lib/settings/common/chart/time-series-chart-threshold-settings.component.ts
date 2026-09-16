// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatIconButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { WidgetConfig } from '@shared/models/widget.models';
import {
  TimeSeriesChartThreshold,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  TimeSeriesChartThresholdSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings-panel.component';

@Component({
    selector: 'tb-time-series-chart-threshold-settings',
    templateUrl: './time-series-chart-threshold-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TimeSeriesChartThresholdSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TimeSeriesChartThresholdSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  widgetConfig: WidgetConfig;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  @coerceBoolean()
  hideYAxis = false;

  @Input()
  @coerceBoolean()
  boxButton = false;

  @Input()
  icon = 'settings';

  @Input()
  title = 'widgets.time-series-chart.threshold.threshold-settings';

  private modelValue: Partial<TimeSeriesChartThreshold>;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Partial<TimeSeriesChartThreshold>): void {
    this.modelValue = value;
  }

  openThresholdSettingsPopup($event: Event, matButton: MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        thresholdSettings: deepClone(this.modelValue),
        panelTitle: this.title,
        widgetConfig: this.widgetConfig,
        hideYAxis: this.hideYAxis,
        yAxisIds: this.yAxisIds
      };
      const thresholdSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: TimeSeriesChartThresholdSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          thresholdSettings: deepClone(this.modelValue),
          panelTitle: this.title,
          widgetConfig: this.widgetConfig,
          hideYAxis: this.hideYAxis,
          yAxisIds: this.yAxisIds
        },
        isModal: true
      });
      thresholdSettingsPanelPopover.tbComponentRef.instance.popover = thresholdSettingsPanelPopover;
      thresholdSettingsPanelPopover.tbComponentRef.instance.thresholdSettingsApplied.subscribe((thresholdSettings) => {
        thresholdSettingsPanelPopover.hide();
        this.modelValue = thresholdSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }

}

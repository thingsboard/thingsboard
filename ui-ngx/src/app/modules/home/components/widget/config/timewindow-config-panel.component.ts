// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { WidgetConfig, widgetType } from '@shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { isDefined } from '@core/utils';
import { TimewindowStyle } from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface TimewindowConfigData {
  useDashboardTimewindow: boolean;
  displayTimewindow: boolean;
  timewindow: Timewindow;
  timewindowStyle: TimewindowStyle;
}

export const getTimewindowConfig = (config: WidgetConfig): TimewindowConfigData => ({
    useDashboardTimewindow: isDefined(config.useDashboardTimewindow) ?
      config.useDashboardTimewindow : true,
    displayTimewindow: isDefined(config.displayTimewindow) ?
      config.displayTimewindow : true,
    timewindow: config.timewindow,
    timewindowStyle: config.timewindowStyle
  });

export const setTimewindowConfig = (config: WidgetConfig, data: TimewindowConfigData): void => {
  config.useDashboardTimewindow = data.useDashboardTimewindow;
  config.displayTimewindow = data.displayTimewindow;
  config.timewindow = data.timewindow;
  config.timewindowStyle = data.timewindowStyle;
};

@Component({
    selector: 'tb-timewindow-config-panel',
    templateUrl: './timewindow-config-panel.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TimewindowConfigPanelComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TimewindowConfigPanelComponent implements ControlValueAccessor, OnInit {

  widgetTypes = widgetType;

  public get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  onlyHistoryTimewindow = false;


  timewindowConfig: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              public translate: TranslateService,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.timewindowConfig = this.fb.group({
      useDashboardTimewindow: [null, []],
      displayTimewindow: [null, []],
      timewindow: [null, []],
      timewindowStyle: [null, []]
    });
    this.timewindowConfig.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.propagateChange(this.timewindowConfig.getRawValue())
    );
    this.timewindowConfig.get('useDashboardTimewindow').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateTimewindowConfigEnabledState();
    });
    this.timewindowConfig.get('displayTimewindow').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateTimewindowConfigEnabledState();
    });
  }

  writeValue(data?: TimewindowConfigData): void {
    this.timewindowConfig.patchValue(data || {}, {emitEvent: false});
    this.updateTimewindowConfigEnabledState();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.timewindowConfig.disable({emitEvent: false});
    } else {
      this.timewindowConfig.enable({emitEvent: false});
      this.updateTimewindowConfigEnabledState();
    }
  }

  private updateTimewindowConfigEnabledState() {
    const useDashboardTimewindow: boolean = this.timewindowConfig.get('useDashboardTimewindow').value;
    const displayTimewindow: boolean = this.timewindowConfig.get('displayTimewindow').value;
    if (useDashboardTimewindow) {
      this.timewindowConfig.get('displayTimewindow').disable({emitEvent: false});
      this.timewindowConfig.get('timewindow').disable({emitEvent: false});
      this.timewindowConfig.get('timewindowStyle').disable({emitEvent: false});
    } else {
      this.timewindowConfig.get('displayTimewindow').enable({emitEvent: false});
      this.timewindowConfig.get('timewindow').enable({emitEvent: false});
      if (displayTimewindow) {
        this.timewindowConfig.get('timewindowStyle').enable({emitEvent: false});
      } else {
        this.timewindowConfig.get('timewindowStyle').disable({emitEvent: false});
      }
    }
  }

}

///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { widgetType } from '@shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';

export interface TimewindowConfigData {
  useDashboardTimewindow: boolean;
  displayTimewindow: boolean;
  timewindow: Timewindow;
}

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
  ]
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
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  ngOnInit() {
    this.timewindowConfig = this.fb.group({
      useDashboardTimewindow: [null, []],
      displayTimewindow: [null, []],
      timewindow: [null, []]
    });
    this.timewindowConfig.valueChanges.subscribe(
      (val) => this.propagateChange(val)
    );
    this.timewindowConfig.get('useDashboardTimewindow').valueChanges.subscribe(() => {
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
    if (useDashboardTimewindow) {
      this.timewindowConfig.get('displayTimewindow').disable({emitEvent: false});
      this.timewindowConfig.get('timewindow').disable({emitEvent: false});
    } else {
      this.timewindowConfig.get('displayTimewindow').enable({emitEvent: false});
      this.timewindowConfig.get('timewindow').enable({emitEvent: false});
    }
  }

}

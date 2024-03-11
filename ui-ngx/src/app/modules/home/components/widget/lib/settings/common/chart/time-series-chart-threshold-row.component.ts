///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input, OnChanges,
  OnInit,
  Output,
  Renderer2, SimpleChanges,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  TimeSeriesChartThreshold,
  TimeSeriesChartThresholdType, TimeSeriesChartYAxisId,
  timeSeriesThresholdTypes,
  timeSeriesThresholdTypeTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  TimeSeriesChartThresholdsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-thresholds-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKey, Datasource, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone } from '@core/utils';
import {
  TimeSeriesChartThresholdSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings-panel.component';

@Component({
  selector: 'tb-time-series-chart-threshold-row',
  templateUrl: './time-series-chart-threshold-row.component.html',
  styleUrls: ['./time-series-chart-threshold-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartThresholdRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartThresholdRowComponent implements ControlValueAccessor, OnInit, OnChanges {

  DataKeyType = DataKeyType;

  DatasourceType = DatasourceType;

  TimeSeriesChartThresholdType = TimeSeriesChartThresholdType;

  timeSeriesThresholdTypes = timeSeriesThresholdTypes;

  timeSeriesThresholdTypeTranslations = timeSeriesThresholdTypeTranslations;

  get aliasController(): IAliasController {
    return this.thresholdsPanel.aliasController;
  }

  get dataKeyCallbacks(): DataKeysCallbacks {
    return this.thresholdsPanel.dataKeyCallbacks;
  }

  get datasource(): Datasource {
    return this.thresholdsPanel.datasource;
  }

  get widgetConfig(): WidgetConfig {
    return this.thresholdsPanel.widgetConfig;
  }

  @Input()
  disabled: boolean;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Output()
  thresholdRemoved = new EventEmitter();

  thresholdFormGroup: UntypedFormGroup;

  modelValue: TimeSeriesChartThreshold;

  latestKeyFormControl: UntypedFormControl;

  entityKeyFormControl: UntypedFormControl;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private thresholdsPanel: TimeSeriesChartThresholdsPanelComponent,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.thresholdFormGroup = this.fb.group({
      type: [null, []],
      value: [null, [Validators.required]],
      entityAlias: [null, [Validators.required]],
      yAxisId: [null, [Validators.required]],
      lineColor: [null, []],
      units: [null, []],
      decimals: [null, []]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    this.thresholdFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.latestKeyFormControl.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.entityKeyFormControl.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.thresholdFormGroup.get('type').valueChanges.subscribe(() => {
      this.updateValidators();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['yAxisIds'].includes(propName)) {
          if (this.modelValue?.yAxisId &&
            !this.yAxisIds.includes(this.modelValue.yAxisId)) {
            this.thresholdFormGroup.patchValue({yAxisId: 'default'}, {emitEvent: true});
          }
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
      this.thresholdFormGroup.disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else {
      this.thresholdFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartThreshold): void {
    this.modelValue = value;
    this.thresholdFormGroup.patchValue(
      {
        type: value.type,
        value: value.value,
        entityAlias: value.entityAlias,
        yAxisId: value.yAxisId,
        lineColor: value.lineColor,
        units: value.units,
        decimals: value.decimals,
      }, {emitEvent: false}
    );
    if (value.type === TimeSeriesChartThresholdType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === TimeSeriesChartThresholdType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }
    this.updateValidators();
    this.cd.markForCheck();
  }

  editThreshold($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        thresholdSettings: deepClone(this.modelValue),
        widgetConfig: this.widgetConfig,
        yAxisIds: this.yAxisIds
      };
      const thresholdSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, TimeSeriesChartThresholdSettingsPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      thresholdSettingsPanelPopover.tbComponentRef.instance.popover = thresholdSettingsPanelPopover;
      thresholdSettingsPanelPopover.tbComponentRef.instance.thresholdSettingsApplied.subscribe((thresholdSettings) => {
        thresholdSettingsPanelPopover.hide();
        this.modelValue = {...this.modelValue, ...thresholdSettings};
        this.thresholdFormGroup.patchValue(
          {
            yAxisId: this.modelValue.yAxisId,
            units: this.modelValue.units,
            decimals: this.modelValue.decimals,
            lineColor: this.modelValue.lineColor
          },
          {emitEvent: false});
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateValidators() {
    const type: TimeSeriesChartThresholdType = this.thresholdFormGroup.get('type').value;
    if (type === TimeSeriesChartThresholdType.constant) {
      this.thresholdFormGroup.get('value').enable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === TimeSeriesChartThresholdType.latestKey) {
      this.thresholdFormGroup.get('value').disable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.enable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === TimeSeriesChartThresholdType.entity) {
      this.thresholdFormGroup.get('value').disable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').enable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.enable({emitEvent: false});
    }
  }

  private updateModel() {
    const value = this.thresholdFormGroup.value;
    this.modelValue.type = value.type;
    this.modelValue.value = value.value;
    this.modelValue.entityAlias = value.entityAlias;
    this.modelValue.yAxisId = value.yAxisId;
    this.modelValue.lineColor = value.lineColor;
    this.modelValue.units = value.units;
    this.modelValue.decimals = value.decimals;
    if (value.type === TimeSeriesChartThresholdType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      this.modelValue.latestKey = latestKey?.name;
      this.modelValue.latestKeyType = (latestKey?.type as any);
    } else if (value.type === TimeSeriesChartThresholdType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      this.modelValue.entityKey = entityKey?.name;
      this.modelValue.entityKeyType = (entityKey?.type as any);
    }
    this.propagateChange(this.modelValue);
  }
}

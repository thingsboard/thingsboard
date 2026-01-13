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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import {
  ComparisonResultType,
  DataKey,
  DataKeyConfigMode,
  DatasourceType,
  Widget,
  widgetType
} from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { AggregationType } from '@shared/models/time/time.models';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component';
import { deepClone, formatValue } from '@core/utils';
import {
  AggregatedValueCardKeyPosition,
  aggregatedValueCardKeyPositionTranslations,
  AggregatedValueCardKeySettings
} from '@home/components/widget/lib/cards/aggregated-value-card.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getSourceTbUnitSymbol, TbUnit } from '@shared/models/unit.models';

@Component({
  selector: 'tb-aggregated-data-key-row',
  templateUrl: './aggregated-data-key-row.component.html',
  styleUrls: ['./aggregated-data-key-row.component.scss', '../../../lib/settings/common/key/data-keys.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AggregatedDataKeyRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class AggregatedDataKeyRowComponent implements ControlValueAccessor, OnInit, OnChanges {

  aggregatedValueCardKeyPositions: AggregatedValueCardKeyPosition[] =
    Object.keys(AggregatedValueCardKeyPosition).map(value => AggregatedValueCardKeyPosition[value]);

  aggregatedValueCardKeyPositionTranslationMap = aggregatedValueCardKeyPositionTranslations;

  dataKeyTypes = DataKeyType;

  aggregationTypes = AggregationType;

  comparisonResultTypes = ComparisonResultType;

  @Input()
  disabled: boolean;

  @Input()
  datasourceType: DatasourceType;

  @Input()
  keyName: string;

  @Input()
  index: number;

  @Output()
  keyRemoved = new EventEmitter();

  keyRowFormGroup: UntypedFormGroup;

  modelValue: DataKey;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  get callbacks(): WidgetConfigCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  get widget(): Widget {
    return this.widgetConfigComponent.widget;
  }

  get latestDataKeySettingsForm(): FormProperty[] {
    return this.widgetConfigComponent.modelValue?.latestDataKeySettingsForm;
  }

  get latestDataKeySettingsDirective(): string {
    return this.widgetConfigComponent.modelValue?.latestDataKeySettingsDirective;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.keyRowFormGroup = this.fb.group({
      position: [null, []],
      units: [null, []],
      decimals: [null, []],
      font: [null, []],
      color: [null, []],
      showArrow: [null, []]
    });
    this.keyRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['keyName'].includes(propName)) {
          if (change.currentValue) {
            this.modelValue.name = change.currentValue;
            setTimeout(() => {
              this.updateModel();
            }, 0);
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
      this.keyRowFormGroup.disable({emitEvent: false});
    } else {
      this.keyRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKey): void {
    this.modelValue = value || {} as DataKey;
    const settings: AggregatedValueCardKeySettings = (this.modelValue.settings || {});
    this.keyRowFormGroup.patchValue(
      {
        position: settings.position || AggregatedValueCardKeyPosition.center,
        units: value?.units,
        decimals: value?.decimals,
        font: settings.font,
        color: settings.color,
        showArrow: settings.showArrow
      }, {emitEvent: false}
    );
    this.cd.markForCheck();
  }

  dataKeyHasPostprocessing(): boolean {
    return !!this.modelValue?.postFuncBody;
  }

  editKey() {
    this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(this.modelValue),
          dataKeyConfigMode: DataKeyConfigMode.general,
          dataKeySettingsForm: this.latestDataKeySettingsForm,
          dataKeySettingsDirective: this.latestDataKeySettingsDirective,
          dashboard: null,
          aliasController: null,
          widget: this.widget,
          widgetType: widgetType.latest,
          deviceId: null,
          entityAliasId: null,
          showPostProcessing: true,
          callbacks: this.callbacks,
          hideDataKeyName: true,
          hideDataKeyLabel: true,
          hideDataKeyColor: true,
          supportsUnitConversion: true
        }
      }).afterClosed().subscribe((updatedDataKey) => {
      if (updatedDataKey) {
        this.modelValue = updatedDataKey;
        const settings: AggregatedValueCardKeySettings = (this.modelValue.settings || {});
        this.keyRowFormGroup.get('position').patchValue(settings.position, {emitEvent: false});
        this.keyRowFormGroup.get('font').patchValue(settings.font, {emitEvent: false});
        this.keyRowFormGroup.get('color').patchValue(settings.color, {emitEvent: false});
        this.keyRowFormGroup.get('showArrow').patchValue(settings.showArrow, {emitEvent: false});
        this.keyRowFormGroup.get('units').patchValue(this.modelValue.units, {emitEvent: false});
        this.keyRowFormGroup.get('decimals').patchValue(this.modelValue.decimals, {emitEvent: false});
        this.updateModel();
        this.cd.markForCheck();
      }
    });
  }

  private updateModel() {
    const value = this.keyRowFormGroup.value;
    this.modelValue.settings = this.modelValue.settings || {};
    this.modelValue.settings.position = value.position;
    this.modelValue.settings.font = value.font;
    this.modelValue.settings.color = value.color;
    this.modelValue.settings.showArrow = value.showArrow;
    this.modelValue.units = value.units;
    this.modelValue.decimals = value.decimals;
    this.propagateChange(this.modelValue);
  }

  private _valuePreviewFn(): string {
    const units: TbUnit = this.keyRowFormGroup.get('units').value;
    const decimals: number = this.keyRowFormGroup.get('decimals').value;
    return formatValue(22, decimals, getSourceTbUnitSymbol(units), true);
  }
}

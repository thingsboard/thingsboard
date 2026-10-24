// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit, Optional,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKey, DatasourceType, Widget, widgetType } from '@shared/models/widget.models';
import { dataKeyRowValidator, dataKeyValid } from '@home/components/widget/config/basic/common/data-key-row.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import {
  DataKeySettingsFormFunction,
  DataKeySettingsFunction
} from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TimeSeriesChartYAxisId } from '@home/components/widget/lib/chart/time-series-chart.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isDefinedAndNotNull } from '@core/utils';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { IAliasController } from '@core/api/widget-api.models';

export interface DataKeysPanelOptions {
  widgetType?: widgetType;
  callbacks?: WidgetConfigCallbacks;
  settingsForm?: FormProperty[];
  settingsFormFunction?: DataKeySettingsFormFunction;
  settingsFormTrimDefaults?: boolean;
  settingsDirective?: string;
  settingsFunction?: DataKeySettingsFunction;
  latestSettingsForm?: FormProperty[];
  latestSettingsFormFunction?: DataKeySettingsFormFunction;
  latestSettingsFormTrimDefaults?: boolean;
  hasAdditionalLatestDataKeys?: boolean;
  widget?: Widget;
}

@Component({
    selector: 'tb-data-keys-panel',
    templateUrl: './data-keys-panel.component.html',
    styleUrls: ['./data-keys-panel.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DataKeysPanelComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DataKeysPanelComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DataKeysPanelComponent implements ControlValueAccessor, OnInit, OnChanges, Validator {

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  stroked = false;

  @Input()
  panelTitle: string;

  @Input()
  addKeyTitle: string;

  @Input()
  keySettingsTitle: string;

  @Input()
  removeKeyTitle: string;

  @Input()
  noKeysText: string;

  @Input()
  requiredKeysText: string;

  @Input()
  datasourceType: DatasourceType;

  @Input()
  entityAliasId: string;

  @Input()
  deviceId: string;

  @Input()
  @coerceBoolean()
  reportMode = false;

  @Input()
  @coerceBoolean()
  hidePanel = false;

  @Input()
  @coerceBoolean()
  hideDataKeyColor = false;

  @Input()
  @coerceBoolean()
  hideUnits = false;

  @Input()
  @coerceBoolean()
  hideDecimals = false;

  @Input()
  @coerceBoolean()
  hideDataKeyUnits = false;

  @Input()
  @coerceBoolean()
  hideDataKeyDecimals = false;

  @Input()
  @coerceBoolean()
  hideSourceSelection = false;

  @Input()
  @coerceBoolean()
  timeSeriesChart = false;

  @Input()
  @coerceBoolean()
  showTimeSeriesType = false;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeysPanelOptions: DataKeysPanelOptions;

  dataKeyType: DataKeyType;

  keysListFormGroup: UntypedFormGroup;

  errorText = '';

  get widgetType(): widgetType {
    return this.widgetConfigComponent?.widgetType || this.getDataKeysPanelOption('widgetType');
  }

  get callbacks(): WidgetConfigCallbacks {
    return this.widgetConfigComponent?.widgetConfigCallbacks || this.getDataKeysPanelOption('callbacks');
  }

  get widget(): Widget {
    return this.widgetConfigComponent?.widget || this.getDataKeysPanelOption('widget');
  }

  get hasAdditionalLatestDataKeys(): boolean {
    return !this.hideSourceSelection && this.widgetType === widgetType.timeseries &&
      (this.widgetConfigComponent?.modelValue?.typeParameters?.hasAdditionalLatestDataKeys || this.getDataKeysPanelOption('hasAdditionalLatestDataKeys'));
  }

  get dataKeySettingsForm(): FormProperty[] {
    return this.widgetConfigComponent?.modelValue?.dataKeySettingsForm || this.getDataKeysPanelOption('settingsForm');
  }

  get dataKeySettingsFormFunction(): DataKeySettingsFormFunction {
    return this.getDataKeysPanelOption('settingsFormFunction');
  }

  get dataKeySettingsFormTrimDefaults(): boolean {
    return this.hasDataKeysPanelOptions('settingsFormTrimDefaults') ? this.getDataKeysPanelOption('settingsFormTrimDefaults') : false;
  }

  get dataKeySettingsDirective(): string {
    return this.widgetConfigComponent?.modelValue?.dataKeySettingsDirective || this.getDataKeysPanelOption('settingsDirective');
  }

  get latestDataKeySettingsForm(): FormProperty[] {
    return this.widgetConfigComponent?.modelValue?.latestDataKeySettingsForm || this.getDataKeysPanelOption('latestSettingsForm');
  }

  get latestDataKeySettingsFormFunction(): DataKeySettingsFormFunction {
    return this.getDataKeysPanelOption('latestSettingsFormFunction');
  }

  get latestDataKeySettingsFormTrimDefaults(): boolean {
    return this.hasDataKeysPanelOptions('latestSettingsFormTrimDefaults') ? this.getDataKeysPanelOption('latestSettingsFormTrimDefaults') : false;
  }

  get dataKeySettingsFunction(): DataKeySettingsFunction {
    return this.widgetConfigComponent?.modelValue?.dataKeySettingsFunction || this.getDataKeysPanelOption('settingsFunction');
  }

  get dragEnabled(): boolean {
    return this.keysFormArray().controls.length > 1;
  }

  get noKeys(): boolean {
    let keys: DataKey[] = this.keysListFormGroup.get('keys').value;
    if (this.hasAdditionalLatestDataKeys) {
      keys = keys.filter(k => !(k as any)?.latest);
    }
    return keys.length === 0;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private utils: UtilsService,
              @Optional() private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.keysListFormGroup = this.fb.group({
      keys: [this.fb.array([]), []]
    });
    this.keysListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let keys: DataKey[] = this.keysListFormGroup.get('keys').value;
        if (keys) {
          keys = keys.filter(k => dataKeyValid(k));
        }
        this.propagateChange(keys);
      }
    );
    this.updateParams();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['datasourceType'].includes(propName)) {
            this.updateParams();
        }
      }
    }
  }

  private updateParams() {
    if (this.datasourceType === DatasourceType.function) {
      this.dataKeyType = DataKeyType.function;
    } else {
      if (this.widgetType !== widgetType.latest && this.widgetType !== widgetType.alarm) {
        this.dataKeyType = DataKeyType.timeseries;
      } else {
        this.dataKeyType = null;
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
      this.keysListFormGroup.disable({emitEvent: false});
    } else {
      this.keysListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKey[] | undefined): void {
    this.keysListFormGroup.setControl('keys', this.prepareKeysFormArray(value), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    this.errorText = '';
    let valid = this.keysListFormGroup.valid;
    if (this.noKeys && this.requiredKeysText) {
      valid = false;
      this.errorText = this.requiredKeysText;
    }
    return valid ? null : {
      dataKeyRows: {
        valid: false,
      },
    };
  }

  keyDrop(event: CdkDragDrop<string[]>) {
    const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
    const key = keysArray.at(event.previousIndex);
    keysArray.removeAt(event.previousIndex);
    keysArray.insert(event.currentIndex, key);
  }

  keysFormArray(): UntypedFormArray {
    return this.keysListFormGroup.get('keys') as UntypedFormArray;
  }

  removeKey(index: number) {
    (this.keysListFormGroup.get('keys') as UntypedFormArray).removeAt(index);
  }

  addKey() {
    const dataKey = this.callbacks.generateDataKey('', null, this.dataKeySettingsForm,
      false, this.dataKeySettingsFunction);
    dataKey.label = '';
    dataKey.decimals = 0;
    if (this.hasAdditionalLatestDataKeys) {
      (dataKey as any).latest = false;
    }
    const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
    const keyControl = this.fb.control(dataKey, [dataKeyRowValidator]);
    keysArray.push(keyControl);
  }

  private prepareKeysFormArray(keys: DataKey[] | undefined): UntypedFormArray {
    const keysControls: Array<AbstractControl> = [];
    if (keys) {
      keys.forEach((key) => {
        keysControls.push(this.fb.control(key, [dataKeyRowValidator]));
      });
    }
    return this.fb.array(keysControls);
  }

  private hasDataKeysPanelOptions(key: string): boolean {
    if (this.dataKeysPanelOptions) {
      return isDefinedAndNotNull(this.dataKeysPanelOptions[key]);
    } else {
      return false;
    }
  }

  private getDataKeysPanelOption<T>(key: string): T {
    return this.dataKeysPanelOptions && this.dataKeysPanelOptions[key];
  }

}

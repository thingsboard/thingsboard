///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { ValueSourceProperty } from '@home/components/widget/lib/settings/common/value-source.component';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { isNumber } from '@core/utils';
import { IAliasController } from '@core/api/widget-api.models';
import { TbFlotKeyThreshold } from '@home/components/widget/lib/flot-widget.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-flot-threshold',
  templateUrl: './flot-threshold.component.html',
  styleUrls: ['./flot-threshold.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FlotThresholdComponent),
      multi: true
    }
  ]
})
export class FlotThresholdComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Input()
  aliasController: IAliasController;

  @Output()
  removeThreshold = new EventEmitter();

  private modelValue: TbFlotKeyThreshold;

  private propagateChange = null;

  public thresholdFormGroup: FormGroup;

  functionScopeVariables: string[];

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
  }

  ngOnInit(): void {
    this.thresholdFormGroup = this.fb.group({
      valueSource: [null, []],
      lineWidth: [null, [Validators.min(0)]],
      color: [null, []],
      usePostProcessing: [null, []],
      postFuncBody: [null, []]
    });
    this.thresholdFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.thresholdFormGroup.get('usePostProcessing').valueChanges.subscribe((usePostProcessing: boolean) => {
      const postFuncBody: string = this.thresholdFormGroup.get('postFuncBody').value;
      if (usePostProcessing && (!postFuncBody || !postFuncBody.length)) {
        this.thresholdFormGroup.get('postFuncBody').patchValue('return value;');
      } else if (!usePostProcessing && postFuncBody && postFuncBody.length) {
        this.thresholdFormGroup.get('postFuncBody').patchValue(null);
      }
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
      this.thresholdFormGroup.disable({emitEvent: false});
    } else {
      this.thresholdFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TbFlotKeyThreshold): void {
    this.modelValue = value;
    const valueSource: ValueSourceProperty = {
      valueSource: value?.thresholdValueSource,
      entityAlias: value?.thresholdEntityAlias,
      attribute: value?.thresholdAttribute,
      value: value?.thresholdValue
    };
    if (this.modelValue.postFuncBody && this.modelValue.postFuncBody.length) {
      this.modelValue.usePostProcessing = true;
    }
    this.thresholdFormGroup.patchValue({
        valueSource,
        lineWidth: value?.lineWidth,
        color: value?.color,
        usePostProcessing: value?.usePostProcessing,
        postFuncBody: value?.postFuncBody
      },
      {emitEvent: false});
  }

  thresholdText(): string {
    const value: ValueSourceProperty = this.thresholdFormGroup.get('valueSource').value;
    return this.valueSourcePropertyText(value);
  }

  private valueSourcePropertyText(source?: ValueSourceProperty): string {
    if (source) {
      if (source.valueSource === 'predefinedValue') {
        return `${isNumber(source.value) ? source.value : 0}`;
      } else if (source.valueSource === 'entityAttribute') {
        const alias = source.entityAlias || 'Undefined';
        const key = source.attribute || 'Undefined';
        return `${alias}.${key}`;
      }
    }
    return 'Undefined';
  }

  private updateModel() {
    const value: {
      valueSource: ValueSourceProperty,
      lineWidth: number,
      color: string,
      usePostProcessing?: boolean,
      postFuncBody?: string} = this.thresholdFormGroup.value;
    this.modelValue = {
      thresholdValueSource: value?.valueSource?.valueSource,
      thresholdEntityAlias: value?.valueSource?.entityAlias,
      thresholdAttribute: value?.valueSource?.attribute,
      thresholdValue: value?.valueSource?.value,
      lineWidth: value?.lineWidth,
      color: value?.color,
      usePostProcessing: value?.usePostProcessing,
      postFuncBody: value?.postFuncBody
    };
    this.propagateChange(this.modelValue);
  }
}

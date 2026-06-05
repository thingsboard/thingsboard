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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { aggregationTranslations, AggregationType } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual } from '@core/utils';

@Component({
    selector: 'tb-aggregation-type-select',
    templateUrl: './aggregation-type-select.component.html',
    styleUrls: ['./aggregation-type-select.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AggregationTypeSelectComponent),
            multi: true
        }],
    standalone: false
})
export class AggregationTypeSelectComponent implements ControlValueAccessor, OnInit, OnChanges {

  aggregationTypeFormGroup: FormGroup;

  modelValue: AggregationType | null;

  @Input()
  allowedAggregationTypes: Array<AggregationType>;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  displayLabel = true;

  @Input()
  labelText: string;

  get label(): string {
    if (this.labelText && this.labelText.length) {
      return this.labelText;
    }
    return this.defaultLabel;
  }

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  aggregationTypes: Array<AggregationType>;

  private defaultLabel = this.translate.instant('aggregation.aggregation');

  private allAggregationTypes: Array<AggregationType> = Object.values(AggregationType);

  private propagateChange = (v: any) => { };

  constructor(private translate: TranslateService,
              private fb: FormBuilder) {
    this.aggregationTypeFormGroup = this.fb.group({
      aggregationType: [null]
    });
    this.aggregationTypeFormGroup.get('aggregationType').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(
      (value) => {
        let modelValue;
        if (!value) {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.aggregationTypes = this.allowedAggregationTypes?.length ? this.allowedAggregationTypes : this.allAggregationTypes;
  }

  ngOnChanges({allowedAggregationTypes}: SimpleChanges): void {
    if (allowedAggregationTypes && !allowedAggregationTypes.firstChange && !isEqual(allowedAggregationTypes.currentValue, allowedAggregationTypes.previousValue)) {
      this.aggregationTypes = this.allowedAggregationTypes?.length ? this.allowedAggregationTypes : this.allAggregationTypes;
      const currentAggregationType: AggregationType = this.aggregationTypeFormGroup.get('aggregationType').value;
      if (currentAggregationType && !this.aggregationTypes.includes(currentAggregationType)) {
        this.aggregationTypeFormGroup.get('aggregationType').patchValue(this.aggregationTypes[0], {emitEvent: true});
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.aggregationTypeFormGroup.disable();
    } else {
      this.aggregationTypeFormGroup.enable();
    }
  }

  writeValue(value: AggregationType | null): void {
    let aggregationType: AggregationType;
    if (value && this.allowedAggregationTypes?.length && !this.allowedAggregationTypes.includes(value)) {
      aggregationType = this.allowedAggregationTypes[0];
    } else {
      aggregationType = value;
    }
    this.modelValue = aggregationType;
    this.aggregationTypeFormGroup.get('aggregationType').patchValue(aggregationType, {emitEvent: false});
  }

  updateView(value: AggregationType | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayAggregationTypeFn(aggregationType?: AggregationType | null): string | undefined {
    if (aggregationType) {
      return this.translate.instant(aggregationTranslations.get(aggregationType));
    } else {
      return '';
    }
  }

}

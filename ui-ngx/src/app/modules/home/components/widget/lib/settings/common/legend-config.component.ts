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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { isDefined } from '@core/utils';
import {
  LegendConfig,
  LegendDirection,
  legendDirectionTranslationMap,
  LegendPosition,
  legendPositionTranslationMap
} from '@shared/models/widget.models';
import { Subscription } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';

// @dynamic
@Component({
    selector: 'tb-legend-config',
    templateUrl: './legend-config.component.html',
    styleUrls: ['./../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => LegendConfigComponent),
            multi: true
        }
    ],
    standalone: false
})
export class LegendConfigComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  @Input()
  @coerceBoolean()
  hideDirection = false;

  legendConfigForm: UntypedFormGroup;
  legendDirection = LegendDirection;
  legendDirections = Object.keys(LegendDirection);
  legendDirectionTranslations = legendDirectionTranslationMap;
  legendPosition = LegendPosition;
  legendPositions = Object.keys(LegendPosition);
  legendPositionTranslations = legendPositionTranslationMap;

  private legendSettingsFormChanges$: Subscription;
  private legendSettingsFormDirectionChanges$: Subscription;
  private propagateChange = (_: any) => {};

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.legendConfigForm = this.fb.group({
      position: [null, []],
      showValues: [[], []],
      sortDataKeys: [null, []]
    });
    if (!this.hideDirection) {
      this.legendConfigForm.addControl('direction', this.fb.control([null, []]));
      this.legendSettingsFormDirectionChanges$ = this.legendConfigForm.get('direction').valueChanges
      .subscribe((direction: LegendDirection) => {
        this.onDirectionChanged(direction);
      });
    }
    this.legendSettingsFormChanges$ = this.legendConfigForm.valueChanges.subscribe(
      () => this.legendConfigUpdated()
    );
  }

  private onDirectionChanged(direction: LegendDirection) {
    if (direction === LegendDirection.row) {
      let position: LegendPosition = this.legendConfigForm.get('position').value;
      if (position !== LegendPosition.bottom && position !== LegendPosition.top) {
        position = LegendPosition.bottom;
      }
      this.legendConfigForm.patchValue({position}, {emitEvent: false}
      );
    }
  }

  ngOnDestroy(): void {
    if (this.legendSettingsFormDirectionChanges$) {
      this.legendSettingsFormDirectionChanges$.unsubscribe();
      this.legendSettingsFormDirectionChanges$ = null;
    }
    if (this.legendSettingsFormChanges$) {
      this.legendSettingsFormChanges$.unsubscribe();
      this.legendSettingsFormChanges$ = null;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.legendConfigForm.disable({emitEvent: false});
    } else {
      this.legendConfigForm.enable({emitEvent: false});
    }
  }

  writeValue(legendConfig: LegendConfig): void {
    if (legendConfig) {
      const value: any = {
        position: legendConfig.position,
        showValues: this.getShowValues(legendConfig),
        sortDataKeys: isDefined(legendConfig.sortDataKeys) ? legendConfig.sortDataKeys : false
      };
      if (!this.hideDirection) {
        value.direction = legendConfig.direction;
      }
      this.legendConfigForm.patchValue(value, {emitEvent: false});
    }
    if (!this.hideDirection) {
      this.onDirectionChanged(legendConfig?.direction);
    }
  }

  private legendConfigUpdated() {
    const configValue = this.legendConfigForm.value;
    const legendConfig: Partial<LegendConfig> = {
      position: configValue.position,
      sortDataKeys: configValue.sortDataKeys
    };
    if (!this.hideDirection) {
      legendConfig.direction = configValue.direction;
    }
    this.setShowValues(configValue.showValues, legendConfig);
    this.propagateChange(legendConfig);
  }

  private getShowValues(legendConfig: LegendConfig): string[] {
    const showValues: string[] = [];
    if (isDefined(legendConfig.showMin) && legendConfig.showMin) {
      showValues.push('min');
    }
    if (isDefined(legendConfig.showMax) && legendConfig.showMax) {
      showValues.push('max');
    }
    if (isDefined(legendConfig.showAvg) && legendConfig.showAvg) {
      showValues.push('average');
    }
    if (isDefined(legendConfig.showTotal) && legendConfig.showTotal) {
      showValues.push('total');
    }
    if (isDefined(legendConfig.showLatest) && legendConfig.showLatest) {
      showValues.push('latest');
    }
    return showValues;
  }

  private setShowValues(showValues: string[], legendConfig: Partial<LegendConfig>) {
    legendConfig.showMin = showValues.includes('min');
    legendConfig.showMax = showValues.includes('max');
    legendConfig.showAvg = showValues.includes('average');
    legendConfig.showTotal = showValues.includes('total');
    legendConfig.showLatest = showValues.includes('latest');
  }
}

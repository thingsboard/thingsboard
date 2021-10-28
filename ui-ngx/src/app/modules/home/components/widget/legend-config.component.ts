///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  Component,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { isDefined } from '@core/utils';
import {
  LegendConfig,
  LegendDirection,
  legendDirectionTranslationMap,
  LegendPosition,
  legendPositionTranslationMap
} from '@shared/models/widget.models';
import { Subscription } from 'rxjs';
// @dynamic
@Component({
  selector: 'tb-legend-config',
  templateUrl: './legend-config.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LegendConfigComponent),
      multi: true
    }
  ]
})
export class LegendConfigComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  legendSettings: LegendConfig;
  legendConfigForm: FormGroup;
  legendDirection = LegendDirection;
  legendDirections = Object.keys(LegendDirection);
  legendDirectionTranslations = legendDirectionTranslationMap;
  legendPosition = LegendPosition;
  legendPositions = Object.keys(LegendPosition);
  legendPositionTranslations = legendPositionTranslationMap;

  legendSettingsChangesSubscription: Subscription;

  private propagateChange = (_: any) => {};

  constructor(public fb: FormBuilder,
              public viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    this.legendConfigForm = this.fb.group({
      direction: [null, []],
      position: [null, []],
      sortDataKeys: [null, []],
      showMin: [null, []],
      showMax: [null, []],
      showAvg: [null, []],
      showTotal: [null, []]
    });
    this.legendConfigForm.get('direction').valueChanges.subscribe((direction: LegendDirection) => {
      this.onDirectionChanged(direction);
    });
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
    this.removeChangeSubscriptions();
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

  private removeChangeSubscriptions() {
    if (this.legendSettingsChangesSubscription) {
      this.legendSettingsChangesSubscription.unsubscribe();
      this.legendSettingsChangesSubscription = null;
    }
  }

  private createChangeSubscriptions() {
    this.legendSettingsChangesSubscription = this.legendConfigForm.valueChanges.subscribe(
      () => this.legendConfigUpdated()
    );
  }

  writeValue(obj: LegendConfig): void {
    this.legendSettings = obj;
    this.removeChangeSubscriptions();
    if (this.legendSettings) {
      this.legendConfigForm.patchValue({
        direction: this.legendSettings.direction,
        position: this.legendSettings.position,
        sortDataKeys: isDefined(this.legendSettings.sortDataKeys) ? this.legendSettings.sortDataKeys : false,
        showMin: isDefined(this.legendSettings.showMin) ? this.legendSettings.showMin : false,
        showMax: isDefined(this.legendSettings.showMax) ? this.legendSettings.showMax : false,
        showAvg: isDefined(this.legendSettings.showAvg) ? this.legendSettings.showAvg : false,
        showTotal: isDefined(this.legendSettings.showTotal) ? this.legendSettings.showTotal : false
      });
    }
    this.onDirectionChanged(this.legendSettings.direction);
    this.createChangeSubscriptions();
  }

  private legendConfigUpdated() {
    this.legendSettings = this.legendConfigForm.value;
    this.propagateChange(this.legendSettings);
  }
}

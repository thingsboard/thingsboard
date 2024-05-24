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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { merge } from 'rxjs';
import {
  IotSvgBehavior,
  IotSvgBehaviorType,
  iotSvgBehaviorTypes,
  iotSvgBehaviorTypeTranslations
} from '@app/modules/home/components/widget/lib/svg/iot-svg.models';
import { ValueType, valueTypesMap } from '@shared/models/constants';
import { ValueToDataType } from '@shared/models/action-widget-settings.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-scada-symbol-behavior-panel',
  templateUrl: './scada-symbol-behavior-panel.component.html',
  styleUrls: ['./scada-symbol-behavior-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolBehaviorPanelComponent implements OnInit {

  IotSvgBehaviorType = IotSvgBehaviorType;

  ValueType = ValueType;

  ValueToDataType = ValueToDataType;

  iotSvgBehaviorTypes = iotSvgBehaviorTypes;
  iotSvgBehaviorTypeTranslations = iotSvgBehaviorTypeTranslations;

  valueTypes = Object.keys(ValueType) as ValueType[];

  valueTypesMap = valueTypesMap;

  @Input()
  isAdd = false;

  @Input()
  behavior: IotSvgBehavior;

  @Input()
  popover: TbPopoverComponent<ScadaSymbolBehaviorPanelComponent>;

  @Output()
  behaviorSettingsApplied = new EventEmitter<IotSvgBehavior>();

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  panelTitle: string;

  behaviorFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService) {
  }

  ngOnInit(): void {
    this.panelTitle = this.isAdd ? 'scada.behavior.add-behavior' : 'scada.behavior.behavior-settings';
    this.behaviorFormGroup = this.fb.group(
      {
        id: [this.behavior.id, [Validators.required]],
        name: [this.behavior.name, [Validators.required]],
        hint: [this.behavior.hint, []],
        type: [this.behavior.type, [Validators.required]],
        valueType: [this.behavior.valueType, [Validators.required]],
        defaultValue: [this.behavior.defaultValue, [Validators.required]],
        trueLabel: [this.behavior.trueLabel, []],
        falseLabel: [this.behavior.falseLabel, []],
        stateLabel: [this.behavior.stateLabel, []],
        valueToDataType: [this.behavior.valueToDataType, [Validators.required]],
        constantValue: [this.behavior.constantValue, [Validators.required]],
        valueToDataFunction: [this.behavior.valueToDataFunction, [Validators.required]]
      }
    );
    merge(this.behaviorFormGroup.get('type').valueChanges,
      this.behaviorFormGroup.get('valueType').valueChanges,
      this.behaviorFormGroup.get('valueToDataType').valueChanges).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyBehaviorSettings() {
    const behavior = this.behaviorFormGroup.getRawValue();
    this.behaviorSettingsApplied.emit(behavior);
  }

  private updateValidators() {
    const type: IotSvgBehaviorType = this.behaviorFormGroup.get('type').value;
    const valueType: ValueType = this.behaviorFormGroup.get('valueType').value;
    let valueToDataType: ValueToDataType = this.behaviorFormGroup.get('valueToDataType').value;
    this.behaviorFormGroup.disable({emitEvent: false});
    this.behaviorFormGroup.get('id').enable({emitEvent: false});
    this.behaviorFormGroup.get('name').enable({emitEvent: false});
    this.behaviorFormGroup.get('type').enable({emitEvent: false});
    this.behaviorFormGroup.get('hint').enable({emitEvent: false});
    switch (type) {
      case IotSvgBehaviorType.value:
        this.behaviorFormGroup.get('valueType').enable({emitEvent: false});
        this.behaviorFormGroup.get('defaultValue').enable({emitEvent: false});
        if (valueType === ValueType.BOOLEAN) {
          this.behaviorFormGroup.get('trueLabel').enable({emitEvent: false});
          this.behaviorFormGroup.get('falseLabel').enable({emitEvent: false});
          this.behaviorFormGroup.get('stateLabel').enable({emitEvent: false});
        }
        break;
      case IotSvgBehaviorType.action:
        if (valueType === ValueType.BOOLEAN && valueToDataType === ValueToDataType.VALUE) {
          this.behaviorFormGroup.patchValue({valueToDataType: ValueToDataType.CONSTANT}, {emitEvent: false});
          valueToDataType = ValueToDataType.CONSTANT;
        } else if (valueType !== ValueType.BOOLEAN && valueToDataType === ValueToDataType.CONSTANT) {
          this.behaviorFormGroup.patchValue({valueToDataType: ValueToDataType.VALUE}, {emitEvent: false});
          valueToDataType = ValueToDataType.VALUE;
        }
        this.behaviorFormGroup.get('valueType').enable({emitEvent: false});
        this.behaviorFormGroup.get('valueToDataType').enable({emitEvent: false});
        if (valueToDataType === ValueToDataType.CONSTANT) {
          this.behaviorFormGroup.get('constantValue').enable({emitEvent: false});
        } else if (valueToDataType === ValueToDataType.FUNCTION) {
          this.behaviorFormGroup.get('valueToDataFunction').enable({emitEvent: false});
        }
        break;
      case IotSvgBehaviorType.widgetAction:
        break;
    }
  }
}

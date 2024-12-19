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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  ScadaSymbolProperty,
  scadaSymbolPropertyFieldClasses,
  scadaSymbolPropertyRowClasses,
  ScadaSymbolPropertyType,
  scadaSymbolPropertyTypes,
  scadaSymbolPropertyTypeTranslations
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { defaultPropertyValue } from '@home/pages/scada-symbol/metadata-components/scada-symbol-property-row.component';
import { ValueType } from '@shared/models/constants';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-scada-symbol-property-panel',
  templateUrl: './scada-symbol-property-panel.component.html',
  styleUrls: ['./scada-symbol-property-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolPropertyPanelComponent implements OnInit {

  ValueType = ValueType;

  ScadaSymbolPropertyType = ScadaSymbolPropertyType;

  scadaSymbolPropertyTypes = scadaSymbolPropertyTypes;
  scadaSymbolPropertyTypeTranslations = scadaSymbolPropertyTypeTranslations;

  scadaSymbolPropertyRowClasses = scadaSymbolPropertyRowClasses;

  scadaSymbolPropertyFieldClasses = scadaSymbolPropertyFieldClasses;

  @Input()
  isAdd = false;

  @Input()
  property: ScadaSymbolProperty;

  @Input()
  booleanPropertyIds: string[];

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent<ScadaSymbolPropertyPanelComponent>;

  @Output()
  propertySettingsApplied = new EventEmitter<ScadaSymbolProperty>();

  panelTitle: string;

  propertyFormGroup: UntypedFormGroup;

  private propertyType: ScadaSymbolPropertyType;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.panelTitle = this.isAdd ? 'scada.property.add-property' : 'scada.property.property-settings';
    this.propertyType = this.property.type;
    this.propertyFormGroup = this.fb.group(
      {
        id: [this.property.id, [Validators.required]],
        name: [this.property.name, [Validators.required]],
        type: [this.property.type, [Validators.required]],
        default: [this.property.default, []],
        required: [this.property.required, []],
        subLabel: [this.property.subLabel, []],
        divider: [this.property.divider, []],
        fieldSuffix: [this.property.fieldSuffix, []],
        disableOnProperty: [this.property.disableOnProperty, []],
        rowClass: [(this.property.rowClass || '').split(' '), []],
        fieldClass: [(this.property.fieldClass || '').split(' '), []],
        min: [this.property.min, []],
        max: [this.property.max, []],
        step: [this.property.step, [Validators.min(0)]]
      }
    );
    if (this.disabled) {
      this.propertyFormGroup.disable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('type').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators();
      });
      this.updateValidators();
    }
  }

  cancel() {
    this.popover?.hide();
  }

  applyPropertySettings() {
    const property = this.propertyFormGroup.getRawValue();
    property.rowClass = (property.rowClass || []).join(' ');
    property.fieldClass = (property.fieldClass || []).join(' ');
    this.propertySettingsApplied.emit(property);
  }

  private updateValidators() {
    const type: ScadaSymbolPropertyType = this.propertyFormGroup.get('type').value;
    if (type === ScadaSymbolPropertyType.number) {
      this.propertyFormGroup.get('min').enable({emitEvent: false});
      this.propertyFormGroup.get('max').enable({emitEvent: false});
      this.propertyFormGroup.get('step').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('min').disable({emitEvent: false});
      this.propertyFormGroup.get('max').disable({emitEvent: false});
      this.propertyFormGroup.get('step').disable({emitEvent: false});
    }
    if (this.propertyType !== type) {
      const defaultValue = defaultPropertyValue(type);
      this.propertyFormGroup.get('default').patchValue(defaultValue, {emitEvent: false});
      this.propertyType = type;
    }
  }
}

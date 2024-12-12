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
import { ValueType } from '@shared/models/constants';
import {
  FormProperty,
  formPropertyFieldClasses,
  formPropertyRowClasses,
  FormPropertyType,
  formPropertyTypes,
  formPropertyTypeTranslations,
  FormSelectItem
} from '@shared/models/dynamic-form.models';
import {
  defaultPropertyValue
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-property-row.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-dynamic-form-property-panel',
  templateUrl: './dynamic-form-property-panel.component.html',
  styleUrls: ['./dynamic-form-property-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DynamicFormPropertyPanelComponent implements OnInit {

  ValueType = ValueType;

  FormPropertyType = FormPropertyType;

  formPropertyTypes = formPropertyTypes;
  formPropertyTypeTranslations = formPropertyTypeTranslations;

  formPropertyRowClasses = formPropertyRowClasses;

  formPropertyFieldClasses = formPropertyFieldClasses;

  @Input()
  isAdd = false;

  @Input()
  property: FormProperty;

  @Input()
  booleanPropertyIds: string[];

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent<DynamicFormPropertyPanelComponent>;

  @Output()
  propertySettingsApplied = new EventEmitter<FormProperty>();

  panelTitle: string;

  propertyFormGroup: UntypedFormGroup;

  private propertyType: FormPropertyType;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.panelTitle = this.isAdd ? 'dynamic-form.property.add-property' : 'dynamic-form.property.property-settings';
    this.propertyType = this.property.type;
    this.propertyFormGroup = this.fb.group(
      {
        id: [this.property.id, [Validators.required]],
        name: [this.property.name, [Validators.required]],
        group: [this.property.group, []],
        type: [this.property.type, [Validators.required]],
        default: [this.property.default, []],
        required: [this.property.required, []],
        subLabel: [this.property.subLabel, []],
        divider: [this.property.divider, []],
        fieldSuffix: [this.property.fieldSuffix, []],
        disableOnProperty: [this.property.disableOnProperty, []],
        condition: [this.property.condition, []],
        rowClass: [(this.property.rowClass || '').split(' '), []],
        fieldClass: [(this.property.fieldClass || '').split(' '), []],
        min: [this.property.min, []],
        max: [this.property.max, []],
        step: [this.property.step, [Validators.min(0)]],
        properties: [this.property.properties, []],
        multiple: [this.property.multiple, []],
        items: [this.property.items, []]
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

      this.propertyFormGroup.get('items').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.onSelectItemsChange();
      });
      this.propertyFormGroup.get('multiple').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.onMultipleSelectChange();
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
    const type: FormPropertyType = this.propertyFormGroup.get('type').value;
    if (type === FormPropertyType.number) {
      this.propertyFormGroup.get('min').enable({emitEvent: false});
      this.propertyFormGroup.get('max').enable({emitEvent: false});
      this.propertyFormGroup.get('step').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('min').disable({emitEvent: false});
      this.propertyFormGroup.get('max').disable({emitEvent: false});
      this.propertyFormGroup.get('step').disable({emitEvent: false});
    }
    if (type === FormPropertyType.fieldset) {
      this.propertyFormGroup.get('default').disable({emitEvent: false});
      this.propertyFormGroup.get('properties').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('default').enable({emitEvent: false});
      this.propertyFormGroup.get('properties').disable({emitEvent: false});
    }
    if (type === FormPropertyType.select) {
      this.propertyFormGroup.get('multiple').enable({emitEvent: false});
      this.propertyFormGroup.get('items').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('multiple').disable({emitEvent: false});
      this.propertyFormGroup.get('items').disable({emitEvent: false});
    }
    if (this.propertyType !== type) {
      const defaultValue = defaultPropertyValue(type);
      this.propertyFormGroup.get('default').patchValue(defaultValue, {emitEvent: false});
      this.propertyType = type;
    }
  }

  private onSelectItemsChange() {
    const type: FormPropertyType = this.propertyFormGroup.get('type').value;
    const multiple: boolean = this.propertyFormGroup.get('multiple').value;
    const defaultValue: any = this.propertyFormGroup.get('default').value;
    const items: FormSelectItem[] = this.propertyFormGroup.get('items').value;
    if (defaultValue && type === FormPropertyType.select) {
      if (multiple) {
        let targetValue: any[] = defaultValue;
        targetValue = targetValue.filter(valItem => !!items.find(item => item.value === valItem));
        this.propertyFormGroup.get('default').patchValue(targetValue, {emitEvent: false});
      } else {
        if (!items.find(item => item.value === defaultValue)) {
          this.propertyFormGroup.get('default').patchValue(null, {emitEvent: false});
        }
      }
    }
  }

  private onMultipleSelectChange() {
    const type: FormPropertyType = this.propertyFormGroup.get('type').value;
    const multiple: boolean = this.propertyFormGroup.get('multiple').value;
    const defaultValue: any = this.propertyFormGroup.get('default').value;
    if (type === FormPropertyType.select) {
      if (multiple) {
        if (defaultValue && !Array.isArray(defaultValue)) {
          const newVal = [defaultValue];
          this.propertyFormGroup.get('default').patchValue(newVal, {emitEvent: false});
        }
      } else {
        if (defaultValue && Array.isArray(defaultValue)) {
          const newVal = defaultValue.length ? defaultValue[0] : null;
          setTimeout(() => {
            this.propertyFormGroup.get('default').patchValue(newVal, {emitEvent: false});
          });
        }
      }
    }
  }
}

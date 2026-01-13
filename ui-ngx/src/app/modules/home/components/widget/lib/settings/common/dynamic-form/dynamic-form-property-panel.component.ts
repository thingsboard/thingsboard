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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ContentType, ValueType } from '@shared/models/constants';
import {
  defaultPropertyValue,
  FormProperty,
  formPropertyFieldClasses,
  formPropertyRowClasses,
  FormPropertyType,
  formPropertyTypes,
  formPropertyTypeTranslations,
  FormSelectItem,
  isPropertyTypeAllowedForRow
} from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isUndefinedOrNull } from '@core/utils';
import { StringItemsOption } from '@shared/components/string-items-list.component';

@Component({
  selector: 'tb-dynamic-form-property-panel',
  templateUrl: './dynamic-form-property-panel.component.html',
  styleUrls: ['./dynamic-form-property-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DynamicFormPropertyPanelComponent implements OnInit {

  ValueType = ValueType;

  FormPropertyType = FormPropertyType;

  ContentType = ContentType;

  formPropertyTypes = formPropertyTypes;
  arrayItemFormPropertyTypes = formPropertyTypes.filter(t => t !== FormPropertyType.array);
  formPropertyTypeTranslations = formPropertyTypeTranslations;

  formPropertyRowClasses: StringItemsOption[] = formPropertyRowClasses.map(clazz => ({name: clazz, value: clazz}));

  formPropertyFieldClasses: StringItemsOption[] = formPropertyFieldClasses.map(clazz => ({name: clazz, value: clazz}));

  isPropertyTypeAllowedForRow = isPropertyTypeAllowedForRow;

  get propertyItemType(): FormPropertyType | any {
    if (this.isArray) {
      return this.propertyFormGroup.get('arrayItemType').value;
    } else {
      return this.propertyFormGroup.get('type').value;
    }
  }

  get isArray(): boolean {
    return this.propertyFormGroup.get('type').value === FormPropertyType.array;
  }

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
    this.propertyType = this.property.type === FormPropertyType.array ? this.property.arrayItemType : this.property.type;
    this.propertyFormGroup = this.fb.group(
      {
        id: [this.property.id, [Validators.required]],
        name: [this.property.name, [Validators.required]],
        hint: [this.property.hint, []],
        group: [this.property.group, []],
        type: [this.property.type, [Validators.required]],
        arrayItemType: [this.property.arrayItemType, [Validators.required]],
        arrayItemName: [this.property.arrayItemName, []],
        default: [this.property.default, []],
        required: [this.property.required, []],
        subLabel: [this.property.subLabel, []],
        divider: [this.property.divider, []],
        fieldSuffix: [this.property.fieldSuffix, []],
        disableOnProperty: [this.property.disableOnProperty, []],
        condition: [this.property.condition, []],
        rowClass: [this.property.rowClass ? this.property.rowClass.split(' ') : [], []],
        fieldClass: [this.property.fieldClass ? this.property.fieldClass.split(' ') : [], []],
        rows: [this.property.rows, [Validators.min(1)]],
        min: [this.property.min, []],
        max: [this.property.max, []],
        step: [this.property.step, [Validators.min(0)]],
        properties: [this.property.properties, []],
        multiple: [this.property.multiple, []],
        allowEmptyOption: [this.property.allowEmptyOption, []],
        minItems: [this.property.minItems, []],
        maxItems: [this.property.maxItems, []],
        items: [this.property.items, []],
        helpId: [this.property.helpId, []],
        direction: [this.property.direction || 'column', []],
        allowClear: [this.property.allowClear || true, []],
        dateTimeType: [this.property.dateTimeType || 'datetime', []],
        htmlContent: [this.property.htmlContent || '', []],
        htmlClassList: [this.property.htmlClassList || [], []],
        supportsUnitConversion: [this.property.supportsUnitConversion ?? false]
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
      this.propertyFormGroup.get('arrayItemType').valueChanges.pipe(
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
    if (this.isArray) {
      this.propertyFormGroup.get('arrayItemType').enable({emitEvent: false});
      this.propertyFormGroup.get('arrayItemName').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('arrayItemType').disable({emitEvent: false});
      this.propertyFormGroup.get('arrayItemName').disable({emitEvent: false});
    }
    const type = this.propertyItemType;
    if (type === FormPropertyType.textarea) {
      this.propertyFormGroup.get('rows').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('rows').disable({emitEvent: false});
    }
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
    if ([FormPropertyType.select, FormPropertyType.radios].includes(type)) {
      this.propertyFormGroup.get('items').enable({emitEvent: false});
      if (type === FormPropertyType.select) {
        this.propertyFormGroup.get('multiple').enable({emitEvent: false});
        const multiple: boolean = this.propertyFormGroup.get('multiple').value;
        if (multiple) {
          this.propertyFormGroup.get('allowEmptyOption').disable({emitEvent: false});
          this.propertyFormGroup.get('minItems').enable({emitEvent: false});
          this.propertyFormGroup.get('maxItems').enable({emitEvent: false});
        } else {
          this.propertyFormGroup.get('allowEmptyOption').enable({emitEvent: false});
          this.propertyFormGroup.get('minItems').disable({emitEvent: false});
          this.propertyFormGroup.get('maxItems').disable({emitEvent: false});
        }
      }
    } else {
      this.propertyFormGroup.get('multiple').disable({emitEvent: false});
      this.propertyFormGroup.get('allowEmptyOption').disable({emitEvent: false});
      this.propertyFormGroup.get('minItems').disable({emitEvent: false});
      this.propertyFormGroup.get('maxItems').disable({emitEvent: false});
      this.propertyFormGroup.get('items').disable({emitEvent: false});
    }
    if (type === FormPropertyType.datetime) {
      this.propertyFormGroup.get('allowClear').enable({emitEvent: false});
      this.propertyFormGroup.get('dateTimeType').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('allowClear').disable({emitEvent: false});
      this.propertyFormGroup.get('dateTimeType').disable({emitEvent: false});
    }
    if (type === FormPropertyType.htmlSection) {
      this.propertyFormGroup.get('htmlContent').enable({emitEvent: false});
      this.propertyFormGroup.get('htmlClassList').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('htmlContent').disable({emitEvent: false});
      this.propertyFormGroup.get('htmlClassList').disable({emitEvent: false});
    }
    if ([FormPropertyType.javascript, FormPropertyType.markdown].includes(type)) {
      this.propertyFormGroup.get('helpId').enable({emitEvent: false});
    } else {
      this.propertyFormGroup.get('helpId').disable({emitEvent: false});
    }
    if (this.propertyType !== type) {
      const defaultValue = defaultPropertyValue(type);
      this.propertyFormGroup.get('default').patchValue(defaultValue, {emitEvent: false});
      this.propertyType = type;
      if (type === FormPropertyType.textarea) {
        if (isUndefinedOrNull(this.propertyFormGroup.get('rows').value)) {
          this.propertyFormGroup.get('rows').patchValue(2, {emitEvent: false});
        }
      }
      if (type === FormPropertyType.radios) {
        if (isUndefinedOrNull(this.propertyFormGroup.get('direction').value)) {
          this.propertyFormGroup.get('direction').patchValue('column', {emitEvent: false});
        }
      }
      if (type === FormPropertyType.datetime) {
        if (isUndefinedOrNull(this.propertyFormGroup.get('dateTimeType').value)) {
          this.propertyFormGroup.get('dateTimeType').patchValue('datetime', {emitEvent: false});
        }
        if (isUndefinedOrNull(this.propertyFormGroup.get('allowClear').value)) {
          this.propertyFormGroup.get('allowClear').patchValue(true, {emitEvent: false});
        }
      }
    }
  }

  private onSelectItemsChange() {
    const type = this.propertyItemType;
    const multiple: boolean = this.propertyFormGroup.get('multiple').value;
    const defaultValue: any = this.propertyFormGroup.get('default').value;
    const items: FormSelectItem[] = this.propertyFormGroup.get('items').value;
    if (defaultValue && [FormPropertyType.select, FormPropertyType.radios].includes(type)) {
      if (multiple && FormPropertyType.select === type) {
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
    const type = this.propertyItemType;
    const multiple: boolean = this.propertyFormGroup.get('multiple').value;
    const defaultValue: any = this.propertyFormGroup.get('default').value;
    if (type === FormPropertyType.select) {
      if (multiple) {
        if (defaultValue && !Array.isArray(defaultValue)) {
          const newVal = [defaultValue];
          this.propertyFormGroup.get('default').patchValue(newVal, {emitEvent: false});
        }
        this.propertyFormGroup.get('allowEmptyOption').patchValue(false, {emitEvent: false});
        this.propertyFormGroup.get('allowEmptyOption').disable({emitEvent: false})
        this.propertyFormGroup.get('minItems').enable({emitEvent: false});
        this.propertyFormGroup.get('maxItems').enable({emitEvent: false});
      } else {
        if (defaultValue && Array.isArray(defaultValue)) {
          const newVal = defaultValue.length ? defaultValue[0] : null;
          setTimeout(() => {
            this.propertyFormGroup.get('default').patchValue(newVal, {emitEvent: false});
          });
        }
        this.propertyFormGroup.get('allowEmptyOption').enable({emitEvent: false});
        this.propertyFormGroup.get('minItems').disable({emitEvent: false});
        this.propertyFormGroup.get('maxItems').disable({emitEvent: false});
      }
    }
  }
}

///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  DataKeySelectOption,
  dataKeySelectOptionValidator
} from '@home/components/widget/lib/settings/input/datakey-select-option.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-update-multiple-attributes-key-settings',
  templateUrl: './update-multiple-attributes-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateMultipleAttributesKeySettingsComponent extends WidgetSettingsComponent {

  updateMultipleAttributesKeySettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.updateMultipleAttributesKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      dataKeyHidden: false,
      dataKeyType: 'server',
      dataKeyValueType: 'string',
      required: false,
      isEditable: 'editable',
      disabledOnDataKey: '',

      slideToggleLabelPosition: 'after',
      selectOptions: [],
      step: 1,
      minValue: null,
      maxValue: null,

      requiredErrorMessage: '',
      minValueErrorMessage: '',
      maxValueErrorMessage: '',
      invalidDateErrorMessage: '',

      useCustomIcon: false,
      icon: '',
      customIcon: '',

      useGetValueFunction: false,
      getValueFunctionBody: '',
      useSetValueFunction: false,
      setValueFunctionBody: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateMultipleAttributesKeySettingsForm = this.fb.group({

      dataKeyHidden: [settings.dataKeyHidden, []],

      // General settings

      dataKeyType: [settings.dataKeyType, []],
      dataKeyValueType: [settings.dataKeyValueType, []],
      required: [settings.required, []],
      isEditable: [settings.isEditable, []],
      disabledOnDataKey: [settings.disabledOnDataKey, []],

      // Slide toggle settings

      slideToggleLabelPosition: [settings.slideToggleLabelPosition, []],

      // Select options

      selectOptions: this.prepareSelectOptionsFormArray(settings.selectOptions),

      // Numeric field settings

      step: [settings.step, [Validators.min(0)]],
      minValue: [settings.minValue, []],
      maxValue: [settings.maxValue, []],

      // Error messages

      requiredErrorMessage: [settings.requiredErrorMessage, []],
      minValueErrorMessage: [settings.minValueErrorMessage, []],
      maxValueErrorMessage: [settings.maxValueErrorMessage, []],
      invalidDateErrorMessage: [settings.invalidDateErrorMessage, []],

      // Icon settings

      useCustomIcon: [settings.useCustomIcon, []],
      icon: [settings.icon, []],
      customIcon: [settings.customIcon, []],

      // Value conversion settings

      useGetValueFunction: [settings.useGetValueFunction, []],
      getValueFunctionBody: [settings.getValueFunctionBody, []],
      useSetValueFunction: [settings.useSetValueFunction, []],
      setValueFunctionBody: [settings.setValueFunctionBody, []]

    });
  }

  protected validatorTriggers(): string[] {
    return ['dataKeyHidden', 'dataKeyValueType', 'required', 'isEditable', 'useCustomIcon', 'useGetValueFunction', 'useSetValueFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const dataKeyHidden: boolean = this.updateMultipleAttributesKeySettingsForm.get('dataKeyHidden').value;
    const dataKeyValueType: string = this.updateMultipleAttributesKeySettingsForm.get('dataKeyValueType').value;
    const required: boolean = this.updateMultipleAttributesKeySettingsForm.get('required').value;
    const isEditable: string = this.updateMultipleAttributesKeySettingsForm.get('isEditable').value;
    const useCustomIcon: boolean = this.updateMultipleAttributesKeySettingsForm.get('useCustomIcon').value;
    const useGetValueFunction: boolean = this.updateMultipleAttributesKeySettingsForm.get('useGetValueFunction').value;
    const useSetValueFunction: boolean = this.updateMultipleAttributesKeySettingsForm.get('useSetValueFunction').value;

    this.updateMultipleAttributesKeySettingsForm.disable({emitEvent: false});
    this.updateMultipleAttributesKeySettingsForm.get('dataKeyHidden').enable({emitEvent: false});

    if (!dataKeyHidden) {
      this.updateMultipleAttributesKeySettingsForm.get('dataKeyType').enable({emitEvent: false});
      this.updateMultipleAttributesKeySettingsForm.get('dataKeyValueType').enable({emitEvent: false});
      this.updateMultipleAttributesKeySettingsForm.get('required').enable({emitEvent: false});
      this.updateMultipleAttributesKeySettingsForm.get('isEditable').enable({emitEvent: false});
      this.updateMultipleAttributesKeySettingsForm.get('useCustomIcon').enable({emitEvent: false});
      this.updateMultipleAttributesKeySettingsForm.get('useGetValueFunction').enable({emitEvent: false});
      this.updateMultipleAttributesKeySettingsForm.get('useSetValueFunction').enable({emitEvent: false});

      if (isEditable !== 'disabled') {
        this.updateMultipleAttributesKeySettingsForm.get('disabledOnDataKey').enable({emitEvent: false});
      }

      if (dataKeyValueType === 'booleanSwitch') {
        this.updateMultipleAttributesKeySettingsForm.get('slideToggleLabelPosition').enable({emitEvent: false});
      } else if (dataKeyValueType === 'select') {
        this.updateMultipleAttributesKeySettingsForm.get('selectOptions').enable({emitEvent: false});
      } else if (dataKeyValueType === 'integer' || dataKeyValueType === 'double') {
        this.updateMultipleAttributesKeySettingsForm.get('step').enable({emitEvent: false});
        this.updateMultipleAttributesKeySettingsForm.get('minValue').enable({emitEvent: false});
        this.updateMultipleAttributesKeySettingsForm.get('maxValue').enable({emitEvent: false});
        this.updateMultipleAttributesKeySettingsForm.get('minValueErrorMessage').enable({emitEvent: false});
        this.updateMultipleAttributesKeySettingsForm.get('maxValueErrorMessage').enable({emitEvent: false});
      } else if (dataKeyValueType === 'dateTime' || dataKeyValueType === 'date' || dataKeyValueType === 'time') {
        this.updateMultipleAttributesKeySettingsForm.get('invalidDateErrorMessage').enable({emitEvent: false});
      }
      if (required) {
        this.updateMultipleAttributesKeySettingsForm.get('requiredErrorMessage').enable({emitEvent: false});
      }
      if (useCustomIcon) {
        this.updateMultipleAttributesKeySettingsForm.get('customIcon').enable({emitEvent: false});
      } else {
        this.updateMultipleAttributesKeySettingsForm.get('icon').enable({emitEvent: false});
      }
      if (useGetValueFunction) {
        this.updateMultipleAttributesKeySettingsForm.get('getValueFunctionBody').enable({emitEvent: false});
      }
      if (useSetValueFunction) {
        this.updateMultipleAttributesKeySettingsForm.get('setValueFunctionBody').enable({emitEvent: false});
      }
    }
    this.updateMultipleAttributesKeySettingsForm.updateValueAndValidity({emitEvent: false});
  }

  protected doUpdateSettings(settingsForm: FormGroup, settings: WidgetSettings) {
    settingsForm.setControl('selectOptions', this.prepareSelectOptionsFormArray(settings.selectOptions), {emitEvent: false});
  }

  private prepareSelectOptionsFormArray(selectOptions: DataKeySelectOption[] | undefined): FormArray {
    const selectOptionsControls: Array<AbstractControl> = [];
    if (selectOptions) {
      selectOptions.forEach((selectOption) => {
        selectOptionsControls.push(this.fb.control(selectOption, [dataKeySelectOptionValidator]));
      });
    }
    return this.fb.array(selectOptionsControls, [(control: AbstractControl) => {
      const selectOptionItems = control.value;
      if (!selectOptionItems || !selectOptionItems.length) {
        return {
          selectOptionItems: true
        };
      }
      return null;
    }]);
  }

  selectOptionsFormArray(): FormArray {
    return this.updateMultipleAttributesKeySettingsForm.get('selectOptions') as FormArray;
  }

  public trackBySelectOption(index: number, selectOptionControl: AbstractControl): any {
    return selectOptionControl;
  }

  public removeSelectOption(index: number) {
    (this.updateMultipleAttributesKeySettingsForm.get('selectOptions') as FormArray).removeAt(index);
  }

  public addSelectOption() {
    const selectOption: DataKeySelectOption = {
      value: null,
      label: null
    };
    const selectOptionsArray = this.updateMultipleAttributesKeySettingsForm.get('selectOptions') as FormArray;
    const selectOptionControl = this.fb.control(selectOption, [dataKeySelectOptionValidator]);
    (selectOptionControl as any).new = true;
    selectOptionsArray.push(selectOptionControl);
    this.updateMultipleAttributesKeySettingsForm.updateValueAndValidity();
    if (!this.updateMultipleAttributesKeySettingsForm.valid) {
      this.onSettingsChanged(this.updateMultipleAttributesKeySettingsForm.value);
    }
  }

  selectOptionDrop(event: CdkDragDrop<any[]>) {
    const selectOptionsArray = this.updateMultipleAttributesKeySettingsForm.get('selectOptions') as FormArray;
    const selectOption = selectOptionsArray.at(event.previousIndex);
    selectOptionsArray.removeAt(event.previousIndex);
    selectOptionsArray.insert(event.currentIndex, selectOption);
  }

  displayErrorMessagesSection(): boolean {
    const dataKeyHidden: boolean = this.updateMultipleAttributesKeySettingsForm.get('dataKeyHidden').value;
    const required: boolean = this.updateMultipleAttributesKeySettingsForm.get('required').value;
    const dataKeyValueType: string = this.updateMultipleAttributesKeySettingsForm.get('dataKeyValueType').value;
    return !dataKeyHidden && (required || (['integer', 'double', 'dateTime', 'date', 'time'].includes(dataKeyValueType)));
  }
}


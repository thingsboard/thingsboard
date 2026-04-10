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
import { PageComponent } from '@shared/components/page.component';
import {
  ColorSettings,
  ColorType,
  colorTypeTranslations,
  defaultGradient,
  defaultRange
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import { WidgetService } from '@core/http/widget.service';
import { ColorSettingsComponent } from '@home/components/widget/lib/settings/common/color-settings.component';
import { IAliasController } from '@core/api/widget-api.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-color-settings-panel',
    templateUrl: './color-settings-panel.component.html',
    providers: [],
    styleUrls: ['./color-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ColorSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  colorSettings: ColorSettings;

  @Input()
  popover: TbPopoverComponent<ColorSettingsPanelComponent>;

  @Input()
  settingsComponents: ColorSettingsComponent[];

  @Output()
  colorSettingsApplied = new EventEmitter<ColorSettings>();

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  @coerceBoolean()
  rangeAdvancedMode = false;

  @Input()
  @coerceBoolean()
  gradientAdvancedMode = false;

  @Input()
  minValue: number;

  @Input()
  maxValue: number;

  colorType = ColorType;

  colorTypes = Object.keys(ColorType) as ColorType[];

  colorTypeTranslationsMap = colorTypeTranslations;

  colorSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.colorSettingsFormGroup = this.fb.group(
      {
        type: [this.colorSettings?.type || ColorType.constant, []],
        color: [this.colorSettings?.color, []],
        gradient: [this.colorSettings?.gradient || defaultGradient(this.minValue, this.maxValue), []],
        rangeList: [this.colorSettings?.rangeList || defaultRange(), []],
        colorFunction: [this.colorSettings?.colorFunction, []]
      }
    );
    this.colorSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.updateValidators();
  }

  updateValidators() {
    const type: ColorType = this.colorSettingsFormGroup.get('type').value;
    this.colorSettingsFormGroup.get('gradient').disable({emitEvent: false});
    this.colorSettingsFormGroup.get('rangeList').disable({emitEvent: false});
    this.colorSettingsFormGroup.get('colorFunction').disable({emitEvent: false});
    switch (type) {
      case ColorType.gradient:
        this.colorSettingsFormGroup.get('gradient').enable({emitEvent: false});
        break;
      case ColorType.range:
        this.colorSettingsFormGroup.get('rangeList').enable({emitEvent: false});
        break;
      case ColorType.function:
        this.colorSettingsFormGroup.get('colorFunction').enable({emitEvent: false});
        break;
    }
  }

  copyColorSettings(comp: ColorSettingsComponent) {
    this.colorSettings = deepClone(comp.modelValue);
    this.colorSettingsFormGroup.patchValue({
      type: this.colorSettings.type,
      color: this.colorSettings.color,
      gradient: this.colorSettings.gradient || null,
      colorFunction: this.colorSettings.colorFunction,
      rangeList: this.colorSettings.rangeList || null
    }, {emitEvent: false});
    this.colorSettingsFormGroup.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const colorSettings: ColorSettings = this.colorSettingsFormGroup.getRawValue();
    this.colorSettingsApplied.emit(colorSettings);
  }

}

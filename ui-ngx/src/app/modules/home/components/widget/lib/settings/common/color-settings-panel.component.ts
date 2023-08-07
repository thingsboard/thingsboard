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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  ColorRange,
  ColorSettings,
  ColorType,
  colorTypeTranslations
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Datasource, DatasourceType } from '@shared/models/widget.models';
import { deepClone } from '@core/utils';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-color-settings-panel',
  templateUrl: './color-settings-panel.component.html',
  providers: [],
  styleUrls: ['./color-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ColorSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  colorSettings: ColorSettings;

  @Input()
  popover: TbPopoverComponent<ColorSettingsPanelComponent>;

  @Output()
  colorSettingsApplied = new EventEmitter<ColorSettings>();

  colorType = ColorType;

  colorTypes = Object.keys(ColorType) as ColorType[];

  colorTypeTranslationsMap = colorTypeTranslations;

  colorSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.colorSettingsFormGroup = this.fb.group(
      {
        type: [this.colorSettings?.type, []],
        color: [this.colorSettings?.color, []],
        rangeList: this.fb.array((this.colorSettings?.rangeList || []).map(r => this.colorRangeControl(r))),
        colorFunction: [this.colorSettings?.colorFunction, []]
      }
    );
    this.colorSettingsFormGroup.get('type').valueChanges.subscribe(() => {
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
  }

  private colorRangeControl(range: ColorRange): AbstractControl {
    return this.fb.group({
      from: [range?.from, []],
      to: [range?.to, []],
      color: [range?.color, []]
    });
  }

  get rangeListFormArray(): UntypedFormArray {
    return this.colorSettingsFormGroup.get('rangeList') as UntypedFormArray;
  }

  get rangeListFormGroups(): FormGroup[] {
    return this.rangeListFormArray.controls as FormGroup[];
  }

  trackByRange(index: number, rangeControl: AbstractControl): any {
    return rangeControl;
  }

  removeRange(index: number) {
    this.rangeListFormArray.removeAt(index);
    this.colorSettingsFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  addRange() {
    const newRange: ColorRange = {
      color: 'rgba(0,0,0,0.87)'
    };
    this.rangeListFormArray.push(this.colorRangeControl(newRange));
    this.colorSettingsFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const colorSettings = this.colorSettingsFormGroup.value;
    this.colorSettingsApplied.emit(colorSettings);
  }

}

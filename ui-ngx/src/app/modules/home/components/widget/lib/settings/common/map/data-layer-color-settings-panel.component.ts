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
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataLayerColorSettings, DataLayerColorType } from '@home/components/widget/lib/maps/map.models';

@Component({
  selector: 'tb-data-layer-color-settings-panel',
  templateUrl: './data-layer-color-settings-panel.component.html',
  providers: [],
  styleUrls: ['./data-layer-color-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DataLayerColorSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  colorSettings: DataLayerColorSettings;

  @Input()
  popover: TbPopoverComponent<DataLayerColorSettingsPanelComponent>;

  @Output()
  colorSettingsApplied = new EventEmitter<DataLayerColorSettings>();

  DataLayerColorType = DataLayerColorType;

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
        type: [this.colorSettings?.type || DataLayerColorType.constant, []],
        color: [this.colorSettings?.color, []],
        colorFunction: [this.colorSettings?.colorFunction, []]
      }
    );
    this.colorSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const colorSettings: DataLayerColorSettings = this.colorSettingsFormGroup.value;
    this.colorSettingsApplied.emit(colorSettings);
  }

}

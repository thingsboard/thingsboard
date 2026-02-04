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
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataLayerColorSettings, DataLayerColorType, MapType } from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
    selector: 'tb-data-layer-color-settings-panel',
    templateUrl: './data-layer-color-settings-panel.component.html',
    providers: [],
    styleUrls: ['./data-layer-color-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DataLayerColorSettingsPanelComponent extends PageComponent implements OnInit {

  widgetType = widgetType;

  DataKeyType = DataKeyType;

  @Input()
  colorSettings: DataLayerColorSettings;

  @Input()
  context: MapSettingsContext;

  @Input()
  dsType: DatasourceType;

  @Input()
  dsEntityAliasId: string;

  @Input()
  dsDeviceId: string;

  @Input()
  helpId = 'widget/lib/map/color_fn';

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
        rangeKey: [this.colorSettings?.rangeKey, [Validators.required]],
        range: [this.colorSettings?.range, []],
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

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const colorSettings: DataLayerColorSettings = this.colorSettingsFormGroup.getRawValue();
    this.colorSettingsApplied.emit(colorSettings);
  }

  public editRangeKey() {
    const targetDataKey: DataKey = this.colorSettingsFormGroup.get('rangeKey').value;
    this.context.editKey(targetDataKey,
      this.dsDeviceId, this.dsEntityAliasId, widgetType.latest).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.colorSettingsFormGroup.get('rangeKey').patchValue(updatedDataKey);
          this.colorSettingsFormGroup.markAsDirty();
        }
      }
    );
  }

  private updateValidators() {
    const type: DataLayerColorType = this.colorSettingsFormGroup.get('type').value;
    if (type === DataLayerColorType.range) {
      this.colorSettingsFormGroup.get('rangeKey').enable({emitEvent: false});
      this.colorSettingsFormGroup.get('range').enable({emitEvent: false});
    } else {
      this.colorSettingsFormGroup.get('rangeKey').disable({emitEvent: false});
      this.colorSettingsFormGroup.get('range').disable({emitEvent: false});
    }
    if (type === DataLayerColorType.function) {
      this.colorSettingsFormGroup.get('colorFunction').enable({emitEvent: false});
    } else {
      this.colorSettingsFormGroup.get('colorFunction').disable({emitEvent: false});
    }
  }
}

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

import { Component, forwardRef } from '@angular/core';
import {
  DataKey,
  DataKeyConfigMode,
  WidgetSettings,
  WidgetSettingsComponent,
  widgetType
} from '@shared/models/widget.models';
import {
  AbstractControl,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn, Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  ApiUsageDataKeysSettings,
  apiUsageDefaultSettings,
  ApiUsageSettingsContext
} from '@home/components/widget/lib/settings/cards/api-usage-settings.component.models';
import { deepClone } from '@core/utils';
import { Observable, of } from 'rxjs';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-api-usage-widget-settings',
  templateUrl: './api-usage-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss', 'api-usage-widget-settings.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ApiUsageWidgetSettingsComponent),
      multi: true
    }
  ],
})
export class ApiUsageWidgetSettingsComponent extends WidgetSettingsComponent {

  apiUsageWidgetSettingsForm: UntypedFormGroup;

  context: ApiUsageSettingsContext;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.context = {
      aliasController: this.aliasController,
      callbacks: this.callbacks,
      widget: this.widget,
      editKey: this.editKey.bind(this),
      generateDataKey: this.generateDataKey.bind(this)
    };
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('apiUsageDataKeys', this.prepareDataKeysFormArray(settings?.apiUsageDataKeys), {emitEvent: false});
  }

  dataKeysFormArray(): UntypedFormArray {
    return this.apiUsageWidgetSettingsForm.get('apiUsageDataKeys') as UntypedFormArray;
  }

  trackByDataKey(index: number): any {
    return index;
  }

  get dragEnabled(): boolean {
    return this.dataKeysFormArray().controls.length > 1;
  }

  layerDrop(event: CdkDragDrop<string[]>) {
    const layer = this.dataKeysFormArray().at(event.previousIndex);
    this.dataKeysFormArray().removeAt(event.previousIndex);
    this.dataKeysFormArray().insert(event.currentIndex, layer);
  }

  removeDataKey(index: number) {
    (this.apiUsageWidgetSettingsForm.get('apiUsageDataKeys') as UntypedFormArray).removeAt(index);
  }

  addDataKey() {
    const dataKey = {
      label: '',
      state: '',
      status: null,
      maxLimit: null,
      current: null
    };
    const dataKeysArray = this.apiUsageWidgetSettingsForm.get('apiUsageDataKeys') as UntypedFormArray;
    const dataKeyControl = this.fb.control(dataKey, [this.apiUsageDataKeyValidator()]);
    dataKeysArray.push(dataKeyControl);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.apiUsageWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return apiUsageDefaultSettings;
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      dsEntityAliasId: settings?.dsEntityAliasId,
      apiUsageDataKeys: settings?.apiUsageDataKeys,
      targetDashboardState: settings?.targetDashboardState,
      background: settings?.background,
      padding: settings.padding
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.apiUsageWidgetSettingsForm = this.fb.group({
      dsEntityAliasId: [settings?.dsEntityAliasId, Validators.required],
      apiUsageDataKeys: this.prepareDataKeysFormArray(settings?.apiUsageDataKeys),
      targetDashboardState: [settings?.targetDashboardState],
      background: [settings?.background, []],
      padding: [settings.padding, []]
    });
  }

  private prepareDataKeysFormArray(dataKeys: ApiUsageDataKeysSettings[]): UntypedFormArray {
    const dataKeysControls: Array<AbstractControl> = [];
    if (dataKeys) {
      dataKeys.forEach((dataLayer) => {
        dataKeysControls.push(this.fb.control(dataLayer, [this.apiUsageDataKeyValidator()]));
      });
    }
    return this.fb.array(dataKeysControls);
  }

  protected validatorTriggers(): string[] {
    return [];
  }

  protected updateValidators() {
  }

  apiUsageDataKeyValidator = (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      const value: ApiUsageDataKeysSettings = control.value;
      if (!value?.label || !value?.current || !value?.maxLimit || !value?.status) {
        return {
          dataKey: true
        }
      }
      return null;
    };
  };

  private editKey(key: DataKey, entityAliasId: string, _widgetType = widgetType.latest): Observable<DataKey> {
    return this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(key),
          dataKeyConfigMode: DataKeyConfigMode.general,
          aliasController: this.aliasController,
          widgetType: _widgetType,
          entityAliasId,
          showPostProcessing: true,
          callbacks: this.callbacks,
          hideDataKeyColor: true,
          hideDataKeyDecimals: true,
          hideDataKeyUnits: true,
          hideDataKeyAggregation: true,
          widget: this.widget,
          dashboard: null,
          dataKeySettingsForm: null,
          dataKeySettingsDirective: null
        }
      }).afterClosed();
  }

  private generateDataKey(key: DataKey): DataKey {
    return this.callbacks.generateDataKey(key.name, key.type, null, false, null);
  }

  fetchDashboardStates(searchText?: string): Observable<Array<string>> {
    return of(this.callbacks.fetchDashboardStates(searchText));
  }
}

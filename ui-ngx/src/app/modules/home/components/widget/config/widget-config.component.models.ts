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

import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { DatasourceCallbacks } from '@home/components/widget/config/datasource.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { AfterViewInit, Directive, EventEmitter, Inject, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormGroup } from '@angular/forms';
import { DataKey, DatasourceType, KeyInfo, WidgetConfigMode } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isDefinedAndNotNull } from '@core/utils';

export type WidgetConfigCallbacks = DatasourceCallbacks & WidgetActionCallbacks;

export interface IBasicWidgetConfigComponent {
  isAdd: boolean;
  widgetConfig: WidgetConfigComponentData;
  widgetConfigChanged: Observable<WidgetConfigComponentData>;
  validateConfig(): boolean;

}

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class BasicWidgetConfigComponent extends PageComponent implements
  IBasicWidgetConfigComponent, OnInit, AfterViewInit {

  isAdd = false;

  basicMode = WidgetConfigMode.basic;

  widgetConfigValue: WidgetConfigComponentData;

  set widgetConfig(value: WidgetConfigComponentData) {
    this.widgetConfigValue = value;
    this.setupConfig(this.widgetConfigValue);
  }

  get widgetConfig(): WidgetConfigComponentData {
    return this.widgetConfigValue;
  }

  widgetConfigChangedEmitter = new EventEmitter<WidgetConfigComponentData>();
  widgetConfigChanged = this.widgetConfigChangedEmitter.asObservable();

  protected constructor(@Inject(Store) protected store: Store<AppState>,
                        protected widgetConfigComponent: WidgetConfigComponent) {
    super(store);
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (!this.validateConfig()) {
        this.onConfigChanged(this.prepareOutputConfig(this.configForm().getRawValue()));
      }
    }, 0);
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    if (this.isAdd) {
      this.setupDefaults(widgetConfig);
    }
    this.onConfigSet(widgetConfig);
    this.updateValidators(false);
    for (const trigger of this.validatorTriggers()) {
      const path = trigger.split('.');
      let control: AbstractControl = this.configForm();
      for (const part of path) {
        control = control.get(part);
      }
      control.valueChanges.subscribe(() => {
        this.updateValidators(true, trigger);
      });
    }
    this.configForm().valueChanges.subscribe(() => {
      this.onConfigChanged(this.prepareOutputConfig(this.configForm().getRawValue()));
    });
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {}

  protected updateValidators(emitEvent: boolean, trigger?: string) {
  }

  protected validatorTriggers(): string[] {
    return [];
  }

  protected onConfigChanged(widgetConfig: WidgetConfigComponentData) {
    this.widgetConfigValue = widgetConfig;
    this.widgetConfigChangedEmitter.emit(this.widgetConfigValue);
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    return config;
  }

  public validateConfig(): boolean {
    return this.configForm().valid;
  }

  protected setupDefaultDatasource(configData: WidgetConfigComponentData, keys?: DataKey[]) {
    let datasources = configData.config.datasources;
    if (!datasources || !datasources.length) {
      datasources = [
        {
          type: DatasourceType.device,
          dataKeys: []
        }
      ];
      configData.config.datasources = datasources;
    }
    let dataKeys = datasources[0].dataKeys;
    if (!dataKeys) {
      dataKeys = [];
      datasources[0].dataKeys = dataKeys;
    }
    if (keys && keys.length) {
      dataKeys.length = 0;
      keys.forEach(key => {
        const dataKey =
          this.widgetConfigComponent.widgetConfigCallbacks.generateDataKey(key.name, key.type, configData.dataKeySettingsSchema);
        if (key.label) {
          dataKey.label = key.label;
        }
        if (key.units) {
          dataKey.units = key.units;
        }
        if (isDefinedAndNotNull(key.decimals)) {
          dataKey.decimals = key.decimals;
        }
        dataKeys.push(dataKey);
      });
    }
  }

  protected abstract configForm(): UntypedFormGroup;

  protected abstract onConfigSet(configData: WidgetConfigComponentData);

}

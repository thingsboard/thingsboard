// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DatasourceType, WidgetConfig, widgetType, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isUndefined } from '@core/utils';
import { getAlarmFilterConfig, setAlarmFilterConfig } from '@shared/models/widget-settings.models';
import { UtilsService } from '@core/services/utils.service';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import {
  countDefaultSettings,
  CountWidgetSettings
} from '@home/components/widget/lib/count/count-widget.models';

@Component({
    selector: 'tb-alarm-count-basic-config',
    templateUrl: './alarm-count-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class AlarmCountBasicConfigComponent extends BasicWidgetConfigComponent {

  alarmSearchStatus = AlarmSearchStatus;

  alarmCountWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.alarmCountWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    let datasources = configData.config.datasources;
    if (!datasources || !datasources.length) {
      datasources = [{}];
      configData.config.datasources = datasources;
    }
    datasources[0].type = DatasourceType.alarmCount;
    datasources[0].alarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]};
    datasources[0].dataKeys = [this.utils.createKey({name: 'count'}, DataKeyType.count)];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: CountWidgetSettings = {...countDefaultSettings(true), ...(configData.config.settings || {})};
    this.alarmCountWidgetConfigForm = this.fb.group({
      alarmFilterConfig: [getAlarmFilterConfig(configData.config.datasources), []],
      datasources: [configData.config.datasources, []],

      settings: [settings, []],

      backgroundColor: [configData.config.backgroundColor, []],
      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.datasources = config.datasources;
    setAlarmFilterConfig(config.alarmFilterConfig, this.widgetConfig.config.datasources);

    this.widgetConfig.config.settings = {...(this.widgetConfig.config.settings || {}), ...config.settings};

    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }
}

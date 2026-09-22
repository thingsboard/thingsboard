// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import {
  TargetDevice,
  TargetDeviceType,
  targetDeviceValid,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityFilter, singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-led-indicator-widget-settings',
    templateUrl: './led-indicator-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class LedIndicatorWidgetSettingsComponent extends WidgetSettingsComponent implements OnInit {

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  dataKeyType = DataKeyType;

  entityFilter: EntityFilter;

  ledIndicatorWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    const targetDevice = this.targetDevice;
    if (!targetDeviceValid(targetDevice)) {
      return;
    }
    if (targetDevice.type === TargetDeviceType.device) {
      this.entityFilter = singleEntityFilterFromDeviceId(targetDevice.deviceId);
    } else {
      this.aliasController.getAliasInfo(targetDevice.entityAliasId).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(aliasInfo => this.entityFilter = aliasInfo.entityFilter);
    }
  }

  protected settingsForm(): UntypedFormGroup {
    return this.ledIndicatorWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      ledColor: 'green',
      initialValue: false,
      performCheckStatus: true,
      checkStatusMethod: 'checkStatus',
      retrieveValueMethod: 'attribute',
      valueAttribute: 'value',
      parseValueFunction: 'return data ? true : false;',
      requestTimeout: 500,
      requestPersistent: false,
      persistentPollingInterval: 5000
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.ledIndicatorWidgetSettingsForm = this.fb.group({

      // Common settings

      title: [settings.title, []],
      ledColor: [settings.ledColor, []],

      // Value settings

      initialValue: [settings.initialValue, []],

      // --> Check status settings

      performCheckStatus: [settings.performCheckStatus, []],
      retrieveValueMethod: [settings.retrieveValueMethod, []],
      checkStatusMethod: [settings.checkStatusMethod, [Validators.required]],
      valueAttribute: [settings.valueAttribute, [Validators.required]],
      parseValueFunction: [settings.parseValueFunction, []],

      // RPC settings

      requestTimeout: [settings.requestTimeout, [Validators.min(0)]],

      // --> Persistent RPC settings

      requestPersistent: [settings.requestPersistent, []],
      persistentPollingInterval: [settings.persistentPollingInterval, [Validators.min(1000)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['performCheckStatus', 'requestPersistent'];
  }

  protected updateValidators(emitEvent: boolean): void {
    const performCheckStatus: boolean = this.ledIndicatorWidgetSettingsForm.get('performCheckStatus').value;
    const requestPersistent: boolean = this.ledIndicatorWidgetSettingsForm.get('requestPersistent').value;
    if (performCheckStatus) {
      this.ledIndicatorWidgetSettingsForm.get('valueAttribute').disable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('checkStatusMethod').enable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestTimeout').enable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestPersistent').enable({emitEvent: false});
      if (requestPersistent) {
        this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').enable({emitEvent});
      } else {
        this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
      }
    } else {
      this.ledIndicatorWidgetSettingsForm.get('valueAttribute').enable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('checkStatusMethod').disable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestTimeout').disable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestPersistent').disable({emitEvent: false});
      this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
    }
    this.ledIndicatorWidgetSettingsForm.get('valueAttribute').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('checkStatusMethod').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('requestTimeout').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('requestPersistent').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}

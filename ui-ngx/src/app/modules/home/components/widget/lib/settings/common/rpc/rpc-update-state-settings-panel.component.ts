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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge } from 'rxjs';
import {
  RpcStateToParamsType,
  RpcUpdateStateAction,
  rpcUpdateStateActions,
  RpcUpdateStateSettings,
  rpcUpdateStateTranslations
} from '@shared/models/rpc-widget-settings.models';
import { ValueType } from '@shared/models/constants';
import { TargetDevice } from '@shared/models/widget.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslationsShort } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-rpc-update-state-settings-panel',
  templateUrl: './rpc-update-state-settings-panel.component.html',
  providers: [],
  styleUrls: ['./rpc-state-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RpcUpdateStateSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  panelTitle: string;

  @Input()
  updateState: RpcUpdateStateSettings;

  @Input()
  stateValueType: ValueType;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  popover: TbPopoverComponent<RpcUpdateStateSettingsPanelComponent>;

  @Output()
  updateStateSettingsApplied = new EventEmitter<RpcUpdateStateSettings>();

  rpcUpdateStateAction = RpcUpdateStateAction;

  rpcUpdateStateActions = rpcUpdateStateActions;

  rpcUpdateStateTranslationsMap = rpcUpdateStateTranslations;

  telemetryTypeTranslationsMap = telemetryTypeTranslationsShort;

  attributeScopes = [AttributeScope.SERVER_SCOPE, AttributeScope.SHARED_SCOPE];

  dataKeyType = DataKeyType;

  stateToParamsType = RpcStateToParamsType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  valueType = ValueType;

  updateStateSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.updateStateSettingsFormGroup = this.fb.group(
      {
        action: [this.updateState?.action, []],
        executeRpc: this.fb.group({
          method: [this.updateState?.executeRpc?.method, [Validators.required]],
          requestTimeout: [this.updateState?.executeRpc?.requestTimeout, [Validators.required, Validators.min(5000)]],
          requestPersistent: [this.updateState?.executeRpc?.requestPersistent, []],
          persistentPollingInterval: [this.updateState?.executeRpc?.persistentPollingInterval, [Validators.required, Validators.min(1000)]]
        }),
        setAttribute: this.fb.group({
          scope: [this.updateState?.setAttribute?.scope, []],
          key: [this.updateState?.setAttribute?.key, [Validators.required]],
        }),
        putTimeSeries: this.fb.group({
          key: [this.updateState?.putTimeSeries?.key, [Validators.required]],
        }),
        stateToParams: this.fb.group({
          type: [this.updateState?.stateToParams?.type, [Validators.required]],
          constantValue: [this.updateState?.stateToParams?.constantValue, [Validators.required]],
          stateToParamsFunction: [this.updateState?.stateToParams?.stateToParamsFunction, [Validators.required]],
        }),
      }
    );

    merge(this.updateStateSettingsFormGroup.get('action').valueChanges,
      this.updateStateSettingsFormGroup.get('stateToParams').get('type').valueChanges,
      this.updateStateSettingsFormGroup.get('executeRpc').get('requestPersistent').valueChanges).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyUpdateStateSettings() {
    const updateStateSettings: RpcUpdateStateSettings = this.updateStateSettingsFormGroup.getRawValue();
    this.updateStateSettingsApplied.emit(updateStateSettings);
  }

  private updateValidators() {
    const action: RpcUpdateStateAction = this.updateStateSettingsFormGroup.get('action').value;
    let stateToParamsType: RpcStateToParamsType = this.updateStateSettingsFormGroup.get('stateToParams').get('type').value;

    this.updateStateSettingsFormGroup.get('executeRpc').disable({emitEvent: false});
    this.updateStateSettingsFormGroup.get('setAttribute').disable({emitEvent: false});
    this.updateStateSettingsFormGroup.get('putTimeSeries').disable({emitEvent: false});
    switch (action) {
      case RpcUpdateStateAction.EXECUTE_RPC:
        this.updateStateSettingsFormGroup.get('executeRpc').enable({emitEvent: false});
        const requestPersistent: boolean = this.updateStateSettingsFormGroup.get('executeRpc').get('requestPersistent').value;
        if (requestPersistent) {
          this.updateStateSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').enable({emitEvent: false});
        } else {
          this.updateStateSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').disable({emitEvent: false});
        }
        break;
      case RpcUpdateStateAction.SET_ATTRIBUTE:
      case RpcUpdateStateAction.ADD_TIME_SERIES:
        if (stateToParamsType === RpcStateToParamsType.NONE) {
          stateToParamsType = RpcStateToParamsType.CONSTANT;
          this.updateStateSettingsFormGroup.get('stateToParams').get('type').patchValue(stateToParamsType, {emitEvent: false});
        }
        if (action === RpcUpdateStateAction.SET_ATTRIBUTE) {
          this.updateStateSettingsFormGroup.get('setAttribute').enable({emitEvent: false});
        } else {
          this.updateStateSettingsFormGroup.get('putTimeSeries').enable({emitEvent: false});
        }
        break;
    }
    switch (stateToParamsType) {
      case RpcStateToParamsType.CONSTANT:
        this.updateStateSettingsFormGroup.get('stateToParams').get('constantValue').enable({emitEvent: false});
        this.updateStateSettingsFormGroup.get('stateToParams').get('stateToParamsFunction').disable({emitEvent: false});
        break;
      case RpcStateToParamsType.FUNCTION:
        this.updateStateSettingsFormGroup.get('stateToParams').get('constantValue').disable({emitEvent: false});
        this.updateStateSettingsFormGroup.get('stateToParams').get('stateToParamsFunction').enable({emitEvent: false});
        break;
      case RpcStateToParamsType.NONE:
        this.updateStateSettingsFormGroup.get('stateToParams').get('constantValue').disable({emitEvent: false});
        this.updateStateSettingsFormGroup.get('stateToParams').get('stateToParamsFunction').disable({emitEvent: false});
        break;
    }
  }
}

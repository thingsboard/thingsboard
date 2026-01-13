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
import { merge } from 'rxjs';
import {
  SetValueAction,
  setValueActionsByWidgetType,
  setValueActionTranslations,
  SetValueSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslationsShort } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetService } from '@core/http/widget.service';
import { ValueType } from '@shared/models/constants';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-set-value-action-settings-panel',
  templateUrl: './set-value-action-settings-panel.component.html',
  providers: [],
  styleUrls: ['./action-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SetValueActionSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  panelTitle: string;

  @Input()
  valueType = ValueType.BOOLEAN;

  @Input()
  setValueSettings: SetValueSettings;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  @Input()
  popover: TbPopoverComponent<SetValueActionSettingsPanelComponent>;

  @Output()
  setValueSettingsApplied = new EventEmitter<SetValueSettings>();

  setValueAction = SetValueAction;

  setValueActions: SetValueAction[];

  setValueActionTranslationsMap = setValueActionTranslations;

  telemetryTypeTranslationsMap = telemetryTypeTranslationsShort;

  attributeScopes = [AttributeScope.SERVER_SCOPE, AttributeScope.SHARED_SCOPE];

  dataKeyType = DataKeyType;

  valueToDataType = ValueToDataType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  ValueType = ValueType;

  setValueSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.setValueActions = setValueActionsByWidgetType(this.widgetType);
    this.setValueSettingsFormGroup = this.fb.group(
      {
        action: [this.setValueSettings?.action, []],
        executeRpc: this.fb.group({
          method: [this.setValueSettings?.executeRpc?.method, [Validators.required]],
          requestTimeout: [this.setValueSettings?.executeRpc?.requestTimeout, [Validators.required, Validators.min(5000)]],
          requestPersistent: [this.setValueSettings?.executeRpc?.requestPersistent, []],
          persistentPollingInterval:
            [this.setValueSettings?.executeRpc?.persistentPollingInterval, [Validators.required, Validators.min(1000)]]
        }),
        setAttribute: this.fb.group({
          scope: [this.setValueSettings?.setAttribute?.scope, []],
          key: [this.setValueSettings?.setAttribute?.key, [Validators.required]],
        }),
        putTimeSeries: this.fb.group({
          key: [this.setValueSettings?.putTimeSeries?.key, [Validators.required]],
        }),
        valueToData: this.fb.group({
          type: [this.setValueSettings?.valueToData?.type, [Validators.required]],
          constantValue: [this.setValueSettings?.valueToData?.constantValue, [Validators.required]],
          valueToDataFunction: [this.setValueSettings?.valueToData?.valueToDataFunction, [Validators.required]],
        }),
      }
    );

    merge(this.setValueSettingsFormGroup.get('action').valueChanges,
      this.setValueSettingsFormGroup.get('valueToData').get('type').valueChanges,
      this.setValueSettingsFormGroup.get('executeRpc').get('requestPersistent').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applySetValueSettings() {
    const setValueSettings: SetValueSettings = this.setValueSettingsFormGroup.getRawValue();
    this.setValueSettingsApplied.emit(setValueSettings);
  }

  private updateValidators() {
    const action: SetValueAction = this.setValueSettingsFormGroup.get('action').value;
    let valueToDataType: ValueToDataType = this.setValueSettingsFormGroup.get('valueToData').get('type').value;

    this.setValueSettingsFormGroup.get('executeRpc').disable({emitEvent: false});
    this.setValueSettingsFormGroup.get('setAttribute').disable({emitEvent: false});
    this.setValueSettingsFormGroup.get('putTimeSeries').disable({emitEvent: false});
    switch (action) {
      case SetValueAction.EXECUTE_RPC:
        this.setValueSettingsFormGroup.get('executeRpc').enable({emitEvent: false});
        const requestPersistent: boolean = this.setValueSettingsFormGroup.get('executeRpc').get('requestPersistent').value;
        if (requestPersistent) {
          this.setValueSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').enable({emitEvent: false});
        } else {
          this.setValueSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').disable({emitEvent: false});
        }
        break;
      case SetValueAction.SET_ATTRIBUTE:
      case SetValueAction.ADD_TIME_SERIES:
        if (valueToDataType === ValueToDataType.NONE) {
          valueToDataType = ValueToDataType.CONSTANT;
          this.setValueSettingsFormGroup.get('valueToData').get('type').patchValue(valueToDataType, {emitEvent: false});
        }
        if (action === SetValueAction.SET_ATTRIBUTE) {
          this.setValueSettingsFormGroup.get('setAttribute').enable({emitEvent: false});
        } else {
          this.setValueSettingsFormGroup.get('putTimeSeries').enable({emitEvent: false});
        }
        break;
    }
    switch (valueToDataType) {
      case ValueToDataType.CONSTANT:
        this.setValueSettingsFormGroup.get('valueToData').get('constantValue').enable({emitEvent: false});
        this.setValueSettingsFormGroup.get('valueToData').get('valueToDataFunction').disable({emitEvent: false});
        break;
      case ValueToDataType.FUNCTION:
        this.setValueSettingsFormGroup.get('valueToData').get('constantValue').disable({emitEvent: false});
        this.setValueSettingsFormGroup.get('valueToData').get('valueToDataFunction').enable({emitEvent: false});
        break;
      case ValueToDataType.NONE:
        this.setValueSettingsFormGroup.get('valueToData').get('constantValue').disable({emitEvent: false});
        this.setValueSettingsFormGroup.get('valueToData').get('valueToDataFunction').disable({emitEvent: false});
        break;
    }
  }
}

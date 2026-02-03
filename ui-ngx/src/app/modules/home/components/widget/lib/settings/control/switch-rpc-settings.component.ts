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

import { Component, DestroyRef, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetService } from '@core/http/widget.service';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityService } from '@core/http/entity.service';
import { TargetDevice } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export declare type RpcRetrieveValueMethod = 'none' | 'rpc' | 'attribute' | 'timeseries';

export interface SwitchRpcSettings {
  initialValue: boolean;
  retrieveValueMethod: RpcRetrieveValueMethod;
  valueKey: string;
  getValueMethod: string;
  setValueMethod: string;
  parseValueFunction: string;
  convertValueFunction: string;
  requestTimeout: number;
  requestPersistent: boolean;
  persistentPollingInterval: number;
}

export const switchRpcDefaultSettings = (): SwitchRpcSettings => ({
    initialValue: false,
    retrieveValueMethod: 'rpc',
    valueKey: 'value',
    getValueMethod: 'getValue',
    parseValueFunction: 'return data ? true : false;',
    setValueMethod: 'setValue',
    convertValueFunction: 'return value;',
    requestTimeout: 500,
    requestPersistent: false,
    persistentPollingInterval: 5000
  });

@Component({
    selector: 'tb-switch-rpc-settings',
    templateUrl: './switch-rpc-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SwitchRpcSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => SwitchRpcSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class SwitchRpcSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @ViewChild('keyInput') keyInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  dataKeyType = DataKeyType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: SwitchRpcSettings;

  private propagateChange = null;

  public switchRpcSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private entityService: EntityService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.switchRpcSettingsFormGroup = this.fb.group({

      // Value settings

      initialValue: [false, []],

      // --> Retrieve value settings

      retrieveValueMethod: ['rpc', []],
      valueKey: ['value', [Validators.required]],
      getValueMethod: ['getValue', [Validators.required]],
      parseValueFunction: ['return data ? true : false;', []],

      // --> Update value settings

      setValueMethod: ['setValue', [Validators.required]],
      convertValueFunction: ['return value;', []],

      // RPC settings

      requestTimeout: [500, [Validators.min(0)]],

      // Persistent RPC settings

      requestPersistent: [false, []],
      persistentPollingInterval: [5000, [Validators.min(1000)]],
    });
    this.switchRpcSettingsFormGroup.get('retrieveValueMethod').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.switchRpcSettingsFormGroup.get('requestPersistent').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.switchRpcSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.switchRpcSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.switchRpcSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SwitchRpcSettings): void {
    this.modelValue = value;
    this.switchRpcSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.switchRpcSettingsFormGroup.valid ? null : {
      switchRpcSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    this.modelValue = this.switchRpcSettingsFormGroup.value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const retrieveValueMethod: RpcRetrieveValueMethod = this.switchRpcSettingsFormGroup.get('retrieveValueMethod').value;
    const requestPersistent: boolean = this.switchRpcSettingsFormGroup.get('requestPersistent').value;
    if (retrieveValueMethod === 'none') {
      this.switchRpcSettingsFormGroup.get('valueKey').disable({emitEvent});
      this.switchRpcSettingsFormGroup.get('getValueMethod').disable({emitEvent});
      this.switchRpcSettingsFormGroup.get('parseValueFunction').disable({emitEvent});
    } else if (retrieveValueMethod === 'rpc') {
      this.switchRpcSettingsFormGroup.get('valueKey').disable({emitEvent});
      this.switchRpcSettingsFormGroup.get('getValueMethod').enable({emitEvent});
      this.switchRpcSettingsFormGroup.get('parseValueFunction').enable({emitEvent});
    } else {
      this.switchRpcSettingsFormGroup.get('valueKey').enable({emitEvent});
      this.switchRpcSettingsFormGroup.get('getValueMethod').disable({emitEvent});
      this.switchRpcSettingsFormGroup.get('parseValueFunction').enable({emitEvent});
    }
    if (requestPersistent) {
      this.switchRpcSettingsFormGroup.get('persistentPollingInterval').enable({emitEvent});
    } else {
      this.switchRpcSettingsFormGroup.get('persistentPollingInterval').disable({emitEvent});
    }
    this.switchRpcSettingsFormGroup.get('valueKey').updateValueAndValidity({emitEvent: false});
    this.switchRpcSettingsFormGroup.get('getValueMethod').updateValueAndValidity({emitEvent: false});
    this.switchRpcSettingsFormGroup.get('parseValueFunction').updateValueAndValidity({emitEvent: false});
    this.switchRpcSettingsFormGroup.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}

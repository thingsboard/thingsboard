///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DefaultTenantProfileConfiguration, TenantProfileConfiguration } from '@shared/models/tenant.model';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-default-tenant-profile-configuration',
  templateUrl: './default-tenant-profile-configuration.component.html',
  styleUrls: ['./default-tenant-profile-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DefaultTenantProfileConfigurationComponent),
    multi: true
  }]
})
export class DefaultTenantProfileConfigurationComponent implements ControlValueAccessor, OnInit {

  defaultTenantProfileConfigurationFormGroup: FormGroup;
  rateLimitsPattern = '([1-9]\\d*:[1-9]\\d*)(,[1-9]\\d*:[1-9]\\d*)*';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.defaultTenantProfileConfigurationFormGroup = this.fb.group({
      maxDevices: [null, [Validators.required, Validators.min(0)]],
      maxAssets: [null, [Validators.required, Validators.min(0)]],
      maxCustomers: [null, [Validators.required, Validators.min(0)]],
      maxUsers: [null, [Validators.required, Validators.min(0)]],
      maxDashboards: [null, [Validators.required, Validators.min(0)]],
      maxRuleChains: [null, [Validators.required, Validators.min(0)]],
      maxResourcesInBytes: [null, [Validators.required, Validators.min(0)]],
      maxOtaPackagesInBytes: [null, [Validators.required, Validators.min(0)]],
      transportTenantMsgRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      transportTenantTelemetryMsgRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      transportTenantTelemetryDataPointsRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      transportDeviceMsgRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      transportDeviceTelemetryMsgRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      transportDeviceTelemetryDataPointsRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      tenantEntityExportRateLimit: [null, []],
      tenantEntityImportRateLimit: [null, []],
      maxTransportMessages: [null, [Validators.required, Validators.min(0)]],
      maxTransportDataPoints: [null, [Validators.required, Validators.min(0)]],
      maxREExecutions: [null, [Validators.required, Validators.min(0)]],
      maxJSExecutions: [null, [Validators.required, Validators.min(0)]],
      maxDPStorageDays: [null, [Validators.required, Validators.min(0)]],
      maxRuleNodeExecutionsPerMessage: [null, [Validators.required, Validators.min(0)]],
      maxEmails: [null, [Validators.required, Validators.min(0)]],
      maxSms: [null, [Validators.required, Validators.min(0)]],
      maxCreatedAlarms: [null, [Validators.required, Validators.min(0)]],
      defaultStorageTtlDays: [null, [Validators.required, Validators.min(0)]],
      alarmsTtlDays: [null, [Validators.required, Validators.min(0)]],
      rpcTtlDays: [null, [Validators.required, Validators.min(0)]],
      auditLogTtlDays: [null, [Validators.required, Validators.min(0)]],
      tenantServerRestLimitsConfiguration: [null, [Validators.pattern(this.rateLimitsPattern)]],
      customerServerRestLimitsConfiguration: [null, [Validators.pattern(this.rateLimitsPattern)]],
      maxWsSessionsPerTenant: [null, [Validators.min(0)]],
      maxWsSessionsPerCustomer: [null, [Validators.min(0)]],
      maxWsSessionsPerRegularUser: [null, [Validators.min(0)]],
      maxWsSessionsPerPublicUser: [null, [Validators.min(0)]],
      wsMsgQueueLimitPerSession: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerTenant: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerCustomer: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerRegularUser: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerPublicUser: [null, [Validators.min(0)]],
      wsUpdatesPerSessionRateLimit: [null, [Validators.pattern(this.rateLimitsPattern)]],
      cassandraQueryTenantRateLimitsConfiguration: [null, [Validators.pattern(this.rateLimitsPattern)]]
    });
    this.defaultTenantProfileConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.defaultTenantProfileConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.defaultTenantProfileConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DefaultTenantProfileConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.defaultTenantProfileConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: TenantProfileConfiguration = null;
    if (this.defaultTenantProfileConfigurationFormGroup.valid) {
      configuration = this.defaultTenantProfileConfigurationFormGroup.getRawValue();
    }
    this.propagateChange(configuration);
  }
}

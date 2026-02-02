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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DefaultTenantProfileConfiguration, TenantProfileConfiguration } from '@shared/models/tenant.model';
import { isDefinedAndNotNull, isUndefinedOrNull} from '@core/utils';
import { RateLimitsType } from './rate-limits/rate-limits.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

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
export class DefaultTenantProfileConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  defaultTenantProfileConfigurationFormGroup: UntypedFormGroup;

  private requiredValue: boolean;
  private destroy$ = new Subject<void>();
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  rateLimitsType = RateLimitsType;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.defaultTenantProfileConfigurationFormGroup = this.fb.group({
      maxDevices: [null, [Validators.required, Validators.min(0)]],
      maxAssets: [null, [Validators.required, Validators.min(0)]],
      maxCustomers: [null, [Validators.required, Validators.min(0)]],
      maxUsers: [null, [Validators.required, Validators.min(0)]],
      maxDashboards: [null, [Validators.required, Validators.min(0)]],
      maxRuleChains: [null, [Validators.required, Validators.min(0)]],
      maxEdges: [null, [Validators.required, Validators.min(0)]],
      maxResourcesInBytes: [null, [Validators.required, Validators.min(0)]],
      maxOtaPackagesInBytes: [null, [Validators.required, Validators.min(0)]],
      maxResourceSize: [null, [Validators.required, Validators.min(0)]],
      transportTenantMsgRateLimit: [null, []],
      transportTenantTelemetryMsgRateLimit: [null, []],
      transportTenantTelemetryDataPointsRateLimit: [null, []],
      transportDeviceMsgRateLimit: [null, []],
      transportDeviceTelemetryMsgRateLimit: [null, []],
      transportDeviceTelemetryDataPointsRateLimit: [null, []],
      transportGatewayMsgRateLimit: [null, []],
      transportGatewayTelemetryMsgRateLimit: [null, []],
      transportGatewayTelemetryDataPointsRateLimit: [null, []],
      transportGatewayDeviceMsgRateLimit: [null, []],
      transportGatewayDeviceTelemetryMsgRateLimit: [null, []],
      transportGatewayDeviceTelemetryDataPointsRateLimit: [null, []],
      tenantEntityExportRateLimit: [null, []],
      tenantEntityImportRateLimit: [null, []],
      tenantNotificationRequestsRateLimit: [null, []],
      tenantNotificationRequestsPerRuleRateLimit: [null, []],
      maxTransportMessages: [null, [Validators.required, Validators.min(0)]],
      maxTransportDataPoints: [null, [Validators.required, Validators.min(0)]],
      maxREExecutions: [null, [Validators.required, Validators.min(0)]],
      maxJSExecutions: [null, [Validators.required, Validators.min(0)]],
      maxTbelExecutions: [null, [Validators.required, Validators.min(0)]],
      maxDPStorageDays: [null, [Validators.required, Validators.min(0)]],
      maxRuleNodeExecutionsPerMessage: [null, [Validators.required, Validators.min(0)]],
      maxEmails: [null, [Validators.required, Validators.min(0)]],
      maxSms: [null, []],
      smsEnabled: [null, []],
      maxCreatedAlarms: [null, [Validators.required, Validators.min(0)]],
      maxDebugModeDurationMinutes: [null, [Validators.min(0)]],
      defaultStorageTtlDays: [null, [Validators.required, Validators.min(0)]],
      alarmsTtlDays: [null, [Validators.required, Validators.min(0)]],
      rpcTtlDays: [null, [Validators.required, Validators.min(0)]],
      queueStatsTtlDays: [null, [Validators.required, Validators.min(0)]],
      ruleEngineExceptionsTtlDays: [null, [Validators.required, Validators.min(0)]],
      tenantServerRestLimitsConfiguration: [null, []],
      customerServerRestLimitsConfiguration: [null, []],
      maxWsSessionsPerTenant: [null, [Validators.min(0)]],
      maxWsSessionsPerCustomer: [null, [Validators.min(0)]],
      maxWsSessionsPerRegularUser: [null, [Validators.min(0)]],
      maxWsSessionsPerPublicUser: [null, [Validators.min(0)]],
      wsMsgQueueLimitPerSession: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerTenant: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerCustomer: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerRegularUser: [null, [Validators.min(0)]],
      maxWsSubscriptionsPerPublicUser: [null, [Validators.min(0)]],
      wsUpdatesPerSessionRateLimit: [null, []],
      cassandraWriteQueryTenantCoreRateLimits: [null, []],
      cassandraReadQueryTenantCoreRateLimits: [null, []],
      cassandraWriteQueryTenantRuleEngineRateLimits: [null, []],
      cassandraReadQueryTenantRuleEngineRateLimits: [null, []],
      edgeEventRateLimits: [null, []],
      edgeEventRateLimitsPerEdge: [null, []],
      edgeUplinkMessagesRateLimits: [null, []],
      edgeUplinkMessagesRateLimitsPerEdge: [null, []],
      maxCalculatedFieldsPerEntity: [null, [Validators.required, Validators.min(0)]],
      maxArgumentsPerCF: [null, [Validators.required, Validators.min(0)]],
      maxDataPointsPerRollingArg: [null, [Validators.required, Validators.min(0)]],
      maxStateSizeInKBytes: [null, [Validators.required, Validators.min(0)]],
      calculatedFieldDebugEventsRateLimit: [null, []],
      maxSingleValueArgumentSizeInKBytes: [null, [Validators.required, Validators.min(0)]],
    });

    this.defaultTenantProfileConfigurationFormGroup.get('smsEnabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: boolean) => {
        this.maxSmsValidation(value);
      }
    );

    this.defaultTenantProfileConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private maxSmsValidation(smsEnabled: boolean) {
    if (smsEnabled) {
      this.defaultTenantProfileConfigurationFormGroup.get('maxSms').addValidators([Validators.required, Validators.min(0)]);
    } else {
      this.defaultTenantProfileConfigurationFormGroup.get('maxSms').clearValidators();
    }
    this.defaultTenantProfileConfigurationFormGroup.get('maxSms').updateValueAndValidity({emitEvent: false});
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
      if (isUndefinedOrNull(value.smsEnabled)) {
        value.smsEnabled = true;
      }
      this.maxSmsValidation(value.smsEnabled);
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

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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { DefaultTenantProfileConfiguration, FormControlsFrom } from '@shared/models/tenant.model';
import { isDefinedAndNotNull, isUndefinedOrNull} from '@core/utils';
import { RateLimitsType } from './rate-limits/rate-limits.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-default-tenant-profile-configuration',
    templateUrl: './default-tenant-profile-configuration.component.html',
    styleUrls: ['./default-tenant-profile-configuration.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DefaultTenantProfileConfigurationComponent),
            multi: true
        }],
    standalone: false
})
export class DefaultTenantProfileConfigurationComponent implements ControlValueAccessor {

  tenantProfileConfigurationForm: FormGroup<FormControlsFrom<DefaultTenantProfileConfiguration>>;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  rateLimitsType = RateLimitsType;

  private propagateChange = (_v: any) => { };

  constructor(private fb: FormBuilder) {
    this.tenantProfileConfigurationForm = this.fb.group({
      maxDevices: [0, [Validators.required, Validators.min(0)]],
      maxAssets: [0, [Validators.required, Validators.min(0)]],
      maxCustomers: [0, [Validators.required, Validators.min(0)]],
      maxUsers: [0, [Validators.required, Validators.min(0)]],
      maxDashboards: [0, [Validators.required, Validators.min(0)]],
      maxRuleChains: [0, [Validators.required, Validators.min(0)]],
      maxEdges: [0, [Validators.required, Validators.min(0)]],
      maxResourcesInBytes: [0, [Validators.required, Validators.min(0)]],
      maxOtaPackagesInBytes: [0, [Validators.required, Validators.min(0)]],
      maxResourceSize: [0, [Validators.required, Validators.min(0)]],
      transportTenantMsgRateLimit: [''],
      transportTenantTelemetryMsgRateLimit: [''],
      transportTenantTelemetryDataPointsRateLimit: [''],
      transportDeviceMsgRateLimit: [''],
      transportDeviceTelemetryMsgRateLimit: [''],
      transportDeviceTelemetryDataPointsRateLimit: [''],
      transportGatewayMsgRateLimit: [''],
      transportGatewayTelemetryMsgRateLimit: [''],
      transportGatewayTelemetryDataPointsRateLimit: [''],
      transportGatewayDeviceMsgRateLimit: [''],
      transportGatewayDeviceTelemetryMsgRateLimit: [''],
      transportGatewayDeviceTelemetryDataPointsRateLimit: [''],
      tenantEntityExportRateLimit: [''],
      tenantEntityImportRateLimit: [''],
      tenantNotificationRequestsRateLimit: [''],
      tenantNotificationRequestsPerRuleRateLimit: [''],
      maxTransportMessages: [0, [Validators.required, Validators.min(0)]],
      maxTransportDataPoints: [0, [Validators.required, Validators.min(0)]],
      maxREExecutions: [0, [Validators.required, Validators.min(0)]],
      maxJSExecutions: [0, [Validators.required, Validators.min(0)]],
      maxTbelExecutions: [0, [Validators.required, Validators.min(0)]],
      maxDPStorageDays: [0, [Validators.required, Validators.min(0)]],
      maxRuleNodeExecutionsPerMessage: [0, [Validators.required, Validators.min(0)]],
      maxEmails: [0, [Validators.required, Validators.min(0)]],
      maxSms: [0],
      smsEnabled: [false],
      maxCreatedAlarms: [0, [Validators.required, Validators.min(0)]],
      maxDebugModeDurationMinutes: [0, [Validators.min(0)]],
      defaultStorageTtlDays: [0, [Validators.required, Validators.min(0)]],
      alarmsTtlDays: [0, [Validators.required, Validators.min(0)]],
      rpcTtlDays: [0, [Validators.required, Validators.min(0)]],
      queueStatsTtlDays: [0, [Validators.required, Validators.min(0)]],
      ruleEngineExceptionsTtlDays: [0, [Validators.required, Validators.min(0)]],
      tenantServerRestLimitsConfiguration: [''],
      customerServerRestLimitsConfiguration: [''],
      maxWsSessionsPerTenant: [0, [Validators.min(0)]],
      maxWsSessionsPerCustomer: [0, [Validators.min(0)]],
      maxWsSessionsPerRegularUser: [0, [Validators.min(0)]],
      maxWsSessionsPerPublicUser: [0, [Validators.min(0)]],
      wsMsgQueueLimitPerSession: [0, [Validators.min(0)]],
      maxWsSubscriptionsPerTenant: [0, [Validators.min(0)]],
      maxWsSubscriptionsPerCustomer: [0, [Validators.min(0)]],
      maxWsSubscriptionsPerRegularUser: [0, [Validators.min(0)]],
      maxWsSubscriptionsPerPublicUser: [0, [Validators.min(0)]],
      wsUpdatesPerSessionRateLimit: [''],
      cassandraWriteQueryTenantCoreRateLimits: [''],
      cassandraReadQueryTenantCoreRateLimits: [''],
      cassandraWriteQueryTenantRuleEngineRateLimits: [''],
      cassandraReadQueryTenantRuleEngineRateLimits: [''],
      edgeEventRateLimits: [''],
      edgeEventRateLimitsPerEdge: [''],
      edgeUplinkMessagesRateLimits: [''],
      edgeUplinkMessagesRateLimitsPerEdge: [''],
      maxCalculatedFieldsPerEntity: [0, [Validators.required, Validators.min(0)]],
      maxArgumentsPerCF: [0, [Validators.required, Validators.min(0)]],
      maxRelationLevelPerCfArgument: [1, [Validators.required, Validators.min(1)]],
      minAllowedDeduplicationIntervalInSecForCF: [0, [Validators.required, Validators.min(0)]],
      minAllowedAggregationIntervalInSecForCF: [0, [Validators.required, Validators.min(0)]],
      maxRelatedEntitiesToReturnPerCfArgument: [1, [Validators.required, Validators.min(1)]],
      minAllowedScheduledUpdateIntervalInSecForCF: [0, [Validators.required, Validators.min(0)]],
      intermediateAggregationIntervalInSecForCF: [0, [Validators.required, Validators.min(1)]],
      cfReevaluationCheckInterval: [0, [Validators.required, Validators.min(1)]],
      alarmsReevaluationInterval: [0, [Validators.required, Validators.min(1)]],
      maxDataPointsPerRollingArg: [0, [Validators.required, Validators.min(0)]],
      maxStateSizeInKBytes: [0, [Validators.required, Validators.min(0)]],
      calculatedFieldDebugEventsRateLimit: [''],
      maxSingleValueArgumentSizeInKBytes: [0, [Validators.required, Validators.min(0)]],
    });

    this.tenantProfileConfigurationForm.get('smsEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: boolean) => {
        this.maxSmsValidation(value);
      }
    );

    this.tenantProfileConfigurationForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private maxSmsValidation(smsEnabled: boolean) {
    if (smsEnabled) {
      this.tenantProfileConfigurationForm.get('maxSms').addValidators([Validators.required, Validators.min(0)]);
    } else {
      this.tenantProfileConfigurationForm.get('maxSms').clearValidators();
    }
    this.tenantProfileConfigurationForm.get('maxSms').updateValueAndValidity({emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.tenantProfileConfigurationForm.disable({emitEvent: false});
    } else {
      this.tenantProfileConfigurationForm.enable({emitEvent: false});
    }
  }

  writeValue(value: DefaultTenantProfileConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      if (isUndefinedOrNull(value.smsEnabled)) {
        value.smsEnabled = true;
      }
      this.maxSmsValidation(value.smsEnabled);
      this.tenantProfileConfigurationForm.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DefaultTenantProfileConfiguration = null;
    if (this.tenantProfileConfigurationForm.valid) {
      configuration = this.tenantProfileConfigurationForm.getRawValue();
    }
    this.propagateChange(configuration);
  }
}

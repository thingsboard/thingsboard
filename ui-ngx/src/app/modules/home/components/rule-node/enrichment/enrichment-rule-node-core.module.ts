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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { CustomerAttributesConfigComponent } from './customer-attributes-config.component';
import { CommonRuleNodeConfigModule } from '../common/common-rule-node-config.module';
import { EntityDetailsConfigComponent } from './entity-details-config.component';
import { DeviceAttributesConfigComponent } from './device-attributes-config.component';
import { OriginatorAttributesConfigComponent } from './originator-attributes-config.component';
import { OriginatorFieldsConfigComponent } from './originator-fields-config.component';
import { GetTelemetryFromDatabaseConfigComponent } from './get-telemetry-from-database-config.component';
import { RelatedAttributesConfigComponent } from './related-attributes-config.component';
import { TenantAttributesConfigComponent } from './tenant-attributes-config.component';
import { CalculateDeltaConfigComponent } from './calculate-delta-config.component';
import { FetchDeviceCredentialsConfigComponent } from './fetch-device-credentials-config.component';

@NgModule({
  declarations: [
    CustomerAttributesConfigComponent,
    EntityDetailsConfigComponent,
    DeviceAttributesConfigComponent,
    OriginatorAttributesConfigComponent,
    OriginatorFieldsConfigComponent,
    GetTelemetryFromDatabaseConfigComponent,
    RelatedAttributesConfigComponent,
    TenantAttributesConfigComponent,
    CalculateDeltaConfigComponent,
    FetchDeviceCredentialsConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    CustomerAttributesConfigComponent,
    EntityDetailsConfigComponent,
    DeviceAttributesConfigComponent,
    OriginatorAttributesConfigComponent,
    OriginatorFieldsConfigComponent,
    GetTelemetryFromDatabaseConfigComponent,
    RelatedAttributesConfigComponent,
    TenantAttributesConfigComponent,
    CalculateDeltaConfigComponent,
    FetchDeviceCredentialsConfigComponent
  ]
})
export class EnrichmentRuleNodeCoreModule {
}

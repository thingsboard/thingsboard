// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { HomeComponentsModule } from '@home/components/public-api';
import { AttributesConfigComponent } from './attributes-config.component';
import { TimeseriesConfigComponent } from './timeseries-config.component';
import { RpcRequestConfigComponent } from './rpc-request-config.component';
import { LogConfigComponent } from './log-config.component';
import { AssignCustomerConfigComponent } from './assign-customer-config.component';
import { ClearAlarmConfigComponent } from './clear-alarm-config.component';
import { CreateAlarmConfigComponent } from './create-alarm-config.component';
import { CreateRelationConfigComponent } from './create-relation-config.component';
import { MsgDelayConfigComponent } from './msg-delay-config.component';
import { DeleteRelationConfigComponent } from './delete-relation-config.component';
import { GeneratorConfigComponent } from './generator-config.component';
import { GpsGeoActionConfigComponent } from './gps-geo-action-config.component';
import { MsgCountConfigComponent } from './msg-count-config.component';
import { RpcReplyConfigComponent } from './rpc-reply-config.component';
import { SaveToCustomTableConfigComponent } from './save-to-custom-table-config.component';
import { CommonRuleNodeConfigModule } from '../common/common-rule-node-config.module';
import { UnassignCustomerConfigComponent } from './unassign-customer-config.component';
import { DeviceProfileConfigComponent } from './device-profile-config.component';
import { PushToEdgeConfigComponent } from './push-to-edge-config.component';
import { PushToCloudConfigComponent } from './push-to-cloud-config.component';
import { DeleteAttributesConfigComponent } from './delete-attributes-config.component';
import { MathFunctionConfigComponent } from './math-function-config.component';
import { DeviceStateConfigComponent } from './device-state-config.component';
import { SendRestApiCallReplyConfigComponent } from './send-rest-api-call-reply-config.component';
import {
  AdvancedProcessingSettingComponent
} from '@home/components/rule-node/action/advanced-processing-setting.component';
import {
  AdvancedProcessingSettingRowComponent
} from '@home/components/rule-node/action/advanced-processing-setting-row.component';

@NgModule({
  declarations: [
    DeleteAttributesConfigComponent,
    AttributesConfigComponent,
    TimeseriesConfigComponent,
    RpcRequestConfigComponent,
    LogConfigComponent,
    AssignCustomerConfigComponent,
    ClearAlarmConfigComponent,
    CreateAlarmConfigComponent,
    CreateRelationConfigComponent,
    MsgDelayConfigComponent,
    DeleteRelationConfigComponent,
    GeneratorConfigComponent,
    GpsGeoActionConfigComponent,
    MsgCountConfigComponent,
    RpcReplyConfigComponent,
    SaveToCustomTableConfigComponent,
    UnassignCustomerConfigComponent,
    SendRestApiCallReplyConfigComponent,
    DeviceProfileConfigComponent,
    PushToEdgeConfigComponent,
    PushToCloudConfigComponent,
    MathFunctionConfigComponent,
    DeviceStateConfigComponent,
    AdvancedProcessingSettingComponent,
    AdvancedProcessingSettingRowComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    DeleteAttributesConfigComponent,
    AttributesConfigComponent,
    TimeseriesConfigComponent,
    RpcRequestConfigComponent,
    LogConfigComponent,
    AssignCustomerConfigComponent,
    ClearAlarmConfigComponent,
    CreateAlarmConfigComponent,
    CreateRelationConfigComponent,
    MsgDelayConfigComponent,
    DeleteRelationConfigComponent,
    GeneratorConfigComponent,
    GpsGeoActionConfigComponent,
    MsgCountConfigComponent,
    RpcReplyConfigComponent,
    SaveToCustomTableConfigComponent,
    UnassignCustomerConfigComponent,
    SendRestApiCallReplyConfigComponent,
    DeviceProfileConfigComponent,
    PushToEdgeConfigComponent,
    PushToCloudConfigComponent,
    MathFunctionConfigComponent,
    DeviceStateConfigComponent
  ]
})
export class ActionRuleNodeConfigModule {
}

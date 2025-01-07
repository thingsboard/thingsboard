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

import { NgModule, Type } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IRuleNodeConfigurationComponent, SharedModule } from '@shared/public-api';
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
import { RuleNodeConfigCommonModule } from '../common/rule-node-config-common.module';
import { UnassignCustomerConfigComponent } from './unassign-customer-config.component';
import { DeviceProfileConfigComponent } from './device-profile-config.component';
import { PushToEdgeConfigComponent } from './push-to-edge-config.component';
import { PushToCloudConfigComponent } from './push-to-cloud-config.component';
import { DeleteAttributesConfigComponent } from './delete-attributes-config.component';
import { MathFunctionConfigComponent } from './math-function-config.component';
import { DeviceStateConfigComponent } from './device-state-config.component';
import { SendRestApiCallReplyConfigComponent } from './send-rest-api-call-reply-config.component';
import { EmptyConfigComponent } from '@home/components/rule-node/empty-config.component';

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
    DeviceStateConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    RuleNodeConfigCommonModule
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
export class RuleNodeConfigActionModule {
}

export const ruleNodeActionConfigComponentsMap: Record<string, Type<IRuleNodeConfigurationComponent>> = {
  'tbActionNodeAssignToCustomerConfig': AssignCustomerConfigComponent,
  'tbActionNodeAttributesConfig': AttributesConfigComponent,
  'tbActionNodeClearAlarmConfig': ClearAlarmConfigComponent,
  'tbActionNodeCreateAlarmConfig': CreateAlarmConfigComponent,
  'tbActionNodeCreateRelationConfig': CreateRelationConfigComponent,
  'tbActionNodeDeleteAttributesConfig': DeleteAttributesConfigComponent,
  'tbActionNodeDeleteRelationConfig': DeleteRelationConfigComponent,
  'tbDeviceProfileConfig': DeviceProfileConfigComponent,
  'tbActionNodeDeviceStateConfig': DeviceStateConfigComponent,
  'tbActionNodeGeneratorConfig': GeneratorConfigComponent,
  'tbActionNodeGpsGeofencingConfig': GpsGeoActionConfigComponent,
  'tbActionNodeLogConfig': LogConfigComponent,
  'tbActionNodeMathFunctionConfig': MathFunctionConfigComponent,
  'tbActionNodeMsgCountConfig': MsgCountConfigComponent,
  'tbActionNodeMsgDelayConfig': MsgDelayConfigComponent,
  'tbActionNodePushToCloudConfig': PushToCloudConfigComponent,
  'tbActionNodePushToEdgeConfig': PushToEdgeConfigComponent,
  'tbActionNodeRpcReplyConfig': RpcReplyConfigComponent,
  'tbActionNodeRpcRequestConfig': RpcRequestConfigComponent,
  'tbActionNodeCustomTableConfig': SaveToCustomTableConfigComponent,
  'tbActionNodeSendRestApiCallReplyConfig': SendRestApiCallReplyConfigComponent,
  'tbActionNodeTimeseriesConfig': TimeseriesConfigComponent,
  'tbActionNodeUnAssignToCustomerConfig': UnassignCustomerConfigComponent
};

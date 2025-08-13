///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { SnsConfigComponent } from './sns-config.component';
import { SqsConfigComponent } from './sqs-config.component';
import { PubSubConfigComponent } from './pubsub-config.component';
import { KafkaConfigComponent } from './kafka-config.component';
import { MqttConfigComponent } from './mqtt-config.component';
import { NotificationConfigComponent } from './notification-config.component';
import { RabbitMqConfigComponent } from './rabbit-mq-config.component';
import { RestApiCallConfigComponent } from './rest-api-call-config.component';
import { SendEmailConfigComponent } from './send-email-config.component';
import { AzureIotHubConfigComponent } from './azure-iot-hub-config.component';
import { SendSmsConfigComponent } from './send-sms-config.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { HomeComponentsModule } from '@home/components/public-api';
import { CommonRuleNodeConfigModule } from '../common/common-rule-node-config.module';
import { SlackConfigComponent } from './slack-config.component';
import { LambdaConfigComponent } from './lambda-config.component';
import { AiConfigComponent } from '@home/components/rule-node/external/ai-config.component';

@NgModule({
  declarations: [
    SnsConfigComponent,
    SqsConfigComponent,
    LambdaConfigComponent,
    PubSubConfigComponent,
    KafkaConfigComponent,
    MqttConfigComponent,
    NotificationConfigComponent,
    RabbitMqConfigComponent,
    RestApiCallConfigComponent,
    SendEmailConfigComponent,
    AzureIotHubConfigComponent,
    SendSmsConfigComponent,
    SlackConfigComponent,
    AiConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    SnsConfigComponent,
    SqsConfigComponent,
    LambdaConfigComponent,
    PubSubConfigComponent,
    KafkaConfigComponent,
    MqttConfigComponent,
    NotificationConfigComponent,
    RabbitMqConfigComponent,
    RestApiCallConfigComponent,
    SendEmailConfigComponent,
    AzureIotHubConfigComponent,
    SendSmsConfigComponent,
    SlackConfigComponent,
    AiConfigComponent
  ]
})
export class ExternalRuleNodeConfigModule {
}

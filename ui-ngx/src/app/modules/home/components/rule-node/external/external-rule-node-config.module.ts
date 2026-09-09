// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-external-node-rabbit-mq-config',
  templateUrl: './rabbit-mq-config.component.html',
  styleUrls: []
})
export class RabbitMqConfigComponent extends RuleNodeConfigurationComponent {

  rabbitMqConfigForm: UntypedFormGroup;

  messageProperties: string[] = [
    null,
    'BASIC',
    'TEXT_PLAIN',
    'MINIMAL_BASIC',
    'MINIMAL_PERSISTENT_BASIC',
    'PERSISTENT_BASIC',
    'PERSISTENT_TEXT_PLAIN'
  ];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.rabbitMqConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.rabbitMqConfigForm = this.fb.group({
      exchangeNamePattern: [configuration ? configuration.exchangeNamePattern : null, []],
      routingKeyPattern: [configuration ? configuration.routingKeyPattern : null, []],
      messageProperties: [configuration ? configuration.messageProperties : null, []],
      host: [configuration ? configuration.host : null, [Validators.required]],
      port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
      virtualHost: [configuration ? configuration.virtualHost : null, []],
      username: [configuration ? configuration.username : null, []],
      password: [configuration ? configuration.password : null, []],
      automaticRecoveryEnabled: [configuration ? configuration.automaticRecoveryEnabled : false, []],
      connectionTimeout: [configuration ? configuration.connectionTimeout : null, [Validators.min(0)]],
      handshakeTimeout: [configuration ? configuration.handshakeTimeout : null, [Validators.min(0)]],
      clientProperties: [configuration ? configuration.clientProperties : null, []]
    });
  }
}

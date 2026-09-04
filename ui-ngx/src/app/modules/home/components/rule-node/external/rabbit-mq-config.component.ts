// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-rabbit-mq-config',
    templateUrl: './rabbit-mq-config.component.html',
    styleUrls: [],
    standalone: false
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

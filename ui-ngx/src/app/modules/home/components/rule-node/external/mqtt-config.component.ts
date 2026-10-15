// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isNotEmptyStr } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-mqtt-config',
    templateUrl: './mqtt-config.component.html',
    styleUrls: ['./mqtt-config.component.scss'],
    standalone: false
})
export class MqttConfigComponent extends RuleNodeConfigurationComponent {

  mqttConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.mqttConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.mqttConfigForm = this.fb.group({
      topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
      host: [configuration ? configuration.host : null, [Validators.required]],
      port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
      connectTimeoutSec: [configuration ? configuration.connectTimeoutSec : null,
        [Validators.required, Validators.min(1), Validators.max(200)]],
      clientId: [configuration ? configuration.clientId : null, []],
      appendClientIdSuffix: [{
        value: configuration ? configuration.appendClientIdSuffix : false,
        disabled: !(configuration && isNotEmptyStr(configuration.clientId))
      }, []],
      parseToPlainText: [configuration ? configuration.parseToPlainText : false, []],
      cleanSession: [configuration ? configuration.cleanSession : false, []],
      retainedMessage: [configuration ? configuration.retainedMessage : false, []],
      ssl: [configuration ? configuration.ssl : false, []],
      protocolVersion: [configuration ? configuration.protocolVersion : null, []],
      credentials: [configuration ? configuration.credentials : null, []]
    });
  }

  protected updateValidators(emitEvent: boolean) {
    if (isNotEmptyStr(this.mqttConfigForm.get('clientId').value)) {
      this.mqttConfigForm.get('appendClientIdSuffix').enable({emitEvent: false});
    } else {
      this.mqttConfigForm.get('appendClientIdSuffix').disable({emitEvent: false});
    }
    this.mqttConfigForm.get('appendClientIdSuffix').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['clientId'];
  }
}

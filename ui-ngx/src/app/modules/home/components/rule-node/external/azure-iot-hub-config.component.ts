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
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  AzureIotHubCredentialsType,
  azureIotHubCredentialsTypes,
  azureIotHubCredentialsTypeTranslations
} from '@home/components/rule-node/rule-node-config.models';
import { MqttVersion } from '@shared/models/mqtt.models';

@Component({
    selector: 'tb-external-node-azure-iot-hub-config',
    templateUrl: './azure-iot-hub-config.component.html',
    styleUrls: ['./mqtt-config.component.scss'],
    standalone: false
})
export class AzureIotHubConfigComponent extends RuleNodeConfigurationComponent {

  azureIotHubConfigForm: UntypedFormGroup;

  allAzureIotHubCredentialsTypes = azureIotHubCredentialsTypes;
  azureIotHubCredentialsTypeTranslationsMap = azureIotHubCredentialsTypeTranslations;
  MqttVersion = MqttVersion;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.azureIotHubConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.azureIotHubConfigForm = this.fb.group({
      topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
      host: [configuration ? configuration.host : null, [Validators.required]],
      port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
      connectTimeoutSec: [configuration ? configuration.connectTimeoutSec : null,
        [Validators.required, Validators.min(1), Validators.max(200)]],
      clientId: [configuration ? configuration.clientId : null, [Validators.required]],
      cleanSession: [configuration ? configuration.cleanSession : false, []],
      ssl: [configuration ? configuration.ssl : false, []],
      protocolVersion: [configuration ? configuration.protocolVersion : null, []],
      credentials: this.fb.group(
        {
          type: [configuration && configuration.credentials ? configuration.credentials.type : null, [Validators.required]],
          sasKey: [configuration && configuration.credentials ? configuration.credentials.sasKey : null, []],
          caCert: [configuration && configuration.credentials ? configuration.credentials.caCert : null, []],
          caCertFileName: [configuration && configuration.credentials ? configuration.credentials.caCertFileName : null, []],
          privateKey: [configuration && configuration.credentials ? configuration.credentials.privateKey : null, []],
          privateKeyFileName: [configuration && configuration.credentials ? configuration.credentials.privateKeyFileName : null, []],
          cert: [configuration && configuration.credentials ? configuration.credentials.cert : null, []],
          certFileName: [configuration && configuration.credentials ? configuration.credentials.certFileName : null, []],
          password: [configuration && configuration.credentials ? configuration.credentials.password : null, []],
        }
      )
    });
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    const credentialsType: AzureIotHubCredentialsType = configuration.credentials.type;
      if (credentialsType === 'sas') {
        configuration.credentials = {
          type: credentialsType,
          sasKey: configuration.credentials.sasKey,
          caCert: configuration.credentials.caCert,
          caCertFileName: configuration.credentials.caCertFileName
        };
    }
    return configuration;
  }

  protected validatorTriggers(): string[] {
    return ['credentials.type'];
  }

  protected updateValidators(emitEvent: boolean) {
    const credentialsControl = this.azureIotHubConfigForm.get('credentials');
    const credentialsType: AzureIotHubCredentialsType = credentialsControl.get('type').value;
    if (emitEvent) {
      credentialsControl.reset({ type: credentialsType }, {emitEvent: false});
    }
    credentialsControl.get('sasKey').setValidators([]);
    credentialsControl.get('privateKey').setValidators([]);
    credentialsControl.get('privateKeyFileName').setValidators([]);
    credentialsControl.get('cert').setValidators([]);
    credentialsControl.get('certFileName').setValidators([]);
    switch (credentialsType) {
      case 'sas':
        credentialsControl.get('sasKey').setValidators([Validators.required]);
        break;
      case 'cert.PEM':
        credentialsControl.get('privateKey').setValidators([Validators.required]);
        credentialsControl.get('privateKeyFileName').setValidators([Validators.required]);
        credentialsControl.get('cert').setValidators([Validators.required]);
        credentialsControl.get('certFileName').setValidators([Validators.required]);
        break;
    }
    credentialsControl.get('sasKey').updateValueAndValidity({emitEvent});
    credentialsControl.get('privateKey').updateValueAndValidity({emitEvent});
    credentialsControl.get('privateKeyFileName').updateValueAndValidity({emitEvent});
    credentialsControl.get('cert').updateValueAndValidity({emitEvent});
    credentialsControl.get('certFileName').updateValueAndValidity({emitEvent});
  }
}

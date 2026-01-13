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
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  MessageType,
  messageTypeNames,
  RuleNodeConfiguration,
  RuleNodeConfigurationComponent
} from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-device-state-config',
    templateUrl: './device-state-config.component.html',
    styleUrls: []
})
export class DeviceStateConfigComponent extends RuleNodeConfigurationComponent {

    deviceState: FormGroup;

    public messageTypeNames = messageTypeNames;
    public eventOptions: MessageType[] = [
        MessageType.CONNECT_EVENT,
        MessageType.ACTIVITY_EVENT,
        MessageType.DISCONNECT_EVENT,
        MessageType.INACTIVITY_EVENT
    ];

    constructor(private fb: FormBuilder) {
        super();
    }

    protected configForm(): FormGroup {
        return this.deviceState;
    }

    protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
        return {
            event: isDefinedAndNotNull(configuration?.event) ? configuration.event : MessageType.ACTIVITY_EVENT
        };
    }

    protected onConfigurationSet(configuration: RuleNodeConfiguration) {
        this.deviceState = this.fb.group({
            event: [configuration.event, [Validators.required]]
        });
    }

}

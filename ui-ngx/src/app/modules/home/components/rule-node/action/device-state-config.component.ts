// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: [],
    standalone: false
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

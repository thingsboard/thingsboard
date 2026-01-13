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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { NotificationType } from '@shared/models/notification.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-external-node-notification-config',
  templateUrl: './notification-config.component.html',
  styleUrls: []
})
export class NotificationConfigComponent extends RuleNodeConfigurationComponent {

  notificationConfigForm: FormGroup;
  notificationType = NotificationType;
  entityType = EntityType;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.notificationConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.notificationConfigForm = this.fb.group({
      templateId: [configuration ? configuration.templateId : null, [Validators.required]],
      targets: [configuration ? configuration.targets : [], [Validators.required]],
    });
  }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { NotificationType } from '@shared/models/notification.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
    selector: 'tb-external-node-notification-config',
    templateUrl: './notification-config.component.html',
    styleUrls: [],
    standalone: false
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

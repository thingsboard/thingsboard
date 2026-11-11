// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-pub-sub-config',
    templateUrl: './pubsub-config.component.html',
    styleUrls: [],
    standalone: false
})
export class PubSubConfigComponent extends RuleNodeConfigurationComponent {

  pubSubConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.pubSubConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.pubSubConfigForm = this.fb.group({
      projectId: [configuration ? configuration.projectId : null, [Validators.required]],
      topicName: [configuration ? configuration.topicName : null, [Validators.required]],
      serviceAccountKey: [configuration ? configuration.serviceAccountKey : null, [Validators.required]],
      serviceAccountKeyFileName: [configuration ? configuration.serviceAccountKeyFileName : null, []],
      messageAttributes: [configuration ? configuration.messageAttributes : null, []]
    });
  }
}

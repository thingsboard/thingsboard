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
      serviceAccountKeyFileName: [configuration ? configuration.serviceAccountKeyFileName : null, [Validators.required]],
      messageAttributes: [configuration ? configuration.messageAttributes : null, []]
    });
  }
}

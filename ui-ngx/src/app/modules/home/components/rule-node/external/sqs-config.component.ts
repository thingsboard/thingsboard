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
import { SqsQueueType, sqsQueueTypeTranslations } from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-external-node-sqs-config',
    templateUrl: './sqs-config.component.html',
    styleUrls: [],
    standalone: false
})
export class SqsConfigComponent extends RuleNodeConfigurationComponent {

  sqsConfigForm: UntypedFormGroup;

  sqsQueueType = SqsQueueType;
  sqsQueueTypes = Object.keys(SqsQueueType);
  sqsQueueTypeTranslationsMap = sqsQueueTypeTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.sqsConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.sqsConfigForm = this.fb.group({
      queueType: [configuration ? configuration.queueType : null, [Validators.required]],
      queueUrlPattern: [configuration ? configuration.queueUrlPattern : null, [Validators.required]],
      delaySeconds: [configuration ? configuration.delaySeconds : null, [Validators.min(0), Validators.max(900)]],
      messageAttributes: [configuration ? configuration.messageAttributes : null, []],
      accessKeyId: [configuration ? configuration.accessKeyId : null, [Validators.required]],
      secretAccessKey: [configuration ? configuration.secretAccessKey : null, [Validators.required]],
      region: [configuration ? configuration.region : null, [Validators.required]]
    });
  }
}

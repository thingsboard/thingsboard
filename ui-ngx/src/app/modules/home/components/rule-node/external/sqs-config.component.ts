// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

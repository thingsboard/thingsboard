// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-sns-config',
    templateUrl: './sns-config.component.html',
    styleUrls: [],
    standalone: false
})
export class SnsConfigComponent extends RuleNodeConfigurationComponent {

  snsConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.snsConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.snsConfigForm = this.fb.group({
      topicArnPattern: [configuration ? configuration.topicArnPattern : null, [Validators.required]],
      accessKeyId: [configuration ? configuration.accessKeyId : null, [Validators.required]],
      secretAccessKey: [configuration ? configuration.secretAccessKey : null, [Validators.required]],
      region: [configuration ? configuration.region : null, [Validators.required]]
    });
  }
}

///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-action-node-send-rest-api-call-reply-config',
  templateUrl: './send-rest-api-call-reply-config.component.html',
  styleUrls: []
})
export class SendRestApiCallReplyConfigComponent extends RuleNodeConfigurationComponent {

  sendRestApiCallReplyConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.sendRestApiCallReplyConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.sendRestApiCallReplyConfigForm = this.fb.group({
      requestIdMetaDataAttribute: [configuration ? configuration.requestIdMetaDataAttribute : null, []],
      serviceIdMetaDataAttribute: [configuration ? configuration.serviceIdMetaDataAttribute : null, []]
    });
  }
}

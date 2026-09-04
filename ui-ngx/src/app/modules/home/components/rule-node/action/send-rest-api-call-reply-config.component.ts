// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-send-rest-api-call-reply-config',
    templateUrl: './send-rest-api-call-reply-config.component.html',
    styleUrls: [],
    standalone: false
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

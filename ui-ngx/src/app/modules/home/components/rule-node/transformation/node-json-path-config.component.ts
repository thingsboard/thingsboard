// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-transformation-node-json-path-config',
    templateUrl: './node-json-path-config.component.html',
    styleUrls: [],
    standalone: false
})

export class NodeJsonPathConfigComponent extends RuleNodeConfigurationComponent {

  jsonPathConfigForm: FormGroup;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.jsonPathConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.jsonPathConfigForm = this.fb.group({
      jsonPath: [configuration ? configuration.jsonPath : null, [Validators.required]],
    });
  }
}

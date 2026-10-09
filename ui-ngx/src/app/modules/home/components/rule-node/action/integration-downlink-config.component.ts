// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-integration-downlink-config',
    templateUrl: './integration-downlink-config.component.html',
    styleUrls: [],
    standalone: false
})
export class IntegrationDownlinkConfigComponent extends RuleNodeConfigurationComponent {

  integrationDownlinkConfigForm: UntypedFormGroup;

  entityType = EntityType;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.integrationDownlinkConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.integrationDownlinkConfigForm = this.fb.group({
      integrationId: [configuration ? configuration.integrationId : null, [Validators.required]]
    });
  }

}

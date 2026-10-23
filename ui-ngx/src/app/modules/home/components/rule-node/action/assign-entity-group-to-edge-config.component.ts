// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { isDefinedAndNotNull } from '@core/public-api';
import { RuleNodeConfiguration } from '@app/shared/models/rule-node.models';
import { AbstractEdgeEntityGroupConfigComponent } from './abstract-edge-entity-group-config.component';

@Component({
    selector: 'tb-action-node-assign-entity-group-to-edge-config',
    templateUrl: './assign-entity-group-to-edge-config.component.html',
    styleUrls: [],
    standalone: false
})
export class AssignEntityGroupToEdgeConfigComponent extends AbstractEdgeEntityGroupConfigComponent {

  constructor(fb: UntypedFormBuilder) {
    super(fb);
  }

  protected nodeSpecificInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      createGroupIfNotExists: isDefinedAndNotNull(configuration?.createGroupIfNotExists) ? configuration.createGroupIfNotExists : false
    };
  }

  protected nodeSpecificControls(configuration: RuleNodeConfiguration): {[controlName: string]: unknown} {
    return {
      createGroupIfNotExists: [configuration ? configuration.createGroupIfNotExists : false, []]
    };
  }
}

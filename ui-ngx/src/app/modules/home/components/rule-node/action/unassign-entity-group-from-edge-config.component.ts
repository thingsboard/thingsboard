// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { AbstractEdgeEntityGroupConfigComponent } from './abstract-edge-entity-group-config.component';

@Component({
    selector: 'tb-action-node-unassign-entity-group-from-edge-config',
    templateUrl: './unassign-entity-group-from-edge-config.component.html',
    styleUrls: [],
    standalone: false
})
export class UnassignEntityGroupFromEdgeConfigComponent extends AbstractEdgeEntityGroupConfigComponent {

  constructor(fb: UntypedFormBuilder) {
    super(fb);
  }
}

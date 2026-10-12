// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { EdgeSource, edgeSourceTranslations } from '../rule-node-config.models';

@Component({
    selector: 'tb-edge-source',
    templateUrl: './edge-source.component.html',
    standalone: false
})
export class EdgeSourceComponent {

  @Input() form: UntypedFormGroup;

  edgeSource = EdgeSource;
  edgeSources = Object.values(EdgeSource);
  edgeSourceTranslationsMap = edgeSourceTranslations;

}

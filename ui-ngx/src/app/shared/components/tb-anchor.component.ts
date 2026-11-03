// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, ViewContainerRef } from '@angular/core';

@Component({
    selector: 'tb-anchor',
    template: '<ng-template></ng-template>',
    standalone: false
})
export class TbAnchorComponent {
  constructor(public viewContainerRef: ViewContainerRef) { }
}
